/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect;

/**
 * Dialect for Derby/Cloudscape 10.5
 *
 * @author Simon Johnston
 * @author Scott Marlow
 */
@SuppressWarnings("deprecation")
public class DerbyTenFiveDialect extends DerbyDialect {

	public DerbyTenFiveDialect() {
		super(1050);
	}
}
