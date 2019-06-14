/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.bytecode.enhancement.lazy.proxy;

import java.util.ArrayList;
import java.util.List;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PersistenceException;
import javax.persistence.Table;

import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.bytecode.enhance.spi.interceptor.EnhancementAsProxyLazinessInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.PersistentAttributeInterceptable;
import org.hibernate.engine.spi.PersistentAttributeInterceptor;
import org.hibernate.stat.Statistics;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.bytecode.enhancement.BytecodeEnhancerRunner;
import org.hibernate.testing.bytecode.enhancement.EnhancementOptions;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.hamcrest.MatcherAssert;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * @author Andrea Boriero
 */
@TestForIssue(jiraKey = "HHH-11147")
@RunWith(BytecodeEnhancerRunner.class)
@EnhancementOptions(lazyLoading = true)
public class SetIdentifierOnAEnhancedProxyTest extends BaseNonConfigCoreFunctionalTestCase {


	@Override
	protected void configureStandardServiceRegistryBuilder(StandardServiceRegistryBuilder ssrb) {
		super.configureStandardServiceRegistryBuilder( ssrb );
		ssrb.applySetting( AvailableSettings.ALLOW_ENHANCEMENT_AS_PROXY, "true" );
		ssrb.applySetting( AvailableSettings.FORMAT_SQL, "false" );
		ssrb.applySetting( AvailableSettings.USE_SECOND_LEVEL_CACHE, "false" );
		ssrb.applySetting( AvailableSettings.ENABLE_LAZY_LOAD_NO_TRANS, "true" );
		ssrb.applySetting( AvailableSettings.GENERATE_STATISTICS, "true" );
	}

	private static final int CHILDREN_SIZE = 10;
	private Long lastChildID;

	@Override
	public Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Parent.class, Child.class, Person.class };
	}

	@Test
	public void setIdTest() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		inTransaction(
				session -> {
					stats.clear();
					Child loadedChild = session.load( Child.class, lastChildID );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					MatcherAssert.assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					loadedChild.setId( lastChildID );

					// ^ should have triggered "base fetch group" initialization which would mean a SQL select
					assertEquals( 1, stats.getPrepareStatementCount() );

					// check that the `#setName` "persisted"
					assertThat( loadedChild.getId(), is( lastChildID ) );
					assertEquals( 1, stats.getPrepareStatementCount() );

				}
		);

		inTransaction(
				session -> {
					Child loadedChild = session.load( Child.class, lastChildID );
					assertThat( loadedChild, is( notNullValue() ) );
				}
		);

	}

	@Ignore
	@Test(expected = PersistenceException.class)
	public void updateIdTest() {
		final Statistics stats = sessionFactory().getStatistics();
		stats.clear();

		Long updatedId = new Long( lastChildID + 1 );

		inTransaction(
				session -> {
					stats.clear();
					Child loadedChild = session.load( Child.class, lastChildID );

					final PersistentAttributeInterceptable interceptable = (PersistentAttributeInterceptable) loadedChild;
					final PersistentAttributeInterceptor interceptor = interceptable.$$_hibernate_getInterceptor();
					MatcherAssert.assertThat( interceptor, instanceOf( EnhancementAsProxyLazinessInterceptor.class ) );

					loadedChild.setId( updatedId );

					// ^ should have triggered "base fetch group" initialization which would mean a SQL select
					assertEquals( 1, stats.getPrepareStatementCount() );

					// check that the `#setName` "persisted"
					assertThat( loadedChild.getId(), is( updatedId ) );
					assertEquals( 1, stats.getPrepareStatementCount() );

				}
		);

	}

	@Before
	public void prepare() {
		doInHibernate( this::sessionFactory, s -> {
			Parent parent = new Parent();
			for ( int i = 0; i < CHILDREN_SIZE; i++ ) {
				Child child = new Child();
				child.setId( new Long( i ) );
				// Association management should kick in here
				child.parent = parent;

				Person relative = new Person();
				relative.setName( "Luigi" );
				child.addRelative( relative );

				s.persist( relative );

				s.persist( child );
				lastChildID = child.id;
			}
			s.persist( parent );
		} );
	}

	@After
	public void tearDown() {
		doInHibernate( this::sessionFactory, s -> {
			s.createQuery( "delete from Child" ).executeUpdate();
			s.createQuery( "delete from Parent" ).executeUpdate();
		} );
	}

	@Entity(name = "Parent")
	@Table(name = "PARENT")
	private static class Parent {

		String name;

		public Parent() {
		}

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		@OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
		List<Child> children;

		void setChildren(List<Child> children) {
			this.children = children;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Person")
	@Table(name = "Person")
	private static class Person {
		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		Long id;

		String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}
	}

	@Entity(name = "Child")
	@Table(name = "CHILD")
	private static class Child {

		@Id
		Long id;

		@ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
//		@LazyToOne(LazyToOneOption.NO_PROXY)
				Parent parent;

		@OneToMany
		List<Person> relatives;

		String name;

		Child() {
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		Child(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public Parent getParent() {
			return parent;
		}

		public void setParent(Parent parent) {
			this.parent = parent;
		}

		public List<Person> getRelatives() {
			return relatives;
		}

		public void setRelatives(List<Person> relatives) {
			this.relatives = relatives;
		}

		public void addRelative(Person person) {
			if ( this.relatives == null ) {
				this.relatives = new ArrayList<>();
			}
			this.relatives.add( person );
		}
	}
}
