/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class EnumSetTest extends BaseNonConfigCoreFunctionalTestCase {

	private BindableType<Set<MyEnum>> enumSetType;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithEnumSet.class };
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
			enumSetType = em.getTypeConfiguration().getBasicTypeForGenericJavaType( Set.class, MyEnum.class );
			em.persist( new TableWithEnumSet( 1L, new HashSet<>() ) );
			em.persist( new TableWithEnumSet( 2L, EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
			em.persist( new TableWithEnumSet( 3L, null ) );

			QueryImplementor q;
			q = em.createNamedQuery( "TableWithEnumSet.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", EnumSet.of( MyEnum.VALUE2, MyEnum.VALUE1, MyEnum.VALUE3 ), enumSetType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_set(id, the_set) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", EnumSet.of( MyEnum.VALUE2, MyEnum.VALUE1, MyEnum.VALUE3 ), enumSetType );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithEnumSet tableRecord;
			tableRecord = em.find( TableWithEnumSet.class, 1L );
			assertThat( tableRecord.getTheSet(), is( new HashSet<>() ) );

			tableRecord = em.find( TableWithEnumSet.class, 2L );
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );

			tableRecord = em.find( TableWithEnumSet.class, 3L );
			assertThat( tableRecord.getTheSet(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithEnumSet> tq = em.createNamedQuery( "TableWithEnumSet.JPQL.getById", TableWithEnumSet.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithEnumSet> tq = em.createNamedQuery( "TableWithEnumSet.JPQL.getByData", TableWithEnumSet.class );
			tq.setParameter( "data", new HashSet<>() );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithEnumSet> tq = em.createNamedQuery( "TableWithEnumSet.Native.getById", TableWithEnumSet.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			QueryImplementor<TableWithEnumSet> tq = em.createNativeQuery(
					"SELECT * FROM table_with_enum_set t WHERE the_set " + op + " :data",
					TableWithEnumSet.class
			);
			tq.setParameter( "data", EnumSet.of( MyEnum.VALUE1, MyEnum.VALUE2 ), enumSetType );
			TableWithEnumSet tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithEnumSet" )
	@Table( name = "table_with_enum_set" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumSet.JPQL.getById",
				query = "SELECT t FROM TableWithEnumSet t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumSet.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumSet t WHERE theSet IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumSet.Native.getById",
				query = "SELECT * FROM table_with_enum_set t WHERE id = :id",
				resultClass = TableWithEnumSet.class ),
		@NamedNativeQuery( name = "TableWithEnumSet.Native.insert",
				query = "INSERT INTO table_with_enum_set(id, the_set) VALUES ( :id , :data )" )
	} )
	public static class TableWithEnumSet {

		@Id
		private Long id;

		@Enumerated(EnumType.ORDINAL)
		@Column( name = "the_set" )
		private Set<MyEnum> theSet;

		public TableWithEnumSet() {
		}

		public TableWithEnumSet(Long id, Set<MyEnum> theSet) {
			this.id = id;
			this.theSet = theSet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<MyEnum> getTheSet() {
			return theSet;
		}

		public void setTheSet(Set<MyEnum> theSet) {
			this.theSet = theSet;
		}
	}

	public enum MyEnum {
		VALUE1, VALUE2, VALUE3
	}
}
