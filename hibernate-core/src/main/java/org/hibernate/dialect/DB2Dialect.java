/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.LockOptions;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.function.CastingConcatFunction;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.CountFunction;
import org.hibernate.dialect.function.DB2FormatEmulation;
import org.hibernate.dialect.function.DB2PositionFunction;
import org.hibernate.dialect.function.DB2SubstringFunction;
import org.hibernate.dialect.identity.DB2IdentityColumnSupport;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.pagination.DB2LimitHandler;
import org.hibernate.dialect.pagination.LegacyDB2LimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.sequence.DB2SequenceSupport;
import org.hibernate.dialect.sequence.SequenceSupport;
import org.hibernate.dialect.unique.AlterTableUniqueIndexDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.jdbc.dialect.spi.DialectResolutionInfo;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelper;
import org.hibernate.engine.jdbc.env.spi.IdentifierHelperBuilder;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EntityMappingType;
import org.hibernate.metamodel.spi.RuntimeModelCreationContext;
import org.hibernate.query.sqm.IntervalType;
import org.hibernate.query.sqm.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.internal.cte.CteInsertStrategy;
import org.hibernate.query.sqm.mutation.internal.cte.CteMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableInsertStrategy;
import org.hibernate.query.sqm.mutation.spi.SqmMultiTableMutationStrategy;
import org.hibernate.query.sqm.produce.function.FunctionParameterType;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.SqlAstTranslatorFactory;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.spi.StandardSqlAstTranslatorFactory;
import org.hibernate.sql.ast.tree.Statement;
import org.hibernate.sql.exec.spi.JdbcOperation;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorDB2DatabaseImpl;
import org.hibernate.tool.schema.extract.internal.SequenceInformationExtractorNoOpImpl;
import org.hibernate.tool.schema.extract.spi.SequenceInformationExtractor;
import org.hibernate.type.JavaObjectType;
import org.hibernate.type.StandardBasicTypes;
import org.hibernate.type.descriptor.java.PrimitiveByteArrayJavaType;
import org.hibernate.type.descriptor.jdbc.*;
import org.hibernate.type.descriptor.jdbc.spi.JdbcTypeRegistry;
import org.hibernate.type.descriptor.sql.internal.CapacityDependentDdlType;
import org.hibernate.type.descriptor.sql.internal.DdlTypeImpl;
import org.hibernate.type.descriptor.sql.spi.DdlTypeRegistry;
import org.hibernate.type.spi.TypeConfiguration;

import java.sql.CallableStatement;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import jakarta.persistence.TemporalType;

import static org.hibernate.type.SqlTypes.*;

/**
 * A {@linkplain Dialect SQL dialect} for DB2.
 *
 * @author Gavin King
 */
public class DB2Dialect extends Dialect {

	final static DatabaseVersion MINIMUM_VERSION = DatabaseVersion.make( 10, 5 );
	private static final int BIND_PARAMETERS_NUMBER_LIMIT = 32_767;

	private static final String FOR_READ_ONLY_SQL = " for read only with rs";
	private static final String FOR_SHARE_SQL = FOR_READ_ONLY_SQL + " use and keep share locks";
	private static final String FOR_UPDATE_SQL = FOR_READ_ONLY_SQL + " use and keep update locks";
	private static final String SKIP_LOCKED_SQL = " skip locked data";
	private static final String FOR_SHARE_SKIP_LOCKED_SQL = FOR_SHARE_SQL + SKIP_LOCKED_SQL;
	private static final String FOR_UPDATE_SKIP_LOCKED_SQL = FOR_UPDATE_SQL + SKIP_LOCKED_SQL;

	private final LimitHandler limitHandler = getDB2Version().isBefore( 11, 1 )
			? LegacyDB2LimitHandler.INSTANCE
			: DB2LimitHandler.INSTANCE;
	private final UniqueDelegate uniqueDelegate = createUniqueDelegate();

	public DB2Dialect() {
		this( MINIMUM_VERSION );
	}

