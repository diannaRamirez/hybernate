/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.PessimisticLockException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.PostgreSQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitOffsetLimitHandler;
import org.hibernate.dialect.pagination.OffsetFetchLimitHandler;
import org.hibernate.dialect.sequence.PostgreSQLSequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor;
import org.hibernate.exception.spi.ViolatedConstraintNameExtractor;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.procedure.internal.PostgresCallableStatementSupport;
import org.hibernate.procedure.spi.CallableStatementSupport;
import org.hibernate.query.SemanticException;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.cte.CteStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.ValueBinder;
import org.hibernate.type.descriptor.ValueExtractor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BasicBinder;
import org.hibernate.type.descriptor.jdbc.BasicExtractor;
import org.hibernate.type.descriptor.jdbc.BlobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.ClobTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcLiteralFormatter;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeDescriptorRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.*;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.persistence.TemporalType;

import static org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtractor.extractUsingTemplate;
import static org.hibernate.query.TemporalUnit.*;
import static org.hibernate.type.descriptor.DateTimeUtils.wrapAsAnsiDateLiteral;
import static org.hibernate.type.descriptor.DateTimeUtils.wrapAsAnsiTimeLiteral;

/**
 * An SQL dialect for Postgres 8 and above.
 *
 * @author Gavin King
 */
public class PostgreSQLDialect extends Dialect {

	private final int version;

	public PostgreSQLDialect(DialectResolutionInfo info) {
		this( info.getDatabaseMajorVersion() * 100 + info.getDatabaseMinorVersion() * 10 );
	}

	public PostgreSQLDialect() {
		this( 800 );
	}

