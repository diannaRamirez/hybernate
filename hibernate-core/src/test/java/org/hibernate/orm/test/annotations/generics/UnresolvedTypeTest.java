/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.generics;


import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Paolo Perrotta
 */
@DomainModel(
		annotatedClasses = {
				Gene.class,
				DNA.class
		}
)
@SessionFactory
public class UnresolvedTypeTest {

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session ->
						session.createQuery( "delete from Gene" ).executeUpdate()

		);
	}

	@Test
	public void testAcceptsUnresolvedPropertyTypesIfATargetEntityIsExplicitlySet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Gene item = new Gene();
					session.persist( item );
					session.flush();
				}
		);
	}

	@Test
	public void testAcceptsUnresolvedPropertyTypesIfATypeExplicitlySet(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Gene item = new Gene();
					item.setState( State.DORMANT );
					session.persist( item );
					session.flush();
					session.clear();
					item = (Gene) session.get( Gene.class, item.getId() );
					assertEquals( State.DORMANT, item.getState() );
				}
		);
	}

}
