/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
public class BasicSortedSetTest extends BaseNonConfigCoreFunctionalTestCase {

	private BindableType<SortedSet<Integer>> integerSortedSetType;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithIntegerSortedSet.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		settings.put( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	public void startUp() {
		super.startUp();
		inTransaction( em -> {
			integerSortedSetType = em.getTypeConfiguration().getBasicTypeForGenericJavaType( SortedSet.class, Integer.class );
			em.persist( new TableWithIntegerSortedSet( 1L, Collections.emptySortedSet() ) );
			em.persist( new TableWithIntegerSortedSet( 2L, new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) ) );
			em.persist( new TableWithIntegerSortedSet( 3L, null ) );

			QueryImplementor q;
			q = em.createNamedQuery( "TableWithIntegerSortedSet.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new TreeSet<>( Arrays.asList( 0 ) ), integerSortedSetType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_integer_sorted_set(id, the_sorted_set) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new TreeSet<>( Arrays.asList( 0 ) ), integerSortedSetType );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithIntegerSortedSet tableRecord;
			tableRecord = em.find( TableWithIntegerSortedSet.class, 1L );
			assertThat( tableRecord.getTheSortedSet(), is( Collections.emptySortedSet() ) );

			tableRecord = em.find( TableWithIntegerSortedSet.class, 2L );
			assertThat( tableRecord.getTheSortedSet(), is( new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) ) );

			tableRecord = em.find( TableWithIntegerSortedSet.class, 3L );
			assertThat( tableRecord.getTheSortedSet(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithIntegerSortedSet> tq = em.createNamedQuery( "TableWithIntegerSortedSet.JPQL.getById", TableWithIntegerSortedSet.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSortedSet(), is( new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithIntegerSortedSet> tq = em.createNamedQuery( "TableWithIntegerSortedSet.JPQL.getByData", TableWithIntegerSortedSet.class );
			tq.setParameter( "data", Collections.emptySortedSet() );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithIntegerSortedSet> tq = em.createNamedQuery( "TableWithIntegerSortedSet.Native.getById", TableWithIntegerSortedSet.class );
			tq.setParameter( "id", 2L );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSortedSet(), is( new TreeSet<>( Arrays.asList( 512, 112, 0 ) ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			QueryImplementor<TableWithIntegerSortedSet> tq = em.createNativeQuery(
					"SELECT * FROM table_with_integer_sorted_set t WHERE the_sorted_set " + op + " :data",
					TableWithIntegerSortedSet.class
			);
			tq.setParameter( "data", new TreeSet<>( Arrays.asList( 512, 112, 0 ) ), integerSortedSetType );
			TableWithIntegerSortedSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithIntegerSortedSet" )
	@Table( name = "table_with_integer_sorted_set" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithIntegerSortedSet.JPQL.getById",
				query = "SELECT t FROM TableWithIntegerSortedSet t WHERE id = :id" ),
		@NamedQuery( name = "TableWithIntegerSortedSet.JPQL.getByData",
				query = "SELECT t FROM TableWithIntegerSortedSet t WHERE theSortedSet IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithIntegerSortedSet.Native.getById",
				query = "SELECT * FROM table_with_integer_sorted_set t WHERE id = :id",
				resultClass = TableWithIntegerSortedSet.class ),
		@NamedNativeQuery( name = "TableWithIntegerSortedSet.Native.getByData",
				query = "SELECT * FROM table_with_integer_sorted_set t WHERE the_sorted_set IS NOT DISTINCT FROM :data",
				resultClass = TableWithIntegerSortedSet.class ),
		@NamedNativeQuery( name = "TableWithIntegerSortedSet.Native.insert",
				query = "INSERT INTO table_with_integer_sorted_set(id, the_sorted_set) VALUES ( :id , :data )" )
	} )
	public static class TableWithIntegerSortedSet {

		@Id
		private Long id;

		@Column( name = "the_sorted_set" )
		private SortedSet<Integer> theSortedSet;

		public TableWithIntegerSortedSet() {
		}

		public TableWithIntegerSortedSet(Long id, SortedSet<Integer> theSortedSet) {
			this.id = id;
			this.theSortedSet = theSortedSet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public SortedSet<Integer> getTheSortedSet() {
			return theSortedSet;
		}

		public void setTheSortedSet(SortedSet<Integer> theSortedSet) {
			this.theSortedSet = theSortedSet;
		}
	}

}
