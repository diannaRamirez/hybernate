/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression.function;

import org.hibernate.metamodel.model.domain.spi.AllowableFunctionReturnType;

/**
 * @author Steve Ebersole
 */
public abstract class AbstractFunctionSqmExpression implements FunctionSqmExpression {
	private final AllowableFunctionReturnType resultType;

	public AbstractFunctionSqmExpression(AllowableFunctionReturnType resultType) {
		this.resultType = resultType;
	}

	@Override
	public AllowableFunctionReturnType getExpressionType() {
		return resultType;
	}

	@Override
	public AllowableFunctionReturnType getInferableType() {
		return getExpressionType();
	}
}
