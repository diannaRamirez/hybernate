/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.query.sqm.function;

import java.util.List;
import java.util.Locale;

import org.hibernate.Incubating;
import org.hibernate.query.derived.AnonymousTupleTableGroupProducer;
import org.hibernate.query.sqm.produce.function.ArgumentsValidator;
import org.hibernate.query.sqm.produce.function.FunctionArgumentTypeResolver;
import org.hibernate.query.sqm.produce.function.SetReturningFunctionTypeResolver;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.sql.ast.tree.SqlAstNode;

import org.checkerframework.checker.nullness.qual.Nullable;

/**
 * Provides a standard implementation that supports the majority of the HQL
 * functions that are translated to SQL. The Dialect and its sub-classes use
 * this class to provide details required for processing of the associated
 * function.
 *
 * @since 7.0
 */
@Incubating
public class NamedSqmSetReturningFunctionDescriptor
		extends AbstractSqmSelfRenderingSetReturningFunctionDescriptor {
	private final boolean rendersColumnNames;
	private final String functionName;
	private final String argumentListSignature;
	private final SqlAstNodeRenderingMode argumentRenderingMode;

	public NamedSqmSetReturningFunctionDescriptor(
			String functionName,
			@Nullable ArgumentsValidator argumentsValidator,
			SetReturningFunctionTypeResolver returnTypeResolver,
			boolean rendersColumnNames,
			@Nullable FunctionArgumentTypeResolver argumentTypeResolver,
			String name,
			String argumentListSignature,
			SqlAstNodeRenderingMode argumentRenderingMode) {
		super( name, argumentsValidator, returnTypeResolver, argumentTypeResolver );

		this.rendersColumnNames = rendersColumnNames;
		this.functionName = functionName;
		this.argumentListSignature = argumentListSignature;
		this.argumentRenderingMode = argumentRenderingMode;
	}

	/**
	 * Function name accessor
	 *
	 * @return The function name.
	 */
	public String getName() {
		return functionName;
	}

	@Override
	public String getArgumentListSignature() {
		return argumentListSignature == null ? super.getArgumentListSignature() : argumentListSignature;
	}

	@Override
	public void render(
			SqlAppender sqlAppender,
			List<? extends SqlAstNode> sqlAstArguments,
			AnonymousTupleTableGroupProducer returnType,
			SqlAstTranslator<?> translator) {
		final boolean useParens = !sqlAstArguments.isEmpty();

		sqlAppender.appendSql( functionName );
		if ( useParens ) {
			sqlAppender.appendSql( "(" );
		}

		boolean firstPass = true;
		for ( SqlAstNode arg : sqlAstArguments ) {
			if ( !firstPass ) {
				sqlAppender.appendSql( "," );
			}
			translator.render( arg, argumentRenderingMode );
			firstPass = false;
		}

		if ( useParens ) {
			sqlAppender.appendSql( ")" );
		}
	}

	@Override
	public String toString() {
		return String.format(
				Locale.ROOT,
				"NamedSqmSetReturningFunctionTemplate(%s)",
				functionName
		);
	}

}
