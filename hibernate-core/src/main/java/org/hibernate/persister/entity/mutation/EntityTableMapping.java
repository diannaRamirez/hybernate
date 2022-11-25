/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.persister.entity.mutation;

import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.internal.util.collections.ArrayHelper;
import org.hibernate.jdbc.Expectation;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.ModelPart;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.sql.model.MutationType;
import org.hibernate.sql.model.TableMapping;

/**
 * Descriptor for the mapping of a table relative to an entity
 *
 * @author Steve Ebersole
 */
public class EntityTableMapping implements TableMapping {
	private enum Flag {
		OPTIONAL,
		INVERSE,
		ID_TABLE,
		CASCADE_DELETE
	}

	private final String tableName;
	private final int relativePosition;
	private final KeyMapping keyMapping;

	private final BitSet flags = new BitSet();

	private final int[] attributeIndexes;

	private final MutationDetails insertDetails;
	private final MutationDetails updateDetails;
	private final MutationDetails deleteDetails;

	public EntityTableMapping(
			String tableName,
			int relativePosition,
			KeyMapping keyMapping,
			boolean isOptional,
			boolean isInverse,
			boolean isIdentifierTable,
			int[] attributeIndexes,
			Expectation insertExpectation,
			String insertCustomSql,
			boolean insertCallable,
			Expectation updateExpectation,
			String updateCustomSql,
			boolean updateCallable,
			boolean cascadeDeleteEnabled,
			Expectation deleteExpectation,
			String deleteCustomSql,
			boolean deleteCallable) {
		this.tableName = tableName;
		this.relativePosition = relativePosition;
		this.keyMapping = keyMapping;
		this.attributeIndexes = attributeIndexes;
		this.insertDetails = new MutationDetails( MutationType.INSERT, insertExpectation, insertCustomSql, insertCallable );
		this.updateDetails = new MutationDetails( MutationType.UPDATE, updateExpectation, updateCustomSql, updateCallable );
		this.deleteDetails = new MutationDetails( MutationType.DELETE, deleteExpectation, deleteCustomSql, deleteCallable );

		if ( isOptional ) {
			flags.set( Flag.OPTIONAL.ordinal() );
		}

		if ( isInverse ) {
			flags.set( Flag.INVERSE.ordinal() );
		}

		if ( isIdentifierTable ) {
			flags.set( Flag.ID_TABLE.ordinal() );
		}

		if ( cascadeDeleteEnabled ) {
			flags.set( Flag.CASCADE_DELETE.ordinal() );
		}
	}

	@Override public String getTableName() {
		return tableName;
	}

	@Override public int getRelativePosition() {
		return relativePosition;
	}

	@Override public boolean isOptional() {
		return flags.get( Flag.OPTIONAL.ordinal() );
	}

	@Override public boolean isInverse() {
		return flags.get( Flag.INVERSE.ordinal() );
	}

	@Override public boolean isIdentifierTable() {
		return flags.get( Flag.ID_TABLE.ordinal() );
	}

	public KeyMapping getKeyMapping() {
		return keyMapping;
	}

	public boolean hasColumns() {
		return attributeIndexes.length > 0;
	}

	public boolean containsAttributeColumns(int attributeIndex) {
		return ArrayHelper.contains( attributeIndexes, attributeIndex );
	}

	public int[] getAttributeIndexes() {
		return attributeIndexes;
	}

	@Override public MutationDetails getInsertDetails() {
		return insertDetails;
	}

	public Expectation getInsertExpectation() {
		return getInsertDetails().getExpectation();
	}

	public String getInsertCustomSql() {
		return getInsertDetails().getCustomSql();
	}

	public boolean isInsertCallable() {
		return getInsertDetails().isCallable();
	}

	@Override public MutationDetails getUpdateDetails() {
		return updateDetails;
	}

	public Expectation getUpdateExpectation() {
		return getUpdateDetails().getExpectation();
	}

