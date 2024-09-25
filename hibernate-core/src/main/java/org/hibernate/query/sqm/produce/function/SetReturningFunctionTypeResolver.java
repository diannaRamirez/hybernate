/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.produce.function;

import java.util.List;

import org.hibernate.Incubating;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SqlExpressible;
import org.hibernate.query.derived.AnonymousTupleType;
import org.hibernate.query.sqm.tree.SqmTypedNode;
import org.hibernate.sql.ast.tree.SqlAstNode;

/**
 * Pluggable strategy for resolving a function return type for a specific call.
 *
 * @since 7.0
 */
@Incubating
public interface SetReturningFunctionTypeResolver {

	/**
	 * Resolve the return type for a function given its arguments to this call.
	 *
	 * @return The resolved type.
	 */
	AnonymousTupleType<?> resolveTupleType(List<? extends SqmTypedNode<?>> arguments);

	/**
	 * Resolve the tuple elements {@link SqlExpressible} for a function given its arguments to this call.
	 *
	 * @return The resolved JdbcMapping.
	 */
	SelectableMapping[] resolveFunctionReturnType(List<? extends SqlAstNode> arguments);
}
