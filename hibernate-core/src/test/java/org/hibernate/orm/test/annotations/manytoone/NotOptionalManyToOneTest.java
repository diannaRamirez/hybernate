/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.manytoone;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

import org.hibernate.orm.test.annotations.manytoone.NotOptionalManyToOneTest.Child;
import org.hibernate.orm.test.annotations.manytoone.NotOptionalManyToOneTest.Parent;

import org.hibernate.testing.jdbc.SQLStatementInspector;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * @author Andrea Boriero
 */
@DomainModel(
		annotatedClasses = {
				Parent.class,
				Child.class
		}
)
@SessionFactory(statementInspectorClass = SQLStatementInspector.class)
public class NotOptionalManyToOneTest {

	@BeforeEach
	public void setUp(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					Child child = new Child( 1, "Luigi" );
					Parent parent = new Parent( 2, "Roberto", child );

					session.save( child );
					session.save( parent );
				}
		);
	}

	@AfterEach
	public void tearDown(SessionFactoryScope scope) {
		scope.inTransaction(
				session -> {
					session.createQuery( "delete from Parent" ).executeUpdate();
					session.createQuery( "delete from Child" ).executeUpdate();
				}
		);
	}

	@Test
	public void testInnerJoinIsUsed(SessionFactoryScope scope) {
		SQLStatementInspector statementInspector = (SQLStatementInspector) scope.getStatementInspector();
		statementInspector.clear();
		scope.inTransaction(
				session -> {
					session.get( Parent.class, 2 );
					statementInspector.assertNumberOfOccurrenceInQuery( 0, "inner", 1 );
				}
		);
	}

	@Entity(name = "Parent")
	public static class Parent {
		@Id
		private Integer id;

		private String name;

		@ManyToOne(optional = false)
		private Child child;

		public Parent() {
		}

		public Parent(Integer id, String name, Child child) {
			this.id = id;
			this.name = name;
			this.child = child;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public Child getChild() {
			return child;
		}
	}

	@Entity(name = "Child")
	public static class Child {
		@Id
		private Integer id;

		private String name;

		public Child() {
		}

		public Child(Integer id, String name) {
			this.id = id;
			this.name = name;
		}

		public Integer getId() {
			return id;
		}

		public String getName() {
			return name;
		}
	}
}