	public PostgreSQLDialect(int version) {
		super();
		this.version = version;

		registerColumnType( Types.TINYINT, "smallint" ); //no tinyint, not even in Postgres 11

		registerColumnType( Types.VARBINARY, "bytea" );
		registerColumnType( Types.BINARY, "bytea" );

		//use oid as the blob type on Postgres because
		//the JDBC driver is rubbish
		registerColumnType( Types.BLOB, "oid" );
		registerColumnType( Types.CLOB, "text" );

		//there are no nchar/nvarchar types in Postgres
		registerColumnType( Types.NCHAR, "char($l)" );
		registerColumnType( Types.NVARCHAR, "varchar($l)" );

		//since there's no real difference between
		//TEXT and VARCHAR, except for the length limit,
		//we can just use 'text' for the "long" types
		registerColumnType( Types.LONGVARCHAR, "text" );
		registerColumnType( Types.LONGNVARCHAR, "text" );

		if ( getVersion() >= 920 ) {
			registerColumnType( Types.JAVA_OBJECT, "json" );
		}

		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.NON_CONTEXTUAL_LOB_CREATION, "true" );
	}

	@Override
	public int getVersion() {
		return version;
	}

	@Override
	public String currentTime() {
		return "localtime";
	}

	@Override
	public String currentTimestamp() {
		return "localtimestamp";
	}

	@Override
	public String currentTimestampWithTimeZone() {
		return "current_timestamp";
	}

	/**
	 * The {@code extract()} function returns {@link TemporalUnit#DAY_OF_WEEK}
	 * numbered from 0 to 6. This isn't consistent with what most other
	 * databases do, so here we adjust the result by generating
	 * {@code (extract(dow,arg)+1))}.
	 */
	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case DAY_OF_WEEK:
				return "(" + super.extractPattern(unit) + "+1)";
			default:
				return super.extractPattern(unit);
		}
	}

	/**
	 * {@code microsecond} is the smallest unit for an {@code interval},
	 * and the highest precision for a {@code timestamp}, so we could
	 * use it as the "native" precision, but it's more convenient to use
	 * whole seconds (with the fractional part), since we want to use
	 * {@code extract(epoch from ...)} in our emulation of
	 * {@code timestampdiff()}.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		return 1_000_000_000; //seconds
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType) {
		switch ( unit ) {
			case NANOSECOND:
				return "(?3 + (?2)/1e3 * interval '1 microsecond')";
			case NATIVE:
				return "(?3 + (?2) * interval '1 second')";
			case QUARTER: //quarter is not supported in interval literals
				return "(?3 + (?2) * interval '3 month')";
			case WEEK: //week is not supported in interval literals
				return "(?3 + (?2) * interval '7 day')";
			default:
				return "(?3 + (?2) * interval '1 ?1')";
		}
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( toTemporalType != TemporalType.TIMESTAMP && fromTemporalType != TemporalType.TIMESTAMP && unit==DAY ) {
			// special case: subtraction of two dates
			// results in an integer number of days
			// instead of an INTERVAL
			return "(?3-?2)";
		}
		else {
			StringBuilder pattern = new StringBuilder();
			switch (unit) {
				case YEAR:
					extractField( pattern, YEAR, fromTemporalType, toTemporalType, unit);
					break;
				case QUARTER:
					pattern.append("(");
					extractField( pattern, YEAR, fromTemporalType, toTemporalType, unit);
					pattern.append("+");
					extractField( pattern, QUARTER, fromTemporalType, toTemporalType, unit);
					pattern.append(")");
					break;
				case MONTH:
					pattern.append("(");
					extractField( pattern, YEAR, fromTemporalType, toTemporalType, unit);
					pattern.append("+");
					extractField( pattern, MONTH, fromTemporalType, toTemporalType, unit);
					pattern.append(")");
					break;
				case WEEK: //week is not supported by extract() when the argument is a duration
				case DAY:
					extractField( pattern, DAY, fromTemporalType, toTemporalType, unit);
					break;
				//in order to avoid multiple calls to extract(),
				//we use extract(epoch from x - y) * factor for
				//all the following units:
				case HOUR:
				case MINUTE:
				case SECOND:
				case NANOSECOND:
				case NATIVE:
					extractField( pattern, EPOCH, fromTemporalType, toTemporalType, unit);
					break;
				default:
					throw new SemanticException("unrecognized field: " + unit);
			}
			return pattern.toString();
		}
	}

	private void extractField(
			StringBuilder pattern,
			TemporalUnit unit,
			TemporalType fromTimestamp, TemporalType toTimestamp,
			TemporalUnit toUnit) {
		pattern.append("extract(");
		pattern.append( translateDurationField(unit) );
		pattern.append(" from ");
		if ( toTimestamp != TemporalType.TIMESTAMP && fromTimestamp != TemporalType.TIMESTAMP ) {
			// special case subtraction of two
			// dates results in an integer not
			// an Interval
			pattern.append("age(?3,?2)");
		}
		else {
			switch (unit) {
				case YEAR:
				case MONTH:
				case QUARTER:
					pattern.append("age(?3,?2)");
					break;
				case DAY:
				case HOUR:
				case MINUTE:
				case SECOND:
				case EPOCH:
					pattern.append("?3-?2");
					break;
				default:
					throw new SemanticException(unit + " is not a legal field");
			}
		}
		pattern.append(")").append( unit.conversionFactor( toUnit, this ) );
	}

	@Override
	public boolean supportsTimezoneTypes() {
		return true;
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.trunc( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.cbrt( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.md5( queryEngine );
		CommonFunctionFactory.initcap( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		CommonFunctionFactory.substring_substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.translate( queryEngine );
		CommonFunctionFactory.toCharNumberDateTimestamp( queryEngine );
		CommonFunctionFactory.concat_pipeOperator( queryEngine );
		CommonFunctionFactory.localtimeLocaltimestamp( queryEngine );
		CommonFunctionFactory.dateTrunc( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.char_chr( queryEngine );
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.bitandorxornot_operator( queryEngine );
		CommonFunctionFactory.bitAndOr( queryEngine );
		CommonFunctionFactory.everyAny_boolAndOr( queryEngine );
		CommonFunctionFactory.median_percentileCont( queryEngine, false );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.covarPopSamp( queryEngine );
		CommonFunctionFactory.corr( queryEngine );
		CommonFunctionFactory.regrLinearRegressionAggregates( queryEngine );
		CommonFunctionFactory.insert_overlay( queryEngine );
		CommonFunctionFactory.overlay( queryEngine );
		CommonFunctionFactory.soundex( queryEngine ); //was introduced in Postgres 9 apparently

		queryEngine.getSqmFunctionRegistry().registerBinaryTernaryPattern(
				"locate",
				StandardBasicTypes.INTEGER,
				"position(?1 in ?2)",
				"(position(?1 in substring(?2 from ?3)) + (?3) - 1)"
		).setArgumentListSignature("(pattern, string[, start])");

		if ( getVersion() >= 940 ) {
			CommonFunctionFactory.makeDateTimeTimestamp( queryEngine );
		}
	}

	@Override
	public JdbcTypeDescriptor getSqlTypeDescriptorOverride(int sqlCode) {
		// For discussion of BLOB support in Postgres, as of 8.4, have a peek at
		// <a href="http://jdbc.postgresql.org/documentation/84/binary-data.html">http://jdbc.postgresql.org/documentation/84/binary-data.html</a>.
 		// For the effects in regards to Hibernate see <a href="http://in.relation.to/15492.lace">http://in.relation.to/15492.lace</a>
		switch ( sqlCode ) {
			case Types.BLOB:
				// Force BLOB binding.  Otherwise, byte[] fields annotated
				// with @Lob will attempt to use
				// BlobTypeDescriptor.PRIMITIVE_ARRAY_BINDING.  Since the
				// dialect uses oid for Blobs, byte arrays cannot be used.
				return BlobTypeDescriptor.BLOB_BINDING;
			case Types.CLOB:
				return ClobTypeDescriptor.CLOB_BINDING;
			default:
				return super.getSqlTypeDescriptorOverride( sqlCode );
		}
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return getVersion() >= 820;
	}

	@Override
	public boolean supportsIfExistsBeforeConstraintName() {
		return getVersion() >= 900;
	}

	@Override
	public boolean supportsIfExistsAfterAlterTable() {
		return getVersion() >= 920;
	}

	@Override
	public boolean supportsValuesList() {
		return getVersion() >= 820;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInInList() {
		return getVersion() >= 820;
	}

	@Override
	public boolean supportsPartitionBy() {
		return getVersion() >= 910;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return getVersion() >= 910;
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return getVersion() < 820
				? PostgreSQLSequenceSupport.LEGACY_INSTANCE
				: PostgreSQLSequenceSupport.INSTANCE;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade";
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from information_schema.sequences";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return getVersion() < 840
				? LimitOffsetLimitHandler.INSTANCE
				: OffsetFetchLimitHandler.INSTANCE;
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateString(String aliases, LockOptions lockOptions) {
		/*
		 * Parent's implementation for (aliases, lockOptions) ignores aliases.
		 */
		if ( aliases.isEmpty() ) {
			LockMode lockMode = lockOptions.getLockMode();
			final Iterator<Map.Entry<String, LockMode>> itr = lockOptions.getAliasLockIterator();
			while ( itr.hasNext() ) {
				// seek the highest lock mode
				final Map.Entry<String, LockMode> entry = itr.next();
				final LockMode lm = entry.getValue();
				if ( lm.greaterThan( lockMode ) ) {
					aliases = entry.getKey();
				}
			}
		}
		LockMode lockMode = lockOptions.getAliasSpecificLockMode( aliases );
		if (lockMode == null ) {
			lockMode = lockOptions.getLockMode();
		}
		switch ( lockMode ) {
			//noinspection deprecation
			case UPGRADE:
				return getForUpdateString(aliases);
			case PESSIMISTIC_READ:
				return getReadLockString( aliases, lockOptions.getTimeOut() );
			case PESSIMISTIC_WRITE:
				return getWriteLockString( aliases, lockOptions.getTimeOut() );
			case UPGRADE_NOWAIT:
				//noinspection deprecation
			case FORCE:
			case PESSIMISTIC_FORCE_INCREMENT:
				return getForUpdateNowaitString(aliases);
			case UPGRADE_SKIPLOCKED:
				return getForUpdateSkipLockedString(aliases);
			default:
				return "";
		}
	}

	@Override
	public String getNoColumnsInsertString() {
		return "default values";
	}

	@Override
	public String getCaseInsensitiveLike(){
		return "ilike";
	}

	@Override
	public boolean supportsCaseInsensitiveLike() {
		return true;
	}

	@Override
	public String getNativeIdentifierGeneratorStrategy() {
		return "sequence";
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean useInputStreamToInsertBlob() {
		return false;
	}

	/**
	 * Workaround for postgres bug #1453
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getSelectClauseNullString(int sqlType) {
		return "null::" + getRawTypeName( sqlType );
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select now()";
	}

	@Override
	public boolean requiresParensForTupleDistinctCounts() {
		return true;
	}

	@Override
	public String toBooleanValueString(boolean bool) {
		return String.valueOf( bool );
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new PostgreSQLSqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public ViolatedConstraintNameExtractor getViolatedConstraintNameExtractor() {
		return EXTRACTOR;
	}

	/**
	 * Constraint-name extractor for Postgres constraint violation exceptions.
	 * Orginally contributed by Denny Bartelt.
	 */
	private static final ViolatedConstraintNameExtractor EXTRACTOR =
			new TemplatedViolatedConstraintNameExtractor( sqle -> {
				switch ( Integer.parseInt( JdbcExceptionHelper.extractSqlState( sqle ) ) ) {
					// CHECK VIOLATION
					case 23514:
						return extractUsingTemplate( "violates check constraint \"","\"", sqle.getMessage() );
					// UNIQUE VIOLATION
					case 23505:
						return extractUsingTemplate( "violates unique constraint \"","\"", sqle.getMessage() );
					// FOREIGN KEY VIOLATION
					case 23503:
						return extractUsingTemplate( "violates foreign key constraint \"","\"", sqle.getMessage() );
					// NOT NULL VIOLATION
					case 23502:
						return extractUsingTemplate( "null value in column \"","\" violates not-null constraint", sqle.getMessage() );
					// TODO: RESTRICT VIOLATION
					case 23001:
						return null;
					// ALL OTHER
					default:
						return null;
				}
			} );

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			switch ( JdbcExceptionHelper.extractSqlState( sqlException ) ) {
				case "40P01":
					// DEADLOCK DETECTED
					return new LockAcquisitionException(message, sqlException, sql);
				case "55P03":
					// LOCK NOT AVAILABLE
					return new PessimisticLockException(message, sqlException, sql);
				default:
					// returning null allows other delegates to operate
					return null;
			}
		};
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		// Register the type of the out param - PostgreSQL uses Types.OTHER
		statement.registerOutParameter( col++, Types.OTHER );
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean supportsUnboundedLobLocatorMaterialization() {
		return false;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public boolean supportsRowValueConstructorSyntaxInQuantifiedPredicates() {
		return true;
	}

	@Override
	public CallableStatementSupport getCallableStatementSupport() {
		return PostgresCallableStatementSupport.INSTANCE;
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, int position) throws SQLException {
		if ( position != 1 ) {
			throw new UnsupportedOperationException( "PostgreSQL only supports REF_CURSOR parameters as the first parameter" );
		}
		return (ResultSet) statement.getObject( 1 );
	}

	@Override
	public ResultSet getResultSet(CallableStatement statement, String name) throws SQLException {
		throw new UnsupportedOperationException( "PostgreSQL only supports accessing REF_CURSOR parameters by position" );
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new PostgreSQLIdentityColumnSupport();
	}

	@Override
	public boolean supportsNationalizedTypes() {
		return false;
	}

	@Override
	public boolean supportsJdbcConnectionLobCreation(DatabaseMetaData databaseMetaData) {
		return false;
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return datetimeFormat( format ).result();
	}

	public Replacer datetimeFormat(String format) {
		return OracleDialect.datetimeFormat( format, true, false )
				.replace("SSSSSS", "US")
				.replace("SSSSS", "US")
				.replace("SSSS", "US")
				.replace("SSS", "MS")
				.replace("SS", "MS")
				.replace("S", "MS")
				//use ISO day in week, as per DateTimeFormatter
				.replace("ee", "ID")
				.replace("e", "fmID")
				//TZR is TZ in Postgres
				.replace("zzz", "TZ")
				.replace("zz", "TZ")
				.replace("z", "TZ")
				.replace("xxx", "OF")
				.replace("xx", "OF")
				.replace("x", "OF");
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on Postgres
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	public String formatBinaryLiteral(byte[] bytes) {
		return "bytea '\\x" + StandardBasicTypes.BINARY.toString( bytes ) + "'";
	}

	@Override
	protected String wrapDateLiteral(String date) {
		return wrapAsAnsiDateLiteral(date);
	}

	@Override
	protected String wrapTimeLiteral(String time) {
		return wrapAsAnsiTimeLiteral(time);
	}

	@Override
	protected String wrapTimestampLiteral(String timestamp) {
		return "timestamp with time zone '" + timestamp + "'";
	}

	private String withTimeout(String lockString, int timeout) {
		switch (timeout) {
			case LockOptions.NO_WAIT:
				return supportsNoWait() ? lockString + " nowait" : lockString;
			case LockOptions.SKIP_LOCKED:
				return supportsSkipLocked() ? lockString + " skip locked" : lockString;
			default:
				return lockString;
		}
	}

	@Override
	public String getWriteLockString(int timeout) {
		return withTimeout( getForUpdateString(), timeout );
	}

	@Override
	public String getWriteLockString(String aliases, int timeout) {
		return withTimeout( getForUpdateString( aliases ), timeout );
	}

	@Override
	public String getReadLockString(int timeout) {
		return withTimeout(" for share", timeout );
	}

	@Override
	public String getReadLockString(String aliases, int timeout) {
		return withTimeout(" for share of " + aliases, timeout );
	}

	@Override
	public String getForUpdateNowaitString() {
		return supportsNoWait()
				? " for update nowait"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return supportsNoWait()
				? " for update of " + aliases + " nowait"
				: getForUpdateString(aliases);
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? " for update skip locked"
				: getForUpdateString();
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return supportsSkipLocked()
				? " for update of " + aliases + " skip locked"
				: getForUpdateString( aliases );
	}

	@Override
	public boolean supportsNoWait() {
		return getVersion() >= 810;
	}

	@Override
	public boolean supportsSkipLocked() {
		return getVersion() >= 950;
	}

	@Override
	public GroupBySummarizationRenderingStrategy getGroupBySummarizationRenderingStrategy() {
		return getVersion() >= 950 ? GroupBySummarizationRenderingStrategy.FUNCTION : GroupBySummarizationRenderingStrategy.NONE;
	}

	@Override
	public GroupByConstantRenderingStrategy getGroupByConstantRenderingStrategy() {
		return getVersion() >= 950 ? GroupByConstantRenderingStrategy.EMPTY_GROUPING : GroupByConstantRenderingStrategy.SUBQUERY;
	}

	@Override
	public void augmentRecognizedTableTypes(List<String> tableTypesList) {
		super.augmentRecognizedTableTypes( tableTypesList );
		if ( getVersion() >= 930 ) {
			tableTypesList.add( "MATERIALIZED VIEW" );

			/*
			 	PostgreSQL 10 and later adds support for Partition table.
			 */
			if ( getVersion() >= 1000 ) {
				tableTypesList.add( "PARTITIONED TABLE" );
			}
		}
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes(typeContributions, serviceRegistry);

		if ( getVersion() >= 820 ) {
			// HHH-9562
			typeContributions.contributeJdbcTypeDescriptor( PostgresUUIDType.INSTANCE );
		}
	}

	private static class PostgresUUIDType implements JdbcTypeDescriptor {
		/**
		 * Singleton access
		 */
		private static final PostgresUUIDType INSTANCE = new PostgresUUIDType();

		/**
		 * Postgres reports its UUID type as {@link java.sql.Types#OTHER}.  Unfortunately
		 * it reports a lot of its types as {@link java.sql.Types#OTHER}, making that
		 * value useless for distinguishing one SqlTypeDescriptor from another.
		 * So here we define a "magic value" that is a (hopefully no collisions)
		 * unique key within the {@link JdbcTypeDescriptorRegistry}
		 */
		private static final int JDBC_TYPE_CODE = 3975;

		@Override
		public int getJdbcType() {
			return JDBC_TYPE_CODE;
		}

		@Override
		public int getJdbcTypeCode() {
			return getJdbcType();
		}

		@Override
		public boolean canBeRemapped() {
			return true;
		}

		@Override
		public <J> BasicJavaDescriptor<J> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
			return (BasicJavaDescriptor<J>) typeConfiguration.getJavaTypeDescriptorRegistry().resolveDescriptor( UUID.class );
		}

		@Override
		public <T> JdbcLiteralFormatter<T> getJdbcLiteralFormatter(JavaTypeDescriptor<T> javaTypeDescriptor) {
			return null;
		}

		@Override
		public <X> ValueBinder<X> getBinder(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicBinder<X>( javaTypeDescriptor, this ) {
				@Override
				protected void doBind(
						PreparedStatement st,
						X value,
						int index,
						WrapperOptions wrapperOptions) throws SQLException {
					st.setObject( index, javaTypeDescriptor.unwrap( value, UUID.class, wrapperOptions ), Types.OTHER );
				}

				@Override
				protected void doBind(
						CallableStatement st,
						X value,
						String name,
						WrapperOptions wrapperOptions) throws SQLException {
					st.setObject( name, javaTypeDescriptor.unwrap( value, UUID.class, wrapperOptions ), Types.OTHER );
				}
			};
		}

		@Override
		public <X> ValueExtractor<X> getExtractor(JavaTypeDescriptor<X> javaTypeDescriptor) {
			return new BasicExtractor<X>( javaTypeDescriptor, this ) {
				@Override
				protected X doExtract(ResultSet rs, int position, WrapperOptions wrapperOptions) throws SQLException {
					return javaTypeDescriptor.wrap( rs.getObject( position ), wrapperOptions );
				}

				@Override
				protected X doExtract(CallableStatement statement, int position, WrapperOptions wrapperOptions) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( position ), wrapperOptions );
				}

				@Override
				protected X doExtract(CallableStatement statement, String name, WrapperOptions wrapperOptions) throws SQLException {
					return javaTypeDescriptor.wrap( statement.getObject( name ), wrapperOptions );
				}
			};
		}
	}
}
