/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.JDBCException;
import org.hibernate.NullPrecedence;
import org.hibernate.PessimisticLockException;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.function.MySQLExtractEmulation;
import org.hibernate.dialect.identity.IdentityColumnSupport;
import org.hibernate.dialect.identity.MySQLIdentityColumnSupport;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.dialect.unique.MySQLUniqueDelegate;
import org.hibernate.dialect.unique.UniqueDelegate;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.exception.LockAcquisitionException;
import org.hibernate.exception.LockTimeoutException;
import org.hibernate.exception.spi.SQLExceptionConversionDelegate;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.query.TemporalUnit;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.query.sqm.mutation.spi.idtable.StandardIdTableSupport;
import org.hibernate.query.sqm.mutation.spi.SqmMutationStrategy;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTable;
import org.hibernate.query.sqm.mutation.spi.idtable.IdTableSupport;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTempTableExporter;
import org.hibernate.query.sqm.mutation.spi.idtable.LocalTemporaryTableStrategy;
import org.hibernate.sql.SqlExpressableType;
import org.hibernate.tool.schema.spi.Exporter;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import static org.hibernate.query.TemporalUnit.NANOSECOND;

/**
 * An SQL dialect for MySQL (prior to 5.x).
 *
 * @author Gavin King
 */
@SuppressWarnings("deprecation")
public class MySQLDialect extends Dialect {

	private final UniqueDelegate uniqueDelegate;
	private MySQLStorageEngine storageEngine;

	private static final LimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			return sql + (hasOffset ? " limit ?, ?" : " limit ?");
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}
	};

	/**
	 * Constructs a MySQLDialect
	 */
	public MySQLDialect() {
		super();

		String storageEngine = Environment.getProperties().getProperty( Environment.STORAGE_ENGINE );
		if(storageEngine == null) {
			storageEngine = System.getProperty( Environment.STORAGE_ENGINE );
		}
		if(storageEngine == null) {
			this.storageEngine = getDefaultMySQLStorageEngine();
		}
		else if( "innodb".equals( storageEngine.toLowerCase() ) ) {
			this.storageEngine = InnoDBStorageEngine.INSTANCE;
		}
		else if( "myisam".equals( storageEngine.toLowerCase() ) ) {
			this.storageEngine = MyISAMStorageEngine.INSTANCE;
		}
		else {
			throw new UnsupportedOperationException( "The " + storageEngine + " storage engine is not supported!" );
		}

		registerColumnType( Types.BOOLEAN, "bit" ); // HHH-6935: Don't use "boolean" i.e. tinyint(1) due to JDBC ResultSetMetaData

		registerColumnType( Types.NUMERIC, "decimal($p,$s)" ); //it's just a synonym

		registerVarcharTypes();

		registerColumnType( Types.TIMESTAMP, "datetime" );
		registerColumnType( Types.TIMESTAMP_WITH_TIMEZONE, "timestamp" );

		registerColumnType( Types.BLOB, "longblob" );
//		registerColumnType( Types.BLOB, 16777215, "mediumblob" );
//		registerColumnType( Types.BLOB, 65535, "blob" );
		registerColumnType( Types.CLOB, "longtext" );
//		registerColumnType( Types.CLOB, 16777215, "mediumtext" );
//		registerColumnType( Types.CLOB, 65535, "text" );
		registerColumnType( Types.NCLOB, "longtext" );

		registerKeyword( "key" );

		getDefaultProperties().setProperty( Environment.MAX_FETCH_DEPTH, "2" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );

		uniqueDelegate = new MySQLUniqueDelegate( this );
	}

	void upgradeTo57() {
		// Since 5.7 we can explicitly specify a fractional second
		// precision for the timestamp-like types
		registerColumnType(Types.TIMESTAMP, "datetime($p)");
		registerColumnType(Types.TIMESTAMP_WITH_TIMEZONE, "timestamp($p)");

		// MySQL 5.7 brings JSON native support with a dedicated datatype
		// https://dev.mysql.com/doc/refman/5.7/en/json.html
		registerColumnType(Types.JAVA_OBJECT, "json");

	}

	void upgradeTo57(QueryEngine queryEngine) {
		// MySQL timestamp type defaults to precision 0 (seconds) but
		// we want the standard default precision of 6 (microseconds)
		CommonFunctionFactory.currentTimestampExplicitMicros( queryEngine );
		CommonFunctionFactory.sysdateExplicitMicros( queryEngine );

	}

