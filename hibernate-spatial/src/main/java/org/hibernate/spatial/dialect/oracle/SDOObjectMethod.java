/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.spatial.dialect.oracle;

import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.query.sqm.produce.spi.SqmFunctionTemplate;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.type.Type;

/**
 * Special SQLFunction implementation for Oracle object methods
 *
 * @author Karel Maesen
 */
class SDOObjectMethod implements SqmFunctionTemplate {

	private final Type type;

	private final String name;

	public SDOObjectMethod(String name, Type type) {
		this.type = type;
		this.name = name;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.query.sqm.produce.spi.SQLFunction#getReturnType(org.hibernate.type.Type,
		  *      org.hibernate.engine.Mapping)
		  */

	public Type getReturnType(Type columnType)
			throws QueryException {
		return type == null ? columnType : type;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.query.sqm.produce.spi.SQLFunction#hasArguments()
		  */

	public boolean hasArguments() {
		return true;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.query.sqm.produce.spi.SQLFunction#hasParenthesesIfNoArguments()
		  */

	public boolean hasParenthesesIfNoArguments() {
		return true;
	}

	public String getName() {
		return this.name;
	}

	/*
		  * (non-Javadoc)
		  *
		  * @see org.hibernate.query.sqm.produce.spi.SQLFunction#render(java.util.List,
		  *      org.hibernate.engine.SessionFactoryImplementor)
		  */

	public String render(Type firstArgumentType, List args, SessionFactoryImplementor factory) throws QueryException {
		final StringBuffer buf = new StringBuffer();
		if ( args.isEmpty() ) {
			throw new QueryException(
					"First Argument in arglist must be object to which method is applied"
			);
		}
		buf.append( args.get( 0 ) ).append( "." ).append( name ).append( '(' );
		for ( int i = 1; i < args.size(); i++ ) {
			buf.append( args.get( i ) );
			if ( i < args.size() - 1 ) {
				buf.append( ", " );
			}
		}
		return buf.append( ')' ).toString();
	}

}