	public DB2Dialect(DialectResolutionInfo info) {
		super( info );
	}

	public DB2Dialect(DatabaseVersion version) {
		super( version );
	}

	@Override
	protected DatabaseVersion getMinimumSupportedVersion() {
		return MINIMUM_VERSION;
	}

	@Override
	protected void registerDefaultKeywords() {
		super.registerDefaultKeywords();
		//not keywords, at least not in DB2 11,
		//but perhaps they were in older versions?
		registerKeyword( "current" );
		registerKeyword( "date" );
		registerKeyword( "time" );
		registerKeyword( "timestamp" );
		registerKeyword( "fetch" );
		registerKeyword( "first" );
		registerKeyword( "rows" );
		registerKeyword( "only" );
	}

	/**
	 * DB2 LUW Version
	 */
	public DatabaseVersion getDB2Version() {
		return this.getVersion();
	}

	@Override
	public int getDefaultStatementBatchSize() {
		return 0;
	}

	@Override
	protected String columnType(int sqlTypeCode) {
		switch ( sqlTypeCode ) {
			case BOOLEAN:
				// prior to DB2 11, the 'boolean' type existed,
				// but was not allowed as a column type
				return getDB2Version().isBefore( 11 ) ? "smallint" : super.columnType( sqlTypeCode );
			case TINYINT:
				// no tinyint
				return "smallint";
			case NUMERIC:
				// HHH-12827: map them both to the same type to avoid problems with schema update
				// Note that 31 is the maximum precision DB2 supports
				return columnType( DECIMAL );
			case BLOB:
				return "blob";
			case CLOB:
				return "clob";
			case TIMESTAMP_WITH_TIMEZONE:
				return "timestamp($p)";
			case TIME_WITH_TIMEZONE:
				return "time";
			case VARBINARY:
				// should use 'varbinary' since version 11
				return getDB2Version().isBefore( 11 ) ? "varchar($l) for bit data" : super.columnType( sqlTypeCode );
			default:
				return super.columnType( sqlTypeCode );
		}
	}

	@Override
	protected void registerColumnTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.registerColumnTypes( typeContributions, serviceRegistry );
		final DdlTypeRegistry ddlTypeRegistry = typeContributions.getTypeConfiguration().getDdlTypeRegistry();