//	@Override
//	public int getDefaultDecimalPrecision() {
//		//this is the maximum, but I guess it's too high
//		return 65;
//	}

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry( queryEngine );

		CommonFunctionFactory.soundex( queryEngine );
		CommonFunctionFactory.radians( queryEngine );
		CommonFunctionFactory.degrees( queryEngine );
		CommonFunctionFactory.cot( queryEngine );
		CommonFunctionFactory.log( queryEngine );
		CommonFunctionFactory.log2( queryEngine );
		CommonFunctionFactory.log10( queryEngine );
		CommonFunctionFactory.pi( queryEngine );
		CommonFunctionFactory.trim2( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.reverse( queryEngine );
		CommonFunctionFactory.space( queryEngine );
		CommonFunctionFactory.repeat( queryEngine );
		CommonFunctionFactory.md5( queryEngine );
		CommonFunctionFactory.yearMonthDay( queryEngine );
		CommonFunctionFactory.hourMinuteSecond( queryEngine );
		CommonFunctionFactory.dayofweekmonthyear( queryEngine );
		CommonFunctionFactory.weekQuarter( queryEngine );
		CommonFunctionFactory.daynameMonthname( queryEngine );
		CommonFunctionFactory.lastDay( queryEngine );
		CommonFunctionFactory.dateTimeTimestamp( queryEngine );
		CommonFunctionFactory.utcDateTimeTimestamp( queryEngine );
		CommonFunctionFactory.rand( queryEngine );
		CommonFunctionFactory.crc32( queryEngine );
		CommonFunctionFactory.sha1( queryEngine );
		CommonFunctionFactory.sha2( queryEngine );
		CommonFunctionFactory.sha( queryEngine );
		CommonFunctionFactory.bitLength( queryEngine );
		CommonFunctionFactory.octetLength( queryEngine );
		CommonFunctionFactory.ascii( queryEngine );
		CommonFunctionFactory.chr_char( queryEngine );
		CommonFunctionFactory.instr( queryEngine );
		CommonFunctionFactory.substr( queryEngine );
		//also natively supports ANSI-style substring()
		CommonFunctionFactory.position( queryEngine );
		CommonFunctionFactory.sysdateParens( queryEngine );
		CommonFunctionFactory.nowCurdateCurtime( queryEngine );
		CommonFunctionFactory.truncate( queryEngine );
		CommonFunctionFactory.insert( queryEngine );
		CommonFunctionFactory.bitandorxornot_operator( queryEngine );
		CommonFunctionFactory.bitAndOr( queryEngine );
		CommonFunctionFactory.stddev( queryEngine );
		CommonFunctionFactory.stddevPopSamp( queryEngine );
		CommonFunctionFactory.variance( queryEngine );
		CommonFunctionFactory.varPopSamp( queryEngine );
		CommonFunctionFactory.datediff( queryEngine );
		CommonFunctionFactory.adddateSubdateAddtimeSubtime( queryEngine );
		CommonFunctionFactory.formatdatetime_dateFormat( queryEngine );
		CommonFunctionFactory.makedateMaketime( queryEngine );

		queryEngine.getSqmFunctionRegistry().register( "extract", new MySQLExtractEmulation() );
	}

	@Override
	public void timestampadd(TemporalUnit unit, Renderer magnitude, Renderer to, Appender sqlAppender, boolean timestamp) {
		sqlAppender.append("timestampadd(");
		if ( unit == NANOSECOND ) {
			sqlAppender.append("microsecond");
		}
		else {
			sqlAppender.append( unit.toString() );
		}
		sqlAppender.append(", ");
		if ( unit == NANOSECOND ) {
			sqlAppender.append("(");
		}
		magnitude.render();
		if ( unit == NANOSECOND ) {
			sqlAppender.append(")/1e3");
		}
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
	}

	@Override
	public void timestampdiff(TemporalUnit unit, Renderer from, Renderer to, Appender sqlAppender, boolean fromTimestamp, boolean toTimestamp) {
		sqlAppender.append("timestampdiff(");
		if ( unit == NANOSECOND ) {
			sqlAppender.append("microsecond");
		}
		else {
			sqlAppender.append( unit.toString() );
		}
		sqlAppender.append(", ");
		from.render();
		sqlAppender.append(", ");
		to.render();
		sqlAppender.append(")");
		if ( unit == NANOSECOND ) {
			sqlAppender.append("*1e3");
		}
	}

	protected void registerVarcharTypes() {
		registerColumnType( Types.VARCHAR, "longtext" );
//		registerColumnType( Types.VARCHAR, 16777215, "mediumtext" );
//		registerColumnType( Types.VARCHAR, 65535, "text" );
		registerColumnType( Types.VARCHAR, 255, "varchar($l)" );
		registerColumnType( Types.LONGVARCHAR, "longtext" );

		registerColumnType( Types.VARBINARY, "longblob" );
//		registerColumnType( Types.VARBINARY, 16777215, "mediumblob" );
//		registerColumnType( Types.VARBINARY, 65535, "bloc" );
		registerColumnType( Types.VARBINARY, 255, "varbinary($l)" );
		registerColumnType( Types.LONGVARBINARY, "longblob" );
	}

	@Override
	public String getAddColumnString() {
		return "add column";
	}

	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	@Override
	public String getAddForeignKeyConstraintString(
			String constraintName,
			String[] foreignKey,
			String referencedTable,
			String[] primaryKey,
			boolean referencesPrimaryKey) {
		final String cols = String.join( ", ", foreignKey );
		final String referencedCols = String.join( ", ", primaryKey );
		return String.format(
				" add constraint %s foreign key (%s) references %s (%s)",
				constraintName,
				cols,
				referencedTable,
				referencedCols
		);
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getDropForeignKeyString() {
		return " drop foreign key ";
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {
		return sql + (hasOffset ? " limit ?, ?" : " limit ?");
	}

	@Override
	public char closeQuote() {
		return '`';
	}

	@Override
	public char openQuote() {
		return '`';
	}

	@Override
	public boolean canCreateCatalog() {
		return true;
	}

	@Override
	public String[] getCreateCatalogCommand(String catalogName) {
		return new String[] { "create database " + catalogName };
	}

	@Override
	public String[] getDropCatalogCommand(String catalogName) {
		return new String[] { "drop database " + catalogName };
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}

	@Override
	public String[] getCreateSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "MySQL does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public String[] getDropSchemaCommand(String schemaName) {
		throw new UnsupportedOperationException( "MySQL does not support dropping creating/dropping schemas in the JDBC sense" );
	}

	@Override
	public boolean supportsIfExistsBeforeTableName() {
		return true;
	}

	@Override
	public String getSelectGUIDString() {
		return "select uuid()";
	}

	@Override
	public String getTableComment(String comment) {
		return " comment='" + comment + "'";
	}

	@Override
	public String getColumnComment(String comment) {
		return " comment '" + comment + "'";
	}

	@Override
	public SqmMutationStrategy getDefaultIdTableStrategy() {
		return new LocalTemporaryTableStrategy( generateIdTableSupport() );
	}

	private IdTableSupport generateIdTableSupport() {
		return new StandardIdTableSupport( generateIdTableExporter() );
	}

	private Exporter<IdTable> generateIdTableExporter() {
		return new LocalTempTableExporter() {
			@Override
			public String getCreateCommand() {
				return "create temporary table if not exists";
			}

			@Override
			public String getDropCommand() {
				return "drop temporary table";
			}
		};
	}

	@Override
	public String getCastTypeName(SqlExpressableType type, Long length, Integer precision, Integer scale) {
		switch ( type.getSqlTypeDescriptor().getJdbcTypeCode() ) {
			case Types.INTEGER:
			case Types.BIGINT:
			case Types.SMALLINT:
			case Types.TINYINT:
				//MySQL doesn't let you cast to INTEGER/BIGINT/TINYINT
				return "signed";
			case Types.FLOAT:
			case Types.DOUBLE:
			case Types.REAL:
				//MySQL doesn't let you cast to DOUBLE/FLOAT
				//but don't just return 'decimal' because
				//the default scale is 0 (no decimal places)
				return String.format(
						"decimal(%d, %d)",
						precision == null ? type.getJavaTypeDescriptor().getDefaultSqlPrecision(this) : precision,
						scale == null ? type.getJavaTypeDescriptor().getDefaultSqlScale() : scale
				);
			case Types.VARBINARY:
			case Types.LONGVARBINARY:
				//MySQL doesn't let you cast to BLOB/TINYBLOB/LONGBLOB
				//we could just return 'binary' here but that would be
				//inconsistent with other Dialects which need a length
				return String.format(
						"binary(%d)",
						length == null ? type.getJavaTypeDescriptor().getDefaultSqlLength(this) : length
				);
			case Types.VARCHAR:
			case Types.LONGVARCHAR:
				//MySQL doesn't let you cast to TEXT/LONGTEXT
				//we could just return 'char' here but that would be
				//inconsistent with other Dialects which need a length
				return String.format(
						"char(%d)",
						length == null ? type.getJavaTypeDescriptor().getDefaultSqlLength(this) : length
				);
			default:
				return super.getCastTypeName( type, length, precision, scale );
		}
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
	public int registerResultSetOutParameter(CallableStatement statement, int col) throws SQLException {
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		boolean isResultSet = ps.execute();
		while ( !isResultSet && ps.getUpdateCount() != -1 ) {
			isResultSet = ps.getMoreResults();
		}
		return ps.getResultSet();
	}

	@Override
	public UniqueDelegate getUniqueDelegate() {
		return uniqueDelegate;
	}

	@Override
	public boolean supportsRowValueConstructorSyntax() {
		return true;
	}

	@Override
	public String renderOrderByElement(String expression, String collation, String order, NullPrecedence nulls) {
		final StringBuilder orderByElement = new StringBuilder();
		if ( nulls != NullPrecedence.NONE ) {
			// Workaround for NULLS FIRST / LAST support.
			orderByElement.append( "case when " ).append( expression ).append( " is null then " );
			if ( nulls == NullPrecedence.FIRST ) {
				orderByElement.append( "0 else 1" );
			}
			else {
				orderByElement.append( "1 else 0" );
			}
			orderByElement.append( " end, " );
		}
		// Nulls precedence has already been handled so passing NONE value.
		orderByElement.append( super.renderOrderByElement( expression, collation, order, NullPrecedence.NONE ) );
		return orderByElement.toString();
	}

	// locking support

	@Override
	public String getForUpdateString() {
		return " for update";
	}

	@Override
	public String getWriteLockString(int timeout) {
		return " for update";
	}

	@Override
	public String getReadLockString(int timeout) {
		return " lock in share mode";
	}


	// Overridden informational metadata ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean areStringComparisonsCaseInsensitive() {
		return true;
	}

	@Override
	public boolean supportsLobValueChangePropogation() {
		// note: at least my local MySQL 5.1 install shows this not working...
		return false;
	}

	@Override
	public boolean supportsSubqueryOnMutatingTable() {
		return false;
	}

	@Override
	public boolean supportsLockTimeouts() {
		// yes, we do handle "lock timeout" conditions in the exception conversion delegate,
		// but that's a hardcoded lock timeout period across the whole entire MySQL database.
		// MySQL does not support specifying lock timeouts as part of the SQL statement, which is really
		// what this meta method is asking.
		return false;
	}

	@Override
	public SQLExceptionConversionDelegate buildSQLExceptionConversionDelegate() {
		return new SQLExceptionConversionDelegate() {
			@Override
			public JDBCException convert(SQLException sqlException, String message, String sql) {
				switch ( sqlException.getErrorCode() ) {
					case 1205: {
						return new PessimisticLockException( message, sqlException, sql );
					}
					case 1207:
					case 1206: {
						return new LockAcquisitionException( message, sqlException, sql );
					}
				}

				final String sqlState = JdbcExceptionHelper.extractSqlState( sqlException );

				if ( "41000".equals( sqlState ) ) {
					return new LockTimeoutException( message, sqlException, sql );
				}

				if ( "40001".equals( sqlState ) ) {
					return new LockAcquisitionException( message, sqlException, sql );
				}

				return null;
			}
		};
	}

	@Override
	public String getNotExpression(String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public IdentityColumnSupport getIdentityColumnSupport() {
		return new MySQLIdentityColumnSupport();
	}

	@Override
	public boolean isJdbcLogWarningsEnabledByDefault() {
		return false;
	}

	@Override
	public boolean supportsCascadeDelete() {
		return storageEngine.supportsCascadeDelete();
	}

	@Override
	public String getTableTypeString() {
		return storageEngine.getTableTypeString( getEngineKeyword());
	}

	protected String getEngineKeyword() {
		return "type";
	}

	@Override
	public boolean hasSelfReferentialForeignKeyBug() {
		return storageEngine.hasSelfReferentialForeignKeyBug();
	}

	@Override
	public boolean dropConstraints() {
		return storageEngine.dropConstraints();
	}

	protected MySQLStorageEngine getDefaultMySQLStorageEngine() {
		return MyISAMStorageEngine.INSTANCE;
	}

	@Override
	protected String escapeLiteral(String literal) {
		return super.escapeLiteral( literal ).replace("\\", "\\\\");
	}

	@Override
	public String translateDatetimeFormat(String format) {
		return datetimeFormat( format ).result();
	}

	public static Replacer datetimeFormat(String format) {
		return new Replacer( format, "'", "" )
				.replace("%", "%%")

				//year
				.replace("yyyy", "%Y")
				.replace("yyy", "%Y")
				.replace("yy", "%y")
				.replace("y", "%Y")

				//month of year
				.replace("MMMM", "%M")
				.replace("MMM", "%b")
				.replace("MM", "%m")
				.replace("M", "%c")

				//week of year
				.replace("ww", "%v")
				.replace("w", "%v")
				//year for week
				.replace("YYYY", "%x")
				.replace("YYY", "%x")
				.replace("Y", "%x")

				//week of month
				//????

				//day of week
				.replace("EEEE", "%W")
				.replace("EEE", "%a")
				.replace("ee", "%w")
				.replace("e", "%w")

				//day of month
				.replace("dd", "%d")
				.replace("d", "%e")

				//day of year
				.replace("DDD", "%j")
				.replace("DD", "%j")
				.replace("D", "%j")

				//am pm
				.replace("aa", "%p")
				.replace("a", "%p")

				//hour
				.replace("hh", "%h")
				.replace("HH", "%H")
				.replace("h", "%l")
				.replace("H", "%k")

				//minute
				.replace("mm", "%i")
				.replace("m", "%i")

				//second
				.replace("ss", "%S")
				.replace("s", "%S")

				//fractional seconds
				.replace("SSSSSS", "%f")
				.replace("SSSSS", "%f")
				.replace("SSSS", "%f")
				.replace("SSS", "%f")
				.replace("SS", "%f")
				.replace("S", "%f");
	}
}
