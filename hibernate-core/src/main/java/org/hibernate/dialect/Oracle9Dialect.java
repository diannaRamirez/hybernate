/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.cfg.Environment;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.NvlFunction;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
import org.hibernate.dialect.function.VarArgsSQLFunction;
import org.hibernate.exception.spi.TemplatedViolatedConstraintNameExtracter;
import org.hibernate.exception.spi.ViolatedConstraintNameExtracter;
import org.hibernate.hql.spi.id.IdTableSupportStandardImpl;
import org.hibernate.hql.spi.id.MultiTableBulkIdStrategy;
import org.hibernate.hql.spi.id.global.GlobalTemporaryTableBulkIdStrategy;
import org.hibernate.hql.spi.id.local.AfterUseAction;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.internal.util.JdbcExceptionHelper;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.jboss.logging.Logger;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Locale;

/**
 * An SQL dialect for Oracle 9 (uses ANSI-style syntax where possible).
 *
 * @author Gavin King
 * @author David Channon
 *
 * @deprecated Use either Oracle9iDialect or Oracle10gDialect instead
 */
@SuppressWarnings("deprecation")
@Deprecated
public class Oracle9Dialect extends Dialect {

	private static final int PARAM_LIST_SIZE_LIMIT = 1000;

	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			Oracle9Dialect.class.getName()
	);

	/**
	 * Constructs a Oracle9Dialect
	 */
	public Oracle9Dialect() {
		super();
		LOG.deprecatedOracle9Dialect();
		registerColumnType( Types.BIT, "number(1,0)" );
		registerColumnType( Types.BIGINT, "number(19,0)" );
		registerColumnType( Types.SMALLINT, "number(5,0)" );
		registerColumnType( Types.TINYINT, "number(3,0)" );
		registerColumnType( Types.INTEGER, "number(10,0)" );
		registerColumnType( Types.CHAR, "char(1 char)" );
		registerColumnType( Types.VARCHAR, 4000, "varchar2($l char)" );
		registerColumnType( Types.VARCHAR, "long" );
		registerColumnType( Types.FLOAT, "float" );
		registerColumnType( Types.DOUBLE, "double precision" );
		registerColumnType( Types.DATE, "date" );
		registerColumnType( Types.TIME, "date" );
		registerColumnType( Types.TIMESTAMP, "timestamp" );
		registerColumnType( Types.VARBINARY, 2000, "raw($l)" );
		registerColumnType( Types.VARBINARY, "long raw" );
		registerColumnType( Types.NUMERIC, "number($p,$s)" );
		registerColumnType( Types.DECIMAL, "number($p,$s)" );
		registerColumnType( Types.BLOB, "blob" );
		registerColumnType( Types.CLOB, "clob" );

		// Oracle driver reports to support getGeneratedKeys(), but they only
		// support the version taking an array of the names of the columns to
		// be returned (via its RETURNING clause).  No other driver seems to
		// support this overloaded version.
		getDefaultProperties().setProperty( Environment.USE_GET_GENERATED_KEYS, "false" );
		getDefaultProperties().setProperty( Environment.USE_STREAMS_FOR_BINARY, "true" );
		getDefaultProperties().setProperty( Environment.STATEMENT_BATCH_SIZE, DEFAULT_BATCH_SIZE );
		getDefaultProperties().setProperty( Environment.BATCH_VERSIONED_DATA, "false" );

		registerFunction( "abs", new StandardSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new StandardSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "acos", new StandardSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSqmFunctionTemplate( "cosh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSqmFunctionTemplate( "sinh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "stddev", new StandardSqmFunctionTemplate( "stddev", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSqmFunctionTemplate( "tanh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "variance", new StandardSqmFunctionTemplate( "variance", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "round", new StandardSqmFunctionTemplate( "round" ) );
		registerFunction( "trunc", new StandardSqmFunctionTemplate( "trunc" ) );
		registerFunction( "ceil", new StandardSqmFunctionTemplate( "ceil" ) );
		registerFunction( "floor", new StandardSqmFunctionTemplate( "floor" ) );

		registerFunction( "chr", new StandardSqmFunctionTemplate( "chr", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "initcap", new StandardSqmFunctionTemplate( "initcap" ) );
		registerFunction( "lower", new StandardSqmFunctionTemplate( "lower" ) );
		registerFunction( "ltrim", new StandardSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "rtrim", new StandardSqmFunctionTemplate( "rtrim" ) );
		registerFunction( "soundex", new StandardSqmFunctionTemplate( "soundex" ) );
		registerFunction( "upper", new StandardSqmFunctionTemplate( "upper" ) );
		registerFunction( "ascii", new StandardSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "to_char", new StandardSqmFunctionTemplate( "to_char", StandardSpiBasicTypes.STRING ) );
		registerFunction( "to_date", new StandardSqmFunctionTemplate( "to_date", StandardSpiBasicTypes.TIMESTAMP ) );

		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "current_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "current_timestamp", StandardSpiBasicTypes.TIME, false ) );
		registerFunction(
				"current_timestamp", new NoArgsSqmFunctionTemplate(
						"current_timestamp",
						StandardSpiBasicTypes.TIMESTAMP,
						false
		)
		);

		registerFunction( "last_day", new StandardSqmFunctionTemplate( "last_day", StandardSpiBasicTypes.DATE ) );
		registerFunction( "sysdate", new NoArgsSqmFunctionTemplate( "sysdate", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "systimestamp", new NoArgsSqmFunctionTemplate( "systimestamp", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "uid", new NoArgsSqmFunctionTemplate( "uid", StandardSpiBasicTypes.INTEGER, false ) );
		registerFunction( "user", new NoArgsSqmFunctionTemplate( "user", StandardSpiBasicTypes.STRING, false ) );

		registerFunction( "rowid", new NoArgsSqmFunctionTemplate( "rowid", StandardSpiBasicTypes.LONG, false ) );
		registerFunction( "rownum", new NoArgsSqmFunctionTemplate( "rownum", StandardSpiBasicTypes.LONG, false ) );

		// Multi-param string dialect functions...
		registerFunction( "concat", new VarArgsSQLFunction( StandardSpiBasicTypes.STRING, "", "||", "" ) );
		registerFunction( "instr", new StandardSqmFunctionTemplate( "instr", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "instrb", new StandardSqmFunctionTemplate( "instrb", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "lpad", new StandardSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substrb", new StandardSqmFunctionTemplate( "substrb", StandardSpiBasicTypes.STRING ) );
		registerFunction( "translate", new StandardSqmFunctionTemplate( "translate", StandardSpiBasicTypes.STRING ) );

		registerFunction( "substring", new StandardSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "locate", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "instr(?2,?1)" ) );
		registerFunction( "bit_length", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "vsize(?1)*8" ) );
		registerFunction( "coalesce", new NvlFunction() );

		// Multi-param numeric dialect functions...
		registerFunction( "atan2", new StandardSqmFunctionTemplate( "atan2", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "log", new StandardSqmFunctionTemplate( "log", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "mod", new StandardSqmFunctionTemplate( "mod", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "nvl", new StandardSqmFunctionTemplate( "nvl" ) );
		registerFunction( "nvl2", new StandardSqmFunctionTemplate( "nvl2" ) );
		registerFunction( "power", new StandardSqmFunctionTemplate( "power", StandardSpiBasicTypes.FLOAT ) );

		// Multi-param date dialect functions...
		registerFunction( "add_months", new StandardSqmFunctionTemplate( "add_months", StandardSpiBasicTypes.DATE ) );
		registerFunction( "months_between", new StandardSqmFunctionTemplate( "months_between", StandardSpiBasicTypes.FLOAT ) );
		registerFunction( "next_day", new StandardSqmFunctionTemplate( "next_day", StandardSpiBasicTypes.DATE ) );

		registerFunction( "str", new StandardSqmFunctionTemplate( "to_char", StandardSpiBasicTypes.STRING ) );
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName ) + " from dual";
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return sequenceName + ".nextval";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		//starts with 1, implicitly
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getCascadeConstraintsString() {
		return " cascade constraints";
	}

	@Override
	public boolean dropConstraints() {
		return false;
	}

	@Override
	public String getForUpdateNowaitString() {
		return " for update nowait";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public boolean supportsPooledSequences() {
		return true;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public String getLimitString(String sql, boolean hasOffset) {

		sql = sql.trim();
		boolean isForUpdate = false;
		if ( sql.toLowerCase(Locale.ROOT).endsWith( " for update" ) ) {
			sql = sql.substring( 0, sql.length() - 11 );
			isForUpdate = true;
		}

		final StringBuilder pagingSelect = new StringBuilder( sql.length() + 100 );
		if ( hasOffset ) {
			pagingSelect.append( "select * from ( select row_.*, rownum rownum_ from ( " );
		}
		else {
			pagingSelect.append( "select * from ( " );
		}
		pagingSelect.append( sql );
		if ( hasOffset ) {
			pagingSelect.append( " ) row_ where rownum <= ?) where rownum_ > ?" );
		}
		else {
			pagingSelect.append( " ) where rownum <= ?" );
		}

		if ( isForUpdate ) {
			pagingSelect.append( " for update" );
		}

		return pagingSelect.toString();
	}

	@Override
	public String getForUpdateString(String aliases) {
		return getForUpdateString() + " of " + aliases;
	}

	@Override
	public String getForUpdateNowaitString(String aliases) {
		return getForUpdateString() + " of " + aliases + " nowait";
	}

	@Override
	public boolean bindLimitParametersInReverseOrder() {
		return true;
	}

	@Override
	public boolean useMaxForLimit() {
		return true;
	}

	@Override
	public boolean forUpdateOfColumns() {
		return true;
	}

	@Override
	public String getQuerySequencesString() {
		return "select sequence_name from user_sequences";
	}

	@Override
	public String getSelectGUIDString() {
		return "select rawtohex(sys_guid()) from dual";
	}

	@Override
	public ViolatedConstraintNameExtracter getViolatedConstraintNameExtracter() {
		return EXTRACTER;
	}

	private static final ViolatedConstraintNameExtracter EXTRACTER = new TemplatedViolatedConstraintNameExtracter() {
		@Override
		protected String doExtractConstraintName(SQLException sqle) throws NumberFormatException {
			final int errorCode = JdbcExceptionHelper.extractErrorCode( sqle );
			if ( errorCode == 1 || errorCode == 2291 || errorCode == 2292 ) {
				return extractUsingTemplate( "constraint (", ") violated", sqle.getMessage() );
			}
			else if ( errorCode == 1400 ) {
				// simple nullability constraint
				return null;
			}
			else {
				return null;
			}
		}
	};

	@Override
	public int registerResultSetOutParameter(java.sql.CallableStatement statement, int col) throws SQLException {
		//	register the type of the out param - an Oracle specific type
		statement.registerOutParameter( col, OracleTypesHelper.INSTANCE.getOracleCursorTypeSqlType() );
		col++;
		return col;
	}

	@Override
	public ResultSet getResultSet(CallableStatement ps) throws SQLException {
		ps.execute();
		return (ResultSet) ps.getObject( 1 );
	}

	@Override
	public boolean supportsUnionAll() {
		return true;
	}

	@Override
	public boolean supportsCommentOn() {
		return true;
	}

	@Override
	public MultiTableBulkIdStrategy getDefaultMultiTableBulkIdStrategy() {
		return new GlobalTemporaryTableBulkIdStrategy(
				new IdTableSupportStandardImpl() {
					@Override
					public String generateIdTableName(String baseName) {
						final String name = super.generateIdTableName( baseName );
						return name.length() > 30 ? name.substring( 0, 30 ) : name;
					}

					@Override
					public String getCreateIdTableCommand() {
						return "create global temporary table";
					}

					@Override
					public String getCreateIdTableStatementOptions() {
						return "on commit delete rows";
					}
				},
				AfterUseAction.CLEAN
		);
	}

	@Override
	public boolean supportsCurrentTimestampSelection() {
		return true;
	}

	@Override
	public String getCurrentTimestampSelectString() {
		return "select systimestamp from dual";
	}

	@Override
	public boolean isCurrentTimestampSelectStringCallable() {
		return false;
	}

	@Override
	public boolean supportsEmptyInList() {
		return false;
	}

	@Override
	public boolean supportsExistsInSelect() {
		return false;
	}

	@Override
	public int getInExpressionCountLimit() {
		return PARAM_LIST_SIZE_LIMIT;
	}

	@Override
	public String getNotExpression(String expression) {
		return "not (" + expression + ")";
	}

	@Override
	public boolean canCreateSchema() {
		return false;
	}
}
