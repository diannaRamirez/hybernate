/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.generics;


import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.AbstractHANADialect;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * @author Emmanuel Bernard
 */
@DomainModel(
		annotatedClasses = {
				Paper.class,
				PaperType.class,
				SomeGuy.class,
				Price.class,
				WildEntity.class,

				//test at deployment only test unbound property when default field access is used
				Dummy.class
		}
)
@SessionFactory
@ServiceRegistry(settings = @ServiceRegistry.Setting(name = Environment.AUTO_CLOSE_SESSION, value = "true"))
public class GenericsTest {

	@SkipForDialect(dialectClass = AbstractHANADialect.class, reason = "known bug in HANA: rs.next() returns false for org.hibernate.id.enhanced.SequenceStructure$1.getNextValue() for this test")
	@Test
	public void testManyToOneGenerics(SessionFactoryScope scope) {
		Paper white = new Paper();
		white.setName( "WhiteA4" );
		PaperType type = new PaperType();
		type.setName( "A4" );
		SomeGuy me = new SomeGuy();
		white.setType( type );
		white.setOwner( me );
		Price price = new Price();
		price.setAmount( new Double( 1 ) );
		price.setCurrency( "Euro" );
		white.setValue( price );

		Session s = scope.getSessionFactory().openSession();
		Transaction tx = s.beginTransaction();
		try {
			s.persist( type );
			s.persist( price );
			s.persist( me );
			s.persist( white );
			tx.commit();
			//s.close();

			s = scope.getSessionFactory().openSession();
			tx = s.beginTransaction();
			white = s.get( Paper.class, white.getId() );
			s.delete( white.getType() );
			s.delete( white.getOwner() );
			s.delete( white.getValue() );
			s.delete( white );
			tx.commit();
			//s.close();
			assertFalse( s.isOpen() );
		}
		finally {
			if ( tx.isActive() ) {
				tx.rollback();
			}
		}
	}
}
