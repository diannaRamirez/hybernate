/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.from;

import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.ModelPartContainer;
import org.hibernate.query.spi.NavigablePath;

/**
 * Acts as a TableGroup for DML query operations.  It is used to simply
 * wrap the TableReference of the "mutating table"
 *
 * @author Steve Ebersole
 */
public class MutatingTableReferenceGroupWrapper implements VirtualTableGroup {
	private final NavigablePath navigablePath;
	private final ModelPartContainer modelPart;
	private final NamedTableReference mutatingTableReference;

	public MutatingTableReferenceGroupWrapper(
			NavigablePath navigablePath,
			ModelPartContainer modelPart,
			NamedTableReference mutatingTableReference) {
		this.navigablePath = navigablePath;
		this.modelPart = modelPart;
		this.mutatingTableReference = mutatingTableReference;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return navigablePath;
	}

	@Override
	public ModelPart getExpressionType() {
		return getModelPart();
	}

	@Override
	public String getGroupAlias() {
		return null;
	}

	@Override
	public ModelPartContainer getModelPart() {
		return modelPart;
	}

	@Override
	public TableReference getPrimaryTableReference() {
		return mutatingTableReference;
	}

	@Override
	public TableReference getTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization,
			boolean resolve) {
		return mutatingTableReference.getTableExpression().equals( tableExpression )
				? mutatingTableReference
				: null;
	}

	@Override
	public TableReference resolveTableReference(
			NavigablePath navigablePath,
			String tableExpression,
			boolean allowFkOptimization) {
		return getTableReference( navigablePath, tableExpression, allowFkOptimization, true );
	}

	@Override
	public void applyAffectedTableNames(Consumer<String> nameCollector) {
		nameCollector.accept( mutatingTableReference.getTableExpression() );
	}

	@Override
	public String getSourceAlias() {
		return null;
	}

	@Override
	public List<TableGroupJoin> getTableGroupJoins() {
		return Collections.emptyList();
	}

	@Override
	public List<TableGroupJoin> getNestedTableGroupJoins() {
		return Collections.emptyList();
	}

	@Override
	public boolean canUseInnerJoins() {
		return false;
	}

	@Override
	public void addTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void prependTableGroupJoin(NavigablePath navigablePath, TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addNestedTableGroupJoin(TableGroupJoin join) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void visitTableGroupJoins(Consumer<TableGroupJoin> consumer) {
	}

	@Override
	public void visitNestedTableGroupJoins(Consumer<TableGroupJoin> consumer) {
	}

	@Override
	public List<TableReferenceJoin> getTableReferenceJoins() {
		return Collections.emptyList();
	}

}
