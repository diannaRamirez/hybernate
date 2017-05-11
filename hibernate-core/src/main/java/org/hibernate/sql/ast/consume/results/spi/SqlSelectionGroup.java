/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */

package org.hibernate.sql.ast.consume.results.spi;

import java.util.List;

import org.hibernate.sql.ast.tree.spi.select.SqlSelection;

/**
 * Represents a grouping of SqlSelection references, generally related to a
 * single Navigable
 *
 * @author Steve Ebersole
 */
public interface SqlSelectionGroup {
	List<SqlSelection> getSqlSelections();

}
