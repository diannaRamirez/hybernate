/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.expression;

import org.hibernate.sql.ast.produce.spi.AllowableFunctionReturnType;

/**
 * Marker interface to more readily identify "aggregate functions".
 *
 * @author Steve Ebersole
 */
public interface AggregateFunction extends Function {
	@Override
	AllowableFunctionReturnType getType();

	Expression getArgument();
	boolean isDistinct();
}