		ddlTypeRegistry.addDescriptor( new DdlTypeImpl( SQLXML, "xml", this ) );
		ddlTypeRegistry.addDescriptor(
				CapacityDependentDdlType.builder( BINARY, "varchar($l) for bit data", this )
						.withTypeCapacity( 254, "char($l) for bit data" )
						.build()
		);
	}

	protected UniqueDelegate createUniqueDelegate() {
		return new AlterTableUniqueIndexDelegate( this );
	}

	@Override
	public int getMaxVarcharLength() {
		return 32_672;
	}

	@Override
	public int getDefaultDecimalPrecision() {
		//this is the maximum allowed in DB2
		return 31;
	}

	@Override
	protected boolean supportsPredicateAsExpression() {
		return getDB2Version().isSameOrAfter( 11 );
	}

	@Override
	public boolean supportsDistinctFromPredicate() {
		return getDB2Version().isSameOrAfter( 11, 1 );
	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory functionFactory = new CommonFunctionFactory(queryEngine);
		// AVG by default uses the input type, so we possibly need to cast the argument type, hence a special function
		functionFactory.avg_castingNonDoubleArguments( this, SqlAstNodeRenderingMode.DEFAULT );

		functionFactory.cot();
		functionFactory.sinh();
		functionFactory.cosh();
		functionFactory.tanh();
		functionFactory.degrees();
		functionFactory.log10();
		functionFactory.radians();
		functionFactory.rand();
		functionFactory.soundex();
		functionFactory.trim2();
		functionFactory.space();
		functionFactory.repeat();
		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "substr" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.STRING )
				)
				.setArgumentCountBetween( 2, 3 )
				.setParameterTypes(FunctionParameterType.STRING, FunctionParameterType.INTEGER, FunctionParameterType.INTEGER)
				.setArgumentListSignature( "(STRING string, INTEGER start[, INTEGER length])" )
				.register();
		queryEngine.getSqmFunctionRegistry().register(
				"substring",
				new DB2SubstringFunction( queryEngine.getTypeConfiguration() )
		);
		functionFactory.translate();
		functionFactory.bitand();
		functionFactory.bitor();
		functionFactory.bitxor();
		functionFactory.bitnot();
		functionFactory.yearMonthDay();
		functionFactory.hourMinuteSecond();
		functionFactory.dayofweekmonthyear();
		functionFactory.weekQuarter();
		functionFactory.daynameMonthname();
		functionFactory.lastDay();
		functionFactory.toCharNumberDateTimestamp();
		functionFactory.dateTimeTimestamp();
		functionFactory.concat_pipeOperator();
		functionFactory.octetLength();
		functionFactory.ascii();
		functionFactory.char_chr();
		functionFactory.trunc();
		functionFactory.truncate();
		functionFactory.insert();
		functionFactory.characterLength_length( SqlAstNodeRenderingMode.DEFAULT );
		functionFactory.stddev();
		functionFactory.regrLinearRegressionAggregates();
		functionFactory.variance();
		functionFactory.hypotheticalOrderedSetAggregates_windowEmulation();
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			functionFactory.position();
			functionFactory.overlayLength_overlay( false );
			functionFactory.median();
			functionFactory.inverseDistributionOrderedSetAggregates();
			functionFactory.stddevPopSamp();
			functionFactory.varPopSamp();
			functionFactory.varianceSamp();
		}
		else {
			// Before version 11, the position function required the use of the code units
			queryEngine.getSqmFunctionRegistry().register(
					"position",
					new DB2PositionFunction( queryEngine.getTypeConfiguration() )
			);
			// Before version 11, the overlay function required the use of the code units
			functionFactory.overlayLength_overlay( true );
			// ordered set aggregate functions are only available as of version 11, and we can't reasonably emulate them
			// so no percent_rank, cume_dist, median, mode, percentile_cont or percentile_disc
			queryEngine.getSqmFunctionRegistry().registerAlternateKey( "stddev_pop", "stddev" );
			functionFactory.stddevSamp_sumCount();
			queryEngine.getSqmFunctionRegistry().registerAlternateKey( "var_pop", "variance" );
			functionFactory.varSamp_sumCount();
		}

		functionFactory.addYearsMonthsDaysHoursMinutesSeconds();
		functionFactory.yearsMonthsDaysHoursMinutesSecondsBetween();
		functionFactory.dateTrunc();
		functionFactory.bitLength_pattern( "length(?1)*8" );

		// DB2 wants parameter operands to be casted to allow lengths bigger than 255
		queryEngine.getSqmFunctionRegistry().register(
				"concat",
				new CastingConcatFunction(
						this,
						"||",
						true,
						SqlAstNodeRenderingMode.NO_PLAIN_PARAMETER,
						queryEngine.getTypeConfiguration()
				)
		);
		// For the count distinct emulation distinct
		queryEngine.getSqmFunctionRegistry().register(
				"count",
				new CountFunction(
						this,
						queryEngine.getTypeConfiguration(),
						SqlAstNodeRenderingMode.DEFAULT,
						"||",
						queryEngine.getTypeConfiguration().getDdlTypeRegistry().getDescriptor( VARCHAR )
								.getCastTypeName(
										queryEngine.getTypeConfiguration()
												.getBasicTypeRegistry()
												.resolve( StandardBasicTypes.STRING ),
										null,
										null,
										null
								),
						true
				)
		);

		queryEngine.getSqmFunctionRegistry().register(
				"format",
				new DB2FormatEmulation( queryEngine.getTypeConfiguration() )
		);

		queryEngine.getSqmFunctionRegistry().namedDescriptorBuilder( "posstr" )
				.setInvariantType(
						queryEngine.getTypeConfiguration().getBasicTypeRegistry().resolve( StandardBasicTypes.INTEGER )
				)
				.setExactArgumentCount( 2 )
				.setParameterTypes(FunctionParameterType.STRING, FunctionParameterType.STRING)
				.setArgumentListSignature("(STRING string, STRING pattern)")
				.register();

		functionFactory.windowFunctions();
		functionFactory.listagg( null );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		return new String[] {"drop schema " + schemaName + " restrict"};
	}

	/**
	 * Since we're using {@code seconds_between()} and
	 * {@code add_seconds()}, it makes sense to use
	 * seconds as the "native" precision.
	 */
	@Override
	public long getFractionalSecondPrecisionInNanos() {
		//Note that DB2 actually supports all the way up to
		//thousands-of-nanoseconds precision for timestamps!
		//i.e. timestamp(12)
		return 1_000_000_000; //seconds
	}

	@Override
	public String timestampdiffPattern(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		if ( getDB2Version().isBefore( 11 ) ) {
			return timestampdiffPatternV10( unit, fromTemporalType, toTemporalType );
		}
		StringBuilder pattern = new StringBuilder();
		boolean castFrom = fromTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		boolean castTo = toTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		switch ( unit ) {
			case NATIVE:
			case NANOSECOND:
				pattern.append( "(seconds_between(" );
				break;
			//note: DB2 does have weeks_between()
			case MONTH:
			case QUARTER:
				// the months_between() function results
				// in a non-integral value, so trunc() it
				pattern.append( "trunc(months_between(" );
				break;
			default:
				pattern.append( "?1s_between(" );
		}
		if ( castTo ) {
			pattern.append( "cast(?3 as timestamp)" );
		}
		else {
			pattern.append( "?3" );
		}
		pattern.append( ',' );
		if ( castFrom ) {
			pattern.append( "cast(?2 as timestamp)" );
		}
		else {
			pattern.append( "?2" );
		}
		pattern.append( ')' );
		switch ( unit ) {
			case NATIVE:
				pattern.append( "+(microsecond(?3)-microsecond(?2))/1e6)" );
				break;
			case NANOSECOND:
				pattern.append( "*1e9+(microsecond(?3)-microsecond(?2))*1e3)" );
				break;
			case MONTH:
				pattern.append( ')' );
				break;
			case QUARTER:
				pattern.append( "/3)" );
				break;
		}
		return pattern.toString();
	}

	public static String timestampdiffPatternV10(TemporalUnit unit, TemporalType fromTemporalType, TemporalType toTemporalType) {
		final boolean castFrom = fromTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		final boolean castTo = toTemporalType != TemporalType.TIMESTAMP && !unit.isDateUnit();
		final String fromExpression = castFrom ? "cast(?2 as timestamp)" : "?2";
		final String toExpression = castTo ? "cast(?3 as timestamp)" : "?3";
		switch ( unit ) {
			case NATIVE:
				return "(select (days(t2)-days(t1))*86400+(midnight_seconds(t2)-midnight_seconds(t1))+(microsecond(t2)-microsecond(t1))/1e6 " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
			case NANOSECOND:
				return "(select (days(t2)-days(t1))*86400+(midnight_seconds(t2)-midnight_seconds(t1))*1e9+(microsecond(t2)-microsecond(t1))*1e3 " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
			case SECOND:
				return "(select (days(t2)-days(t1))*86400+(midnight_seconds(t2)-midnight_seconds(t1)) " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
			case MINUTE:
				return "(select (days(t2)-days(t1))*1440+(midnight_seconds(t2)-midnight_seconds(t1))/60 from " +
						"lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
			case HOUR:
				return "(select (days(t2)-days(t1))*24+(midnight_seconds(t2)-midnight_seconds(t1))/3600 " +
						"from lateral(values(" + fromExpression + ',' + toExpression + ")) as temp(t1,t2))";
			case YEAR:
				return "(year(" + toExpression + ")-year(" + fromExpression + "))";
			// the months_between() function results
			// in a non-integral value, so trunc() it
			case MONTH:
				return "trunc(months_between(" + toExpression + ',' + fromExpression + "))";
			case QUARTER:
				return "trunc(months_between(" + toExpression + ',' + fromExpression + ")/3)";
			case WEEK:
				return "int((days" + toExpression + ")-days(" + fromExpression + "))/7)";
			case DAY:
				return "(days(" + toExpression + ")-days(" + fromExpression + "))";
			default:
				throw new UnsupportedOperationException( "Unsupported unit: " + unit );
		}
	}

	@Override
	public String timestampaddPattern(TemporalUnit unit, TemporalType temporalType, IntervalType intervalType) {
		final StringBuilder pattern = new StringBuilder();
		final boolean castTo;
		if ( unit.isDateUnit() ) {
			castTo = temporalType == TemporalType.TIME;
		}
		else {
			castTo = temporalType == TemporalType.DATE;
		}
		if (castTo) {
			pattern.append("cast(?3 as timestamp)");
		}
		else {
			pattern.append("?3");
		}
		pattern.append("+(");
		// DB2 supports temporal arithmetic. See https://www.ibm.com/support/knowledgecenter/en/SSEPGG_9.7.0/com.ibm.db2.luw.sql.ref.doc/doc/r0023457.html
		switch (unit) {
			case NATIVE:
				// AFAICT the native format is seconds with fractional parts after the decimal point
				pattern.append("?2) seconds");
				break;
			case NANOSECOND:
				pattern.append("(?2)/1e9) seconds");
				break;
			case WEEK:
				pattern.append("(?2)*7) days");
				break;
			case QUARTER:
				pattern.append("(?2)*3) months");
				break;
			default:
				pattern.append("?2) ?1s");
		}
		return pattern.toString();
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getCreateIndexTail(boolean unique, List<Column> columns) {
		return unique ? " exclude null keys" : "";
	}

	@Override
	public SequenceSupport getSequenceSupport() {
		return DB2SequenceSupport.INSTANCE;
	}

	@Override
	public String getQuerySequencesString() {
		return "select * from syscat.sequences";
	}

	@Override
	public SequenceInformationExtractor getSequenceInformationExtractor() {
		if ( getQuerySequencesString() == null ) {
			return SequenceInformationExtractorNoOpImpl.INSTANCE;
		}
		else {
			return SequenceInformationExtractorDB2DatabaseImpl.INSTANCE;
		}
	}

	@Override
	public String getForUpdateString() {
		return FOR_UPDATE_SQL;
	}

	@Override
	public boolean supportsSkipLocked() {
		// Introduced in 11.5: https://www.ibm.com/docs/en/db2/11.5?topic=statement-concurrent-access-resolution-clause
		return getDB2Version().isSameOrAfter( 11, 5 );
	}

	@Override
	public String getForUpdateSkipLockedString() {
		return supportsSkipLocked()
				? FOR_UPDATE_SKIP_LOCKED_SQL
				: FOR_UPDATE_SQL;
	}

	@Override
	public String getForUpdateSkipLockedString(String aliases) {
		return getForUpdateSkipLockedString();
	}

	@Override
	public String getWriteLockString(int timeout) {
		return timeout == LockOptions.SKIP_LOCKED && supportsSkipLocked()
				? FOR_UPDATE_SKIP_LOCKED_SQL
				: FOR_UPDATE_SQL;
	}

	@Override
	public String getReadLockString(int timeout) {
		return timeout == LockOptions.SKIP_LOCKED && supportsSkipLocked()
				? FOR_SHARE_SKIP_LOCKED_SQL
				: FOR_SHARE_SQL;
	}

	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		//as far as I know, DB2 doesn't support this
		return false;
	}

	@Override
	public boolean requiresCastForConcatenatingNonStrings() {
		return true;
	}

	@Override
	public String getSelectClauseNullString(int sqlType, TypeConfiguration typeConfiguration) {
		return selectNullString(sqlType);
	}

	public static String selectNullString(int sqlType) {
		String literal;
		switch ( sqlType ) {
			case Types.VARCHAR:
			case Types.CHAR:
				literal = "''";
				break;
			case Types.DATE:
				literal = "'2000-1-1'";
				break;
			case Types.TIME:
				literal = "'00:00:00'";
				break;
			case Types.TIMESTAMP:
			case Types.TIMESTAMP_WITH_TIMEZONE:
				literal = "'2000-1-1 00:00:00'";
				break;
			default:
				literal = "0";
		}
		return "nullif(" + literal + "," + literal + ')';
	}

	@Override
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		// This assumes you will want to ignore any update counts
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}

		return ps.getResultSet();
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public SqmMultiTableMutationStrategy getFallbackSqmMutationStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteMutationStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public SqmMultiTableInsertStrategy getFallbackSqmInsertStrategy(
			EntityMappingType rootEntityDescriptor,
			RuntimeModelCreationContext runtimeModelCreationContext) {
		return new CteInsertStrategy( rootEntityDescriptor, runtimeModelCreationContext );
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "values current timestamp";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsParametersInInsertSelect() {
		return true;
	}

	@Override
	public boolean supportsResultSetPositionQueryMethodsOnForwardOnlyCursor() {
		return false;
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsLobValueChangePropagation() {
		return false;
	}

	@Override
	public boolean doesReadCommittedCauseWritersToBlockReaders() {
		return true;
	}

	@Override
	public boolean supportsTupleDistinctCounts() {
		return false;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		final JdbcTypeRegistry jdbcTypeRegistry = typeContributions.getTypeConfiguration().getJdbcTypeRegistry();

		if ( getDB2Version().isBefore( 11 ) ) {
			jdbcTypeRegistry.addDescriptor( Types.BOOLEAN, SmallIntJdbcType.INSTANCE );
			// Binary literals were only added in 11. See https://www.ibm.com/support/knowledgecenter/SSEPGG_11.1.0/com.ibm.db2.luw.sql.ref.doc/doc/r0000731.html#d79816e393
			jdbcTypeRegistry.addDescriptor( Types.VARBINARY, VarbinaryJdbcType.INSTANCE_WITHOUT_LITERALS );
			if ( getDB2Version().isBefore( 9, 7 ) ) {
				jdbcTypeRegistry.addDescriptor( Types.NUMERIC, DecimalJdbcType.INSTANCE );
			}
		}
		// See HHH-12753
		// It seems that DB2's JDBC 4.0 support as of 9.5 does not
		// support the N-variant methods like NClob or NString.
		// Therefore here we overwrite the sql type descriptors to
		// use the non-N variants which are supported.
		jdbcTypeRegistry.addDescriptor( Types.NCHAR, CharJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor(
				Types.NCLOB,
				useInputStreamToInsertBlob()
						? ClobJdbcType.STREAM_BINDING
						: ClobJdbcType.CLOB_BINDING
		);
		jdbcTypeRegistry.addDescriptor( Types.NVARCHAR, VarcharJdbcType.INSTANCE );
		jdbcTypeRegistry.addDescriptor( Types.NUMERIC, DecimalJdbcType.INSTANCE );

		jdbcTypeRegistry.addDescriptor( XmlJdbcType.INSTANCE );

		// DB2 requires a custom binder for binding untyped nulls that resolves the type through the statement
		typeContributions.contributeJdbcType( ObjectNullResolvingJdbcType.INSTANCE );

		// Until we remove StandardBasicTypes, we have to keep this
		typeContributions.contributeType(
				new JavaObjectType(
						ObjectNullResolvingJdbcType.INSTANCE,
						typeContributions.getTypeConfiguration()
								.getJavaTypeRegistry()
								.getDescriptor( Object.class )
				)
		);
	}

	@Override
	public void appendBinaryLiteral(SqlAppender appender, byte[] bytes) {
		if ( getDB2Version().isSameOrAfter( 11 ) ) {
			appender.appendSql( "BX'" );
		}
		else {
			// This should be fine on DB2 prior to 10
			appender.appendSql( "X'" );
		}
		PrimitiveByteArrayJavaType.INSTANCE.appendString( appender, bytes );
		appender.appendSql( '\'' );
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return (sqlException, message, sql) -> {
			final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqlException );

			if ( -952 == errorCode && "57014".equals( sqlState ) ) {
				throw new LockTimeoutException( message, sqlException, sql );
			}
			return null;
		};
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public int getMaxIdentifierLength() {
		return 128;
	}

	@Override
	public LimitHandler getLimitHandler() {
		return limitHandler;
	}

	@Override
	public boolean supportsNullPrecedence() {
		return false;
	}

	@Override
	public SqlAstTranslatorFactory getSqlAstTranslatorFactory() {
		return new StandardSqlAstTranslatorFactory() {
			@Override
			protected <T extends JdbcOperation> SqlAstTranslator<T> buildTranslator(
					SessionFactoryImplementor sessionFactory, Statement statement) {
				return new DB2SqlAstTranslator<>( sessionFactory, statement );
			}
		};
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new DB2IdentityColumnSupport();
	}

	@Override
	public boolean supportsValuesList() {
		return true;
	}

	@Override
	public boolean supportsPartitionBy() {
		return true;
	}

	@Override
	public boolean supportsNonQueryWithCTE() {
		return true;
	}

	@Override
	public boolean supportsRecursiveCTE() {
		// Supported at last since 9.7
		return true;
	}

	@Override
	public boolean supportsOffsetInSubquery() {
		return true;
	}

	@Override
	public boolean supportsWindowFunctions() {
		return true;
	}

	@Override
	public boolean supportsLateral() {
		return true;
	}

	@Override
	public void appendDatetimeFormat(SqlAppender appender, String format) {
		//DB2 does not need nor support FM
		appender.appendSql( OracleDialect.datetimeFormat( format, false, false ).result() );
	}

	@Override
	public String translateExtractField(TemporalUnit unit) {
		switch ( unit ) {
			//WEEK means the ISO week number on DB2
			case DAY_OF_MONTH: return "day";
			case DAY_OF_YEAR: return "doy";
			case DAY_OF_WEEK: return "dow";
			default: return super.translateExtractField( unit );
		}
	}

	@Override
	public void appendBooleanValueString(SqlAppender appender, boolean bool) {
		if ( getDB2Version().isBefore( 11 ) ) {
			appender.appendSql( bool ? '1' : '0' );
		}
		else {
			appender.appendSql( bool );
		}
	}

	@Override
	public String extractPattern(TemporalUnit unit) {
		switch ( unit ) {
			case WEEK:
				// Not sure why, but `extract(week from '2019-05-27')` wrongly returns 21 and week_iso behaves correct
				return "week_iso(?2)";
			case DAY_OF_YEAR:
				return "dayofyear(?2)";
			case DAY_OF_WEEK:
				return "dayofweek(?2)";
			case QUARTER:
				return "quarter(?2)";
		}
		return super.extractPattern( unit );
	}

	@Override
	public int getInExpressionCountLimit() {
		return BIND_PARAMETERS_NUMBER_LIMIT;
	}

	@Override
	public String generatedAs(String generatedAs) {
		return " generated always as (" + generatedAs + ")";
	}

	@Override
	public IdentifierHelper buildIdentifierHelper(IdentifierHelperBuilder builder, DatabaseMetaData dbMetaData)
			throws SQLException {
		builder.setAutoQuoteInitialUnderscore(true);
		return super.buildIdentifierHelper(builder, dbMetaData);
	}

	@Override
	public boolean canDisableConstraints() {
		return true;
	}

	@Override
	public String getDisableConstraintStatement(String tableName, String name) {
		return "alter table " + tableName + " alter foreign key " + name + " not enforced";
	}

	@Override
	public String getEnableConstraintStatement(String tableName, String name) {
		return "alter table " + tableName + " alter foreign key " + name + " enforced";
	}

	@Override
	public String getTruncateTableStatement(String tableName) {
		return super.getTruncateTableStatement(tableName) + " immediate";
	}
}
