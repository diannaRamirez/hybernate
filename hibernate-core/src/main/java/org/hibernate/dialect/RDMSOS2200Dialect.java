/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import java.sql.Types;

import org.hibernate.LockMode;
import org.hibernate.dialect.function.NoArgsSqmFunctionTemplate;
import org.hibernate.dialect.function.SQLFunctionTemplate;
import org.hibernate.query.sqm.produce.function.spi.StandardSqmFunctionTemplate;
import org.hibernate.dialect.lock.LockingStrategy;
import org.hibernate.dialect.lock.OptimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.OptimisticLockingStrategy;
import org.hibernate.dialect.lock.PessimisticForceIncrementLockingStrategy;
import org.hibernate.dialect.lock.PessimisticReadUpdateLockingStrategy;
import org.hibernate.dialect.lock.PessimisticWriteUpdateLockingStrategy;
import org.hibernate.dialect.lock.SelectLockingStrategy;
import org.hibernate.dialect.lock.UpdateLockingStrategy;
import org.hibernate.dialect.pagination.AbstractLimitHandler;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.LimitHelper;
import org.hibernate.engine.spi.RowSelection;
import org.hibernate.internal.CoreMessageLogger;
import org.hibernate.persister.entity.Lockable;
import org.hibernate.sql.CaseFragment;
import org.hibernate.sql.DecodeCaseFragment;
import org.hibernate.type.spi.StandardSpiBasicTypes;

import org.jboss.logging.Logger;

/**
 * This is the Hibernate dialect for the Unisys 2200 Relational Database (RDMS).
 * This dialect was developed for use with Hibernate 3.0.5. Other versions may
 * require modifications to the dialect.
 * <p/>
 * Version History:
 * Also change the version displayed below in the constructor
 * 1.1
 * 1.0  2005-10-24  CDH - First dated version for use with CP 11
 *
 * @author Ploski and Hanson
 */
@SuppressWarnings("deprecation")
public class RDMSOS2200Dialect extends Dialect {
	private static final CoreMessageLogger LOG = Logger.getMessageLogger(
			CoreMessageLogger.class,
			RDMSOS2200Dialect.class.getName()
	);

	private static final AbstractLimitHandler LIMIT_HANDLER = new AbstractLimitHandler() {
		@Override
		public String processSql(String sql, RowSelection selection) {
			final boolean hasOffset = LimitHelper.hasFirstRow( selection );
			if (hasOffset) {
				throw new UnsupportedOperationException( "query result offset is not supported" );
			}
			return sql + " fetch first " + getMaxOrLimit( selection ) + " rows only ";
		}

		@Override
		public boolean supportsLimit() {
			return true;
		}

		@Override
		public boolean supportsLimitOffset() {
			return false;
		}

		@Override
		public boolean supportsVariableLimit() {
			return false;
		}
	};