	public String getUpdateCustomSql() {
		return getUpdateDetails().getCustomSql();
	}

	public boolean isUpdateCallable() {
		return getUpdateDetails().isCallable();
	}

	@Override public boolean isCascadeDeleteEnabled() {
		return flags.get( Flag.CASCADE_DELETE.ordinal() );
	}

	@Override public MutationDetails getDeleteDetails() {
		return deleteDetails;
	}

	public Expectation getDeleteExpectation() {
		return getDeleteDetails().getExpectation();
	}

	public String getDeleteCustomSql() {
		return getDeleteDetails().getCustomSql();
	}

	public boolean isDeleteCallable() {
		return getDeleteDetails().isCallable();
	}

	@Override
	public boolean equals(Object o) {
		if ( this == o ) {
			return true;
		}
		if ( o == null || getClass() != o.getClass() ) {
			return false;
		}
		final EntityTableMapping that = (EntityTableMapping) o;
		return tableName.equals( that.tableName );
	}

	@Override
	public int hashCode() {
		return Objects.hash( tableName );
	}

	@Override
	public String toString() {
		return "TableMapping(" + tableName + ")";
	}

	@FunctionalInterface
	public interface KeyValueConsumer {
		void consume(Object jdbcValue, KeyColumn columnMapping);
	}

	public static class KeyMapping {
		private final List<KeyColumn> keyColumns;

		private final ModelPart identifierPart;

		public KeyMapping(List<KeyColumn> keyColumns, ModelPart identifierPart) {
			assert keyColumns.size() == identifierPart.getJdbcTypeCount();

			this.keyColumns = keyColumns;
			this.identifierPart = identifierPart;
		}

		public void breakDownKeyJdbcValues(
				Object domainValue,
				KeyValueConsumer valueConsumer,
				SharedSessionContractImplementor session) {
			identifierPart.forEachJdbcValue(
					domainValue,
					null,
					(selectionIndex, jdbcValue, jdbcMapping) -> valueConsumer.consume(
							jdbcValue,
							keyColumns.get( selectionIndex )
					),
					session
			);
		}

		public void forEachKeyColumn(Consumer<KeyColumn> keyColumnConsumer) {
			keyColumns.forEach( keyColumnConsumer );
		}
	}

	public static class KeyColumn implements SelectableMapping {
		private final String tableName;
		private final String columnName;
		private final String writeExpression;

		private final boolean formula;

		private final JdbcMapping jdbcMapping;

		public KeyColumn(
				String tableName,
				String columnName,
				String writeExpression,
				boolean formula,
				JdbcMapping jdbcMapping) {
			this.tableName = tableName;
			this.columnName = columnName;
			this.writeExpression = writeExpression;
			this.formula = formula;
			this.jdbcMapping = jdbcMapping;
		}

		public String getColumnName() {
			return columnName;
		}

		@Override
		public String getContainingTableExpression() {
			return tableName;
		}

		@Override
		public String getWriteExpression() {
			return writeExpression;
		}

		@Override
		public String getSelectionExpression() {
			return columnName;
		}

		@Override
		public JdbcMapping getJdbcMapping() {
			return jdbcMapping;
		}

		@Override
		public boolean isFormula() {
			return formula;
		}

		@Override
		public boolean isNullable() {
			// keys are never nullable
			return false;
		}

		@Override
		public boolean isInsertable() {
			// keys are always insertable, unless this "column" is a formula
			return !formula;
		}

		@Override
		public boolean isUpdateable() {
			// keys are never updateable
			return false;
		}

		@Override
		public String getColumnDefinition() {
			return null;
		}

		@Override
		public Long getLength() {
			return null;
		}

		@Override
		public Integer getPrecision() {
			return null;
		}

		@Override
		public Integer getScale() {
			return null;
		}

		@Override
		public String getCustomReadExpression() {
			return null;
		}

		@Override
		public String getCustomWriteExpression() {
			return null;
		}
	}
}
