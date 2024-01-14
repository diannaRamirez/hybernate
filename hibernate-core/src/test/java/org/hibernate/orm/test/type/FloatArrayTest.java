/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.Map;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;
import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jordan Gigov
 * @author Christian Beikov
 */
@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class FloatArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithFloatArrays.class };
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
			em.persist( new TableWithFloatArrays( 1L, new Float[]{} ) );
			em.persist( new TableWithFloatArrays( 2L, new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );
			em.persist( new TableWithFloatArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithFloatArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Float[]{ null, null, 0.0f } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_float_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Float[]{ null, null, 0.0f } );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithFloatArrays tableRecord;
			tableRecord = em.find( TableWithFloatArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Float[]{} ) );

			tableRecord = em.find( TableWithFloatArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );

			tableRecord = em.find( TableWithFloatArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithFloatArrays> tq = em.createNamedQuery( "TableWithFloatArrays.JPQL.getById", TableWithFloatArrays.class );
			tq.setParameter( "id", 2L );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithFloatArrays> tq = em.createNamedQuery( "TableWithFloatArrays.JPQL.getByData", TableWithFloatArrays.class );
			tq.setParameter( "data", new Float[]{} );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithFloatArrays> tq = em.createNamedQuery( "TableWithFloatArrays.Native.getById", TableWithFloatArrays.class );
			tq.setParameter( "id", 2L );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Float[]{ 512.5f, 112.0f, null, -0.5f } ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			TypedQuery<TableWithFloatArrays> tq = em.createNativeQuery(
					"SELECT * FROM table_with_float_arrays t WHERE the_array " + op + " :data",
					TableWithFloatArrays.class
			);
			tq.setParameter( "data", new Float[]{ 512.5f, 112.0f, null, -0.5f } );
			TableWithFloatArrays tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Test
	@RequiresDialectFeature(DialectChecks.SupportsArrayDataTypes.class)
	public void testNativeQueryUntyped() {
		inSession( em -> {
			Query q = em.createNamedQuery( "TableWithFloatArrays.Native.getByIdUntyped" );
			q.setParameter( "id", 2L );
			Object[] tuple = (Object[]) q.getSingleResult();
			if ( em.getSessionFactory().getJdbcServices().getDialect() instanceof HSQLDialect ) {
				// In HSQL, float is a synonym for double
				assertThat( tuple[1], is( new Double[] { 512.5d, 112.0d, null, -0.5d } ) );
			}
			else {
				assertThat( tuple[1], is( new Float[] { 512.5f, 112.0f, null, -0.5f } ) );
			}
		} );
	}

	@Entity( name = "TableWithFloatArrays" )
	@Table( name = "table_with_float_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithFloatArrays.JPQL.getById",
				query = "SELECT t FROM TableWithFloatArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithFloatArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithFloatArrays t WHERE theArray IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.getById",
				query = "SELECT * FROM table_with_float_arrays t WHERE id = :id",
				resultClass = TableWithFloatArrays.class ),
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.getByIdUntyped",
				query = "SELECT * FROM table_with_float_arrays t WHERE id = :id" ),
		@NamedNativeQuery( name = "TableWithFloatArrays.Native.insert",
				query = "INSERT INTO table_with_float_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithFloatArrays {

		@Id
		private Long id;

		@Column( name = "the_array" )
		private Float[] theArray;

		public TableWithFloatArrays() {
		}

		public TableWithFloatArrays(Long id, Float[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Float[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Float[] theArray) {
			this.theArray = theArray;
		}
	}
}
