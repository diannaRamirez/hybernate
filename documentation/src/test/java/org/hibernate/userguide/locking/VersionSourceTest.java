/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.userguide.locking;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Version;
import org.hibernate.annotations.CurrentTimestamp;


import java.time.LocalDateTime;

import org.hibernate.annotations.SourceType;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Vlad Mihalcea
 */
@DomainModel( annotatedClasses = VersionSourceTest.Person.class )
@SessionFactory
public class VersionSourceTest {

	@Test
	public void test(SessionFactoryScope scope) {
		scope.inTransaction( entityManager -> {
			//tag::locking-optimistic-version-timestamp-source-persist-example[]
			Person person = new Person();
			person.setId(1L);
			person.setFirstName("John");
			person.setLastName("Doe");
			assertNull(person.getVersion());

			entityManager.persist(person);
			assertNotNull(person.getVersion());
			//end::locking-optimistic-version-timestamp-source-persist-example[]
		});
		sleep();
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.setFirstName("Jane");
		});
		sleep();
		scope.inTransaction( entityManager -> {
			Person person = entityManager.find(Person.class, 1L);
			person.setFirstName("John");
		});
	}

	private static void sleep() {
		try {
			Thread.sleep(300);
		}
		catch (InterruptedException ignored) {
		}
	}

	//tag::locking-optimistic-version-timestamp-source-mapping-example[]
	@Entity(name = "Person")
	public static class Person {

		@Id
		private Long id;

		private String firstName;

		private String lastName;

		@Version @CurrentTimestamp(source = SourceType.VM)
		private LocalDateTime version;
	//end::locking-optimistic-version-timestamp-source-mapping-example[]

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public String getFirstName() {
			return firstName;
		}

		public void setFirstName(String firstName) {
			this.firstName = firstName;
		}

		public String getLastName() {
			return lastName;
		}

		public void setLastName(String lastName) {
			this.lastName = lastName;
		}

		public LocalDateTime getVersion() {
			return version;
		}
	}
}
