/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.hql;

import java.util.List;

import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Before;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;

/**
 * @author Christian Beikov
 */
public class QuotedIdentifierTest extends BaseCoreFunctionalTestCase {

	private Person person;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[] {
				Person.class
		};
	}

	@Before
	public void setUp() {
		doInHibernate( this::sessionFactory, session -> {
			person = new Person();
			person.setName( "Chuck" );
			person.setSurname( "Norris" );
			session.persist( person );
		} );
	}

	@Test
	public void testQuotedIdentifier() {
		doInHibernate( this::sessionFactory, session -> {
			TypedQuery<Tuple> query = session.createQuery(
					"select `the person`.`name` as `The person name` " +
							"from `The Person` `the person`",
					Tuple.class
			);
			List<Tuple> resultList = query.getResultList();
			assertEquals( 1, resultList.size() );
			assertEquals( "Chuck", resultList.get( 0 ).get( "The person name" ) );
		} );
	}

	@Entity(name = "The Person")
	public static class Person {

		@Id
		@GeneratedValue
		private Integer id;

		@Column(name = "the name")
		private String name;

		@Column
		private String surname;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getSurname() {
			return surname;
		}

		public void setSurname(String surname) {
			this.surname = surname;
		}

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		@Override
		public boolean equals(Object o) {
			if ( this == o ) {
				return true;
			}
			if ( !( o instanceof Person ) ) {
				return false;
			}

			Person person = (Person) o;

			return id != null ? id.equals( person.id ) : person.id == null;

		}

		@Override
		public int hashCode() {
			return id != null ? id.hashCode() : 0;
		}
	}

}
