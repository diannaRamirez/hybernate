/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

import org.hibernate.dialect.function.CommonFunctionFactory;
import org.hibernate.dialect.pagination.LimitHandler;
import org.hibernate.dialect.pagination.SQLServer2012LimitHandler;
import org.hibernate.query.spi.QueryEngine;
import org.hibernate.type.spi.StandardSpiBasicTypes;

/**
 * Microsoft SQL Server 2012 Dialect
 *
 * @author Brett Meyer
 */
public class SQLServer2012Dialect extends SQLServer2008Dialect {

	@Override
	public void initializeFunctionRegistry(QueryEngine queryEngine) {
		super.initializeFunctionRegistry(queryEngine);

		//actually translate() was added in 2017 but
		//it's not worth adding a new dialect for that!
		CommonFunctionFactory.translate( queryEngine );

		CommonFunctionFactory.median_percentileCont( queryEngine, true );

		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datefromparts" )
				.setInvariantType( StandardSpiBasicTypes.DATE )
				.setExactArgumentCount( 3 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "timefromparts" )
				.setInvariantType( StandardSpiBasicTypes.TIME )
				.setExactArgumentCount( 5 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "smalldatetimefromparts" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 5 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datetimefromparts" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 7 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datetime2fromparts" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 8 )
				.register();
		queryEngine.getSqmFunctionRegistry().namedTemplateBuilder( "datetimeoffsetfromparts" )
				.setInvariantType( StandardSpiBasicTypes.TIMESTAMP )
				.setExactArgumentCount( 10 )
				.register();

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
	public String getCreateSequenceString(String sequenceName) {
		return "create sequence " + sequenceName;
	}

	@Override
	public String getDropSequenceString(String sequenceName) {
		return "drop sequence " + sequenceName;
	}

	@Override
	public String getSelectSequenceNextValString(String sequenceName) {
		return "next value for " + sequenceName;
	}

	@Override
	public String getSequenceNextValString(String sequenceName) {
		return "select " + getSelectSequenceNextValString( sequenceName );
	}

	@Override
	public String getQuerySequencesString() {
		// The upper-case name should work on both case-sensitive and case-insensitive collations.
		return "select * from INFORMATION_SCHEMA.SEQUENCES";
	}

	@Override
	public String getQueryHintString(String sql, String hints) {
		final StringBuilder buffer = new StringBuilder(
				sql.length()
						+ hints.length() + 12
		);
		final int pos = sql.indexOf( ";" );
		if ( pos > -1 ) {
			buffer.append( sql.substring( 0, pos ) );
		}
		else {
			buffer.append( sql );
		}
		buffer.append( " OPTION (" ).append( hints ).append( ")" );
		if ( pos > -1 ) {
			buffer.append( ";" );
		}
		sql = buffer.toString();

		return sql;
	}

	@Override
	public boolean supportsLimitOffset() {
		return true;
	}

	@Override
	protected LimitHandler getDefaultLimitHandler() {
		return new SQLServer2012LimitHandler();
	}
}
