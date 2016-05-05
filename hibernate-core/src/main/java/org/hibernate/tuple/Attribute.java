/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.tuple;

import org.hibernate.type.Type;

/**
 * Contract for attributes
 *
 * @author Steve Ebersole
 */
public interface Attribute extends java.io.Serializable {
	public String getName();
	public Type getType();
}
