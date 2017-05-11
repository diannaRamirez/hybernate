/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.sql.ast.tree.spi.from;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.persister.common.spi.Column;
import org.hibernate.persister.common.spi.Navigable;
import org.hibernate.persister.entity.spi.EntityPersister;
import org.hibernate.sql.ast.produce.result.internal.QueryResultEntityImpl;
import org.hibernate.sql.ast.tree.spi.expression.Expression;
import org.hibernate.sql.ast.tree.spi.expression.domain.EntityReferenceExpression;
import org.hibernate.query.spi.NavigablePath;
import org.hibernate.sql.ast.tree.spi.select.Selectable;
import org.hibernate.sql.ast.produce.result.spi.QueryResult;
import org.hibernate.sql.ast.produce.result.spi.QueryResultCreationContext;
import org.hibernate.sql.ast.consume.spi.SqlSelectAstToJdbcSelectConverter;

/**
 * A TableGroup for an entity reference
 *
 * @author Steve Ebersole
 */
public class EntityTableGroup extends AbstractTableGroup implements Selectable {
	private final EntityPersister persister;

	private EntityReferenceExpression selectableExpression;
	private List<ColumnReference> identifierColumnBindings;

	public EntityTableGroup(
			TableSpace tableSpace,
			String uid,
			String aliasBase,
			EntityPersister persister,
			NavigablePath propertyPath) {
		super( tableSpace, uid, aliasBase, propertyPath );

		this.persister = persister;
	}

	public List<ColumnReference> resolveIdentifierColumnBindings() {
		if ( identifierColumnBindings == null ) {
			identifierColumnBindings = buildIdentifierColumnBindings();
		}
		return identifierColumnBindings;
	}

	private List<ColumnReference> buildIdentifierColumnBindings() {
		final List<ColumnReference> bindings = new ArrayList<>();

		for ( Column column : persister.getHierarchy().getIdentifierDescriptor().getColumns() ) {
			bindings.add( resolveColumnReference( column ) );
		}
		return bindings;
	}

	public EntityPersister getPersister() {
		return persister;
	}

	@Override
	public Selectable getSelectable() {
		return this;
	}

	@Override
	public void accept(SqlSelectAstToJdbcSelectConverter walker) {
		// todo : need a way to resolve ColumnBinding[] to SqlSelectable[]
		// walking a TableGroup as an Expression is likely wrong
		//throw new IllegalStateException( "Cannot treat TableGroup as Expression" );

		walker.visitEntityExpression( selectableExpression );
	}

	@Override
	public Navigable getNavigable() {
		return persister;
	}

	@Override
	public NavigablePath getNavigablePath() {
		return super.getNavigablePath();
	}

	@Override
	public QueryResult makeQueryResult(Expression selectedExpression, String resultVariable, QueryResultCreationContext returnResolutionContext) {
		return new QueryResultEntityImpl(
				selectedExpression,
				persister,
				resultVariable,
				// todo (6.0) : build this Map<?,SqlSelectionGroup>
				null,
				getNavigablePath(),
				getUid()
		);
	}

	@Override
	public List<ColumnReference> getColumnReferences() {
		return getSelectedExpression().getColumnReferences();
	}

	@Override
	public QueryResult toQueryReturn(QueryResultCreationContext returnResolutionContext, String resultVariable) {
		return getSelectedExpression().getSelectable().toQueryReturn( returnResolutionContext, resultVariable );
	}
}
