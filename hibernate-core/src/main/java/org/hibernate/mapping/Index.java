/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.boot.model.naming.Identifier;
import org.hibernate.boot.model.relational.Exportable;
import org.hibernate.dialect.Dialect;

import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableMap;
import static java.util.stream.Collectors.toUnmodifiableList;
import static java.util.stream.Collectors.toUnmodifiableMap;
import static org.hibernate.internal.util.StringHelper.isNotEmpty;
import static org.hibernate.internal.util.StringHelper.qualify;
import static org.hibernate.internal.util.StringHelper.unqualify;

/**
 * A mapping model object representing an {@linkplain jakarta.persistence.Index index} on a relational database table.
 * <p>
 * We regularize the semantics of unique constraints on nullable columns: two null values are not considered to be
 * "equal" for the purpose of determining uniqueness, just as specified by ANSI SQL and common sense.
 *
 * @author Gavin King
 */
public class Index implements Exportable, Serializable {
	private Identifier name;
	private Table table;
	private boolean unique;
	private final java.util.List<Selectable> selectables = new ArrayList<>();
	private final java.util.Map<Selectable, String> selectableOrderMap = new HashMap<>();

	/**
	 * @deprecated This method will be removed in the next release
	 */
	@Deprecated(forRemoval = true)
	public static String buildSqlDropIndexString(String name, String tableName) {
		return "drop index " + qualify( tableName, name );
	}

	/**
	 * @deprecated This method will be removed in the next release
	 */
	@Deprecated(forRemoval = true)
	public static String buildSqlCreateIndexString(
			Dialect dialect,
			String name,
			String tableName,
			java.util.List<Column> columns,
			java.util.Map<Column, String> columnOrderMap,
			boolean unique) {
		StringBuilder statement = new StringBuilder( dialect.getCreateIndexString( unique ) )
				.append( " " )
				.append( dialect.qualifyIndexName() ? name : unqualify( name ) )
				.append( " on " )
				.append( tableName )
				.append( " (" );
		boolean first = true;
		for ( Column column : columns ) {
			if ( first ) {
				first = false;
			}
			else {
				statement.append(", ");
			}
			statement.append( column.getQuotedName( dialect ) );
			if ( columnOrderMap.containsKey( column ) ) {
				statement.append( " " ).append( columnOrderMap.get( column ) );
			}
		}
		statement.append( ")" );
		statement.append( dialect.getCreateIndexTail( unique, columns ) );

		return statement.toString();
	}

	public Table getTable() {
		return table;
	}

	public void setTable(Table table) {
		this.table = table;
	}

	public void setUnique(boolean unique) {
		this.unique = unique;
	}

	public boolean isUnique() {
		return unique;
	}

	public int getColumnSpan() {
		return selectables.size();
	}

	public List<Selectable> getSelectables() {
		return unmodifiableList( selectables );
	}

	public Map<Selectable, String> getSelectableOrderMap() {
		return unmodifiableMap( selectableOrderMap );
	}

	/**
	 * @deprecated use {@link #getSelectables()}
	 */
	@Deprecated(since = "6.3")
	public java.util.List<Column> getColumns() {
		return selectables.stream()
				.map( s -> (Column) s ).collect( toUnmodifiableList() );
	}

	/**
	 * @deprecated use {@link #getSelectableOrderMap()}
	 */
	@Deprecated(since = "6.3")
	public java.util.Map<Column, String> getColumnOrderMap() {
		return selectableOrderMap.entrySet().stream()
				.collect( toUnmodifiableMap( e -> (Column) e.getKey(), Map.Entry::getValue ) );
	}

	public void addColumn(Selectable selectable) {
		if ( !selectables.contains( selectable ) ) {
			selectables.add( selectable );
		}
	}

	public void addColumn(Selectable selectable, String order) {
		addColumn( selectable );
		if ( isNotEmpty( order ) ) {
			selectableOrderMap.put( selectable, order );
		}
	}

	public String getName() {
		return name == null ? null : name.getText();
	}

	public void setName(String name) {
		this.name = Identifier.toIdentifier( name );
	}

	public String getQuotedName(Dialect dialect) {
		return name == null ? null : name.render( dialect );
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + "(" + getName() + ")";
	}

	@Override
	public String getExportIdentifier() {
		return qualify( getTable().getExportIdentifier(), "IDX-" + getName() );
	}
}
