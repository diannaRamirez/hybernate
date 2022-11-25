/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.sql.model.ast.builder;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.sql.model.MutationTarget;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;
import org.hibernate.sql.model.ast.ColumnValueBinding;
import org.hibernate.sql.model.ast.MutatingTableReference;
import org.hibernate.sql.model.ast.TableInsert;

/**
 * Base support for TableInsertBuilder implementations
 *
 * @author Steve Ebersole
 */
public abstract class AbstractTableInsertBuilder
		extends AbstractTableMutationBuilder<TableInsert>
		implements TableInsertBuilder {
	private final List<ColumnValueBinding> keyBindingList = new ArrayList<>();
	private final List<ColumnValueBinding> valueBindingList = new ArrayList<>();
	private List<ColumnValueBinding> lobValueBindingList;

	public AbstractTableInsertBuilder(
			MutationTarget mutationTarget,
			TableMapping table,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.INSERT, mutationTarget, table, sessionFactory );
	}

	public AbstractTableInsertBuilder(
			MutationTarget mutationTarget,
			MutatingTableReference tableReference,
			SessionFactoryImplementor sessionFactory) {
		super( MutationType.INSERT, mutationTarget, tableReference, sessionFactory );
	}

	protected List<ColumnValueBinding> getKeyBindingList() {
		return keyBindingList;
	}

	protected List<ColumnValueBinding> getValueBindingList() {
		return valueBindingList;
	}

	protected List<ColumnValueBinding> getLobValueBindingList() {
		return lobValueBindingList;
	}

	@Override
	public void addValueColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping) {
		final ColumnValueBinding valueBinding = createValueBinding( columnName, columnWriteFragment, jdbcMapping );

		if ( jdbcMapping.getJdbcType().isLob() && getJdbcServices().getDialect().forceLobAsLastValue() ) {
			if ( lobValueBindingList == null ) {
				lobValueBindingList = new ArrayList<>();
				lobValueBindingList.add( valueBinding );
			}
		}
		else {
			valueBindingList.add( valueBinding );
		}
	}

	@Override
	public void addKeyColumn(
			String columnName,
			String columnWriteFragment,
			JdbcMapping jdbcMapping) {
		addColumn( columnName, columnWriteFragment, jdbcMapping, keyBindingList );
	}
}
