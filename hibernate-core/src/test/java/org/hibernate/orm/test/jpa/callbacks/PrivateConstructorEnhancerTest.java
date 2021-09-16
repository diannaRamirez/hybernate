/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.jpa.callbacks;

import java.util.List;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.PersistenceException;

import org.hibernate.InstantiationException;
import org.hibernate.engine.spi.SessionFactoryImplementor;

import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

@RunWith(BytecodeEnhancerRunner.class)
public class PrivateConstructorEnhancerTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] {
				Person.class,
				Country.class
		};
	}

	private Country country = new Country( "Romania" );
	private Person person = new Person( "Vlad Mihalcea", country );

	@Override
	protected void afterSessionFactoryBuilt(SessionFactoryImplementor sessionFactory) {
		doInHibernate( this::sessionFactory, session -> {
			session.persist( country );

			session.persist( person );
		} );
	}

	@Test
	public void testFindEntity() {
		inTransaction( (session) -> {
			try {
				Country country = session.find( Country.class, this.country.id );

				assertNotNull( country.getName() );
				fail( "Should have thrown exception" );
			}
			catch (PersistenceException expected) {
				assertThat( expected.getCause() ).isInstanceOf( InstantiationException.class );
			}
		} );
	}

	@Test
	public void testGetReferenceEntity() {
		inTransaction( (session) -> {
			try {
				Country country = session.getReference( Country.class, this.country.id );

				assertNotNull( country.getName() );
				fail( "Should have thrown exception" );
			}
			catch (PersistenceException expected) {
				assertThat( expected.getCause() ).isInstanceOf( InstantiationException.class );
			}
		} );
	}

	@Test
	public void testLoadProxyAssociation() {
		inTransaction( (session) -> {
			try {
				Person person = session.find( Person.class, this.person.id );

				assertNotNull( person.getCountry().getName() );
				fail( "Should have thrown exception" );
			}
			catch (PersistenceException expected) {
				assertThat( expected.getCause() ).isInstanceOf( InstantiationException.class );
			}
		} );
	}

	@Test
	public void testListEntity() {
		inTransaction( (session) -> {
			try {
				List<Person> persons = session.createQuery( "select p from Person p" ).getResultList();
				assertTrue( persons.stream().anyMatch( p -> p.getCountry().getName().equals( "Romania" ) ) );

				fail( "Should have thrown exception" );
			}
			catch (PersistenceException expected) {
				assertThat( expected.getCause() ).isInstanceOf( InstantiationException.class );
			}
		} );
	}

	@Test
	public void testListJoinFetchEntity() {
		inTransaction( (session) -> {
			try {
				List<Person> persons = session.createQuery( "select p from Person p join fetch p.country" )
						.getResultList();
				assertTrue( persons.stream().anyMatch( p -> p.getCountry().getName().equals( "Romania" ) ) );

				fail( "Should have thrown exception" );
			}
			catch (PersistenceException expected) {
				assertThat( expected.getCause() ).isInstanceOf( InstantiationException.class );
			}
		} );
	}

	@Entity(name = "Person")
	private static class Person {

		@Id
		@GeneratedValue
		private int id;

		private String name;

		@ManyToOne(fetch = FetchType.LAZY)
		private Country country;

		public Person() {
		}

		private Person(String name, Country country) {
			this.name = name;
			this.country = country;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Country getCountry() {
			return country;
		}
	}

	@Entity(name = "Country")
	private static class Country {

		@Id
		@GeneratedValue
		private int id;

		@Basic(fetch = FetchType.LAZY)
		private String name;

		private Country(String name) {
			this.name = name;
		}

		public int getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
