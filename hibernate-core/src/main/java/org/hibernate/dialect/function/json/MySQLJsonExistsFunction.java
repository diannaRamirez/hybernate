/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.dialect.function.json;

import org.hibernate.query.ReturnableType;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.expression.JsonPathPassingClause;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * MySQL json_exists function.
 */
public class MySQLJsonExistsFunction extends JsonExistsFunction {

	public MySQLJsonExistsFunction(TypeConfiguration typeConfiguration) {
		super( typeConfiguration, true, false );
	}

	@Override
	protected void render(
			SqlAppender sqlAppender,
			JsonExistsArguments arguments,
			ReturnableType<?> returnType,
			SqlAstTranslator<?> walker) {
		final JsonPathPassingClause passingClause = arguments.passingClause();
		sqlAppender.appendSql( "json_contains_path(" );
		arguments.jsonDocument().accept( walker );
		sqlAppender.appendSql( ",'one'," );
		if ( passingClause == null ) {
			arguments.jsonPath().accept( walker );
		}
		else {
			JsonPathHelper.appendJsonPathConcatPassingClause(
					sqlAppender,
					arguments.jsonPath(),
					passingClause, walker
			);
		}
		sqlAppender.appendSql( ')' );
	}
}