	/**
	 * Constructs a RDMSOS2200Dialect
	 */
	public RDMSOS2200Dialect() {
		super();
		// Display the dialect version.
		LOG.rdmsOs2200Dialect();

		/**
		 * This section registers RDMS Built-in Functions (BIFs) with Hibernate.
		 * The first parameter is the 'register' function name with Hibernate.
		 * The second parameter is the defined RDMS SQL Function and it's
		 * characteristics. If StandardSQLFunction(...) is used, the RDMS BIF
		 * name and the return type (if any) is specified.  If
		 * SQLFunctionTemplate(...) is used, the return type and a template
		 * string is provided, plus an optional hasParenthesesIfNoArgs flag.
		 */
		registerFunction( "abs", new StandardSqmFunctionTemplate( "abs" ) );
		registerFunction( "sign", new StandardSqmFunctionTemplate( "sign", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "ascii", new StandardSqmFunctionTemplate( "ascii", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "char_length", new StandardSqmFunctionTemplate( "char_length", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "character_length", new StandardSqmFunctionTemplate( "character_length", StandardSpiBasicTypes.INTEGER ) );

		// The RDMS concat() function only supports 2 parameters
		registerFunction( "concat", new SQLFunctionTemplate( StandardSpiBasicTypes.STRING, "concat(?1, ?2)" ) );
		registerFunction( "instr", new StandardSqmFunctionTemplate( "instr", StandardSpiBasicTypes.STRING ) );
		registerFunction( "lpad", new StandardSqmFunctionTemplate( "lpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "replace", new StandardSqmFunctionTemplate( "replace", StandardSpiBasicTypes.STRING ) );
		registerFunction( "rpad", new StandardSqmFunctionTemplate( "rpad", StandardSpiBasicTypes.STRING ) );
		registerFunction( "substr", new StandardSqmFunctionTemplate( "substr", StandardSpiBasicTypes.STRING ) );

		registerFunction( "lcase", new StandardSqmFunctionTemplate( "lcase" ) );
		registerFunction( "lower", new StandardSqmFunctionTemplate( "lower" ) );
		registerFunction( "ltrim", new StandardSqmFunctionTemplate( "ltrim" ) );
		registerFunction( "reverse", new StandardSqmFunctionTemplate( "reverse" ) );
		registerFunction( "rtrim", new StandardSqmFunctionTemplate( "rtrim" ) );

		// RDMS does not directly support the trim() function, we use rtrim() and ltrim()
		registerFunction( "trim", new SQLFunctionTemplate( StandardSpiBasicTypes.INTEGER, "ltrim(rtrim(?1))" ) );
		registerFunction( "soundex", new StandardSqmFunctionTemplate( "soundex" ) );
		registerFunction( "space", new StandardSqmFunctionTemplate( "space", StandardSpiBasicTypes.STRING ) );
		registerFunction( "ucase", new StandardSqmFunctionTemplate( "ucase" ) );
		registerFunction( "upper", new StandardSqmFunctionTemplate( "upper" ) );

		registerFunction( "acos", new StandardSqmFunctionTemplate( "acos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "asin", new StandardSqmFunctionTemplate( "asin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "atan", new StandardSqmFunctionTemplate( "atan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cos", new StandardSqmFunctionTemplate( "cos", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cosh", new StandardSqmFunctionTemplate( "cosh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "cot", new StandardSqmFunctionTemplate( "cot", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "exp", new StandardSqmFunctionTemplate( "exp", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "ln", new StandardSqmFunctionTemplate( "ln", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log", new StandardSqmFunctionTemplate( "log", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "log10", new StandardSqmFunctionTemplate( "log10", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "pi", new NoArgsSqmFunctionTemplate( "pi", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "rand", new NoArgsSqmFunctionTemplate( "rand", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sin", new StandardSqmFunctionTemplate( "sin", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sinh", new StandardSqmFunctionTemplate( "sinh", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "sqrt", new StandardSqmFunctionTemplate( "sqrt", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tan", new StandardSqmFunctionTemplate( "tan", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "tanh", new StandardSqmFunctionTemplate( "tanh", StandardSpiBasicTypes.DOUBLE ) );

		registerFunction( "round", new StandardSqmFunctionTemplate( "round" ) );
		registerFunction( "trunc", new StandardSqmFunctionTemplate( "trunc" ) );
		registerFunction( "ceil", new StandardSqmFunctionTemplate( "ceil" ) );
		registerFunction( "floor", new StandardSqmFunctionTemplate( "floor" ) );

		registerFunction( "chr", new StandardSqmFunctionTemplate( "chr", StandardSpiBasicTypes.CHARACTER ) );
		registerFunction( "initcap", new StandardSqmFunctionTemplate( "initcap" ) );

		registerFunction( "user", new NoArgsSqmFunctionTemplate( "user", StandardSpiBasicTypes.STRING, false ) );

		registerFunction( "current_date", new NoArgsSqmFunctionTemplate( "current_date", StandardSpiBasicTypes.DATE, false ) );
		registerFunction( "current_time", new NoArgsSqmFunctionTemplate( "current_timestamp", StandardSpiBasicTypes.TIME, false ) );
		registerFunction( "current_timestamp", new NoArgsSqmFunctionTemplate( "current_timestamp", StandardSpiBasicTypes.TIMESTAMP, false ) );
		registerFunction( "curdate", new NoArgsSqmFunctionTemplate( "curdate", StandardSpiBasicTypes.DATE ) );
		registerFunction( "curtime", new NoArgsSqmFunctionTemplate( "curtime", StandardSpiBasicTypes.TIME ) );
		registerFunction( "days", new StandardSqmFunctionTemplate( "days", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofmonth", new StandardSqmFunctionTemplate( "dayofmonth", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayname", new StandardSqmFunctionTemplate( "dayname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "dayofweek", new StandardSqmFunctionTemplate( "dayofweek", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "dayofyear", new StandardSqmFunctionTemplate( "dayofyear", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "hour", new StandardSqmFunctionTemplate( "hour", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "last_day", new StandardSqmFunctionTemplate( "last_day", StandardSpiBasicTypes.DATE ) );
		registerFunction( "microsecond", new StandardSqmFunctionTemplate( "microsecond", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "minute", new StandardSqmFunctionTemplate( "minute", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "month", new StandardSqmFunctionTemplate( "month", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "monthname", new StandardSqmFunctionTemplate( "monthname", StandardSpiBasicTypes.STRING ) );
		registerFunction( "now", new NoArgsSqmFunctionTemplate( "now", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "quarter", new StandardSqmFunctionTemplate( "quarter", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "second", new StandardSqmFunctionTemplate( "second", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "time", new StandardSqmFunctionTemplate( "time", StandardSpiBasicTypes.TIME ) );
		registerFunction( "timestamp", new StandardSqmFunctionTemplate( "timestamp", StandardSpiBasicTypes.TIMESTAMP ) );
		registerFunction( "week", new StandardSqmFunctionTemplate( "week", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "year", new StandardSqmFunctionTemplate( "year", StandardSpiBasicTypes.INTEGER ) );

		registerFunction( "atan2", new StandardSqmFunctionTemplate( "atan2", StandardSpiBasicTypes.DOUBLE ) );
		registerFunction( "mod", new StandardSqmFunctionTemplate( "mod", StandardSpiBasicTypes.INTEGER ) );
		registerFunction( "nvl", new StandardSqmFunctionTemplate( "nvl" ) );
		registerFunction( "power", new StandardSqmFunctionTemplate( "power", StandardSpiBasicTypes.DOUBLE ) );

		/**
		 * For a list of column types to register, see section A-1
		 * in 7862 7395, the Unisys JDBC manual.
		 *
		 * Here are column sizes as documented in Table A-1 of
		 * 7831 0760, "Enterprise Relational Database Server
		 * for ClearPath OS2200 Administration Guide"
		 * Numeric - 21
		 * Decimal - 22 (21 digits plus one for sign)
		 * Float   - 60 bits
		 * Char    - 28000
		 * NChar   - 14000
		 * BLOB+   - 4294967296 (4 Gb)
		 * + RDMS JDBC driver does not support BLOBs
		 *
		 * DATE, TIME and TIMESTAMP literal formats are
		 * are all described in section 2.3.4 DATE Literal Format
		 * in 7830 8160.
		 * The DATE literal format is: YYYY-MM-DD
		 * The TIME literal format is: HH:MM:SS[.[FFFFFF]]
		 * The TIMESTAMP literal format is: YYYY-MM-DD HH:MM:SS[.[FFFFFF]]
		 *
		 * Note that $l (dollar-L) will use the length value if provided.
		 * Also new for Hibernate3 is the $p percision and $s (scale) parameters
		 */
		registerColumnType( Types.BIT, "SMALLINT" );
		registerColumnType( Types.TINYINT, "SMALLINT" );
		registerColumnType( Types.BIGINT, "NUMERIC(21,0)" );
		registerColumnType( Types.SMALLINT, "SMALLINT" );
		registerColumnType( Types.CHAR, "CHARACTER(1)" );
		registerColumnType( Types.DOUBLE, "DOUBLE PRECISION" );
		registerColumnType( Types.FLOAT, "FLOAT" );
		registerColumnType( Types.REAL, "REAL" );
		registerColumnType( Types.INTEGER, "INTEGER" );
		registerColumnType( Types.NUMERIC, "NUMERIC(21,$l)" );
		registerColumnType( Types.DECIMAL, "NUMERIC(21,$l)" );
		registerColumnType( Types.DATE, "DATE" );
		registerColumnType( Types.TIME, "TIME" );
		registerColumnType( Types.TIMESTAMP, "TIMESTAMP" );
		registerColumnType( Types.VARCHAR, "CHARACTER($l)" );
		registerColumnType( Types.BLOB, "BLOB($l)" );
		/*
         * The following types are not supported in RDMS/JDBC and therefore commented out.
         * However, in some cases, mapping them to CHARACTER columns works
         * for many applications, but does not work for all cases.
         */
		// registerColumnType(Types.VARBINARY, "CHARACTER($l)");
		// registerColumnType(Types.BLOB, "CHARACTER($l)" );  // For use prior to CP 11.0
		// registerColumnType(Types.CLOB, "CHARACTER($l)" );
	}


	// Dialect method overrides ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * RDMS does not support qualifing index names with the schema name.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean qualifyIndexName() {
		return false;
	}

	/**
	 * The RDMS DB supports the 'FOR UPDATE OF' clause. However, the RDMS-JDBC
	 * driver does not support this feature, so a false is return.
	 * The base dialect also returns a false, but we will leave this over-ride
	 * in to make sure it stays false.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean forUpdateOfColumns() {
		return false;
	}

	/**
	 * Since the RDMS-JDBC driver does not support for updates, this string is
	 * set to an empty string. Whenever, the driver does support this feature,
	 * the returned string should be " FOR UPDATE OF". Note that RDMS does not
	 * support the string 'FOR UPDATE' string.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public String getForUpdateString() {
		// Original Dialect.java returns " for update";
		return "";
	}

	// Verify the state of this new method in Hibernate 3.0 Dialect.java

	/**
	 * RDMS does not support Cascade Deletes.
	 * Need to review this in the future when support is provided.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsCascadeDelete() {
		return false;
	}

	/**
	 * Currently, RDMS-JDBC does not support ForUpdate.
	 * Need to review this in the future when support is provided.
	 * <p/>
	 * {@inheritDoc}
	 */
	@Override
	public boolean supportsOuterJoinForUpdate() {
		return false;
	}

	@Override
	public String getAddColumnString() {
		return "add";
	}

	@Override
	public String getNullColumnString() {
		// The keyword used to specify a nullable column.
		return " null";
	}

	@Override
	public boolean supportsSequences() {
		return true;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		// The where clause was added to eliminate this statement from Brute Force Searches.
		return "select permuted_id('NEXT',31) from rdms.rdms_dummy where key_col = 1 ";
	}

	@Override
	public String getCreateSequenceString(String sequenceName) {
		// We must return a valid RDMS/RSA command from this method to
		// prevent RDMS/RSA from issuing *ERROR 400
		return "";
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		// We must return a valid RDMS/RSA command from this method to
		// prevent RDMS/RSA from issuing *ERROR 400
		return "";
	}

	@Override
	public String getCascadeConstraintsString() {
		// Used with DROP TABLE to delete all records in the table.
		return " including contents";
	}

	@Override
	public CaseFragment createCaseFragment() {
		return new DecodeCaseFragment();
	}

	@Override
	public LimitHandler getLimitHandler() {
		return LIMIT_HANDLER;
	}

	@Override
	public boolean supportsLimit() {
		return true;
	}

	@Override
	public boolean supportsLimitOffset() {
		return false;
	}

	@Override
	public String getLimitString(String sql, int offset, int limit) {
		if ( offset > 0 ) {
			throw new UnsupportedOperationException( "query result offset is not supported" );
		}
		return sql + " fetch first " + limit + " rows only ";
	}

	@Override
	public boolean supportsVariableLimit() {
		return false;
	}

	@Override
	public boolean supportsUnionAll() {
		// RDMS supports the UNION ALL clause.
		return true;
	}

	@Override
	public LockingStrategy getLockingStrategy(Lockable lockable, LockMode lockMode) {
		// RDMS has no known variation of a "SELECT ... FOR UPDATE" syntax...
		if ( lockMode == LockMode.PESSIMISTIC_FORCE_INCREMENT ) {
			return new PessimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_WRITE ) {
			return new PessimisticWriteUpdateLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.PESSIMISTIC_READ ) {
			return new PessimisticReadUpdateLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC ) {
			return new OptimisticLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode == LockMode.OPTIMISTIC_FORCE_INCREMENT ) {
			return new OptimisticForceIncrementLockingStrategy( lockable, lockMode );
		}
		else if ( lockMode.greaterThan( LockMode.READ ) ) {
			return new UpdateLockingStrategy( lockable, lockMode );
		}
		else {
			return new SelectLockingStrategy( lockable, lockMode );
		}
	}
}
