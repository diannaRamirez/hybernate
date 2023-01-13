/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.engine.jdbc.mutation.internal;

import java.util.Locale;

import org.hibernate.engine.jdbc.mutation.JdbcValueBindings;
import org.hibernate.engine.jdbc.mutation.ParameterUsage;
import org.hibernate.engine.jdbc.mutation.TableInclusionChecker;
import org.hibernate.engine.jdbc.mutation.group.PreparedStatementDetails;
import org.hibernate.engine.jdbc.mutation.spi.BindingGroup;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.sql.model.SelfExecutingUpdateOperation;
import org.hibernate.sql.model.ValuesAnalysis;
import org.hibernate.sql.model.jdbc.JdbcValueDescriptor;

/**
 * @author Steve Ebersole
 */
public class MutationExecutorSingleSelfExecuting extends AbstractMutationExecutor implements JdbcValueBindingsImpl.JdbcValueDescriptorAccess {
	private final SelfExecutingUpdateOperation operation;
	private final JdbcValueBindingsImpl valueBindings;

	public MutationExecutorSingleSelfExecuting(SelfExecutingUpdateOperation operation) {
		this.operation = operation;

		this.valueBindings = new JdbcValueBindingsImpl(
				operation.getMutationType(),
				operation.getMutationTarget(),
				this
		);
	}

	@Override
	public boolean bindValues(BindingGroup bindingGroup, String tableName, String columnName, ParameterUsage usage, Object value) {
		assert operation.getTableDetails().getTableName().equals( tableName )
				: String.format( Locale.ROOT, "table names did not match : `%s` & `%s`", tableName, operation.getTableDetails().getTableName()  );
		return operation.bindValues( bindingGroup, columnName, usage, value );
	}

	@Override
	public JdbcValueBindings getJdbcValueBindings() {
		return valueBindings;
	}

	@Override
	public PreparedStatementDetails getPreparedStatementDetails(String tableName) {
		throw new UnsupportedOperationException();
	}

	@Override
	protected void performSelfExecutingOperations(ValuesAnalysis valuesAnalysis, TableInclusionChecker inclusionChecker, SharedSessionContractImplementor session) {
		if ( inclusionChecker.include( operation.getTableDetails() ) ) {
			operation.performMutation( valueBindings, valuesAnalysis, session );
		}
	}

	@Override
	public void release() {
		// todo (mutation) :implement
	}
}
