/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.persistence.Id;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.Table;
import javax.persistence.TypedQuery;
import javax.persistence.Query;

import org.junit.Before;
import org.junit.Test;

import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.hibernate.testing.DialectChecks;
import org.hibernate.testing.RequiresDialectFeature;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Jordan Gigov
 */
@RequiresDialectFeature(DialectChecks.HasArrayDatatypes.class)
public class ShortArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithShortArrays.class };
	}

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			em.persist(new TableWithShortArrays( 1L, new Short[]{} ) );
			em.persist(new TableWithShortArrays( 2L, new Short[]{ 512, 112, null, 0 } ) );
			em.persist(new TableWithShortArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithShortArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Short[]{ null, null, 0 } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_short_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Short[]{ null, null, 0 } );
			q.executeUpdate();

			et.commit();
		}
		catch ( Exception e ) {
			if ( et.isActive() ) {
				et.rollback();
			}
			throw e;
		}
		finally {
			em.close();
		}
	}

	@Test
	public void testArrays() {
		final EntityManager em = openSession();
		try {
			TableWithShortArrays tableRecord;
			tableRecord = em.find(TableWithShortArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Short[]{} ) );

			tableRecord = em.find(TableWithShortArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Short[]{ 512, 112, null, 0 } ) );

			tableRecord = em.find(TableWithShortArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			TypedQuery<TableWithShortArrays> tq;

			tq = em.createNamedQuery("TableWithShortArrays.JPQL.getById", TableWithShortArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Short[]{ 512, 112, null, 0 } ) );

			tq = em.createNamedQuery("TableWithShortArrays.JPQL.getByData", TableWithShortArrays.class );
			tq.setParameter( "data", new Short[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery("TableWithShortArrays.Native.getById", TableWithShortArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Short[]{ 512, 112, null, 0 } ) );

			tq = em.createNamedQuery("TableWithShortArrays.Native.getByData", TableWithShortArrays.class );
			tq.setParameter( "data", new Short[]{ 512, 112, null, 0 } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithShortArrays" )
	@Table( name = "table_with_short_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithShortArrays.JPQL.getById",
				query = "SELECT t FROM TableWithShortArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithShortArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithShortArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithShortArrays.Native.getById",
				query = "SELECT * FROM table_with_short_arrays t WHERE id = :id",
				resultClass = TableWithShortArrays.class ),
		@NamedNativeQuery( name = "TableWithShortArrays.Native.getByData",
				query = "SELECT * FROM table_with_short_arrays t WHERE the_array = :data",
				resultClass = TableWithShortArrays.class ),
		@NamedNativeQuery( name = "TableWithShortArrays.Native.insert",
				query = "INSERT INTO table_with_short_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithShortArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "smallint ARRAY" )
		private Short[] theArray;

		public TableWithShortArrays() {
		}

		public TableWithShortArrays(Long id, Short[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Short[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Short[] theArray) {
			this.theArray = theArray;
		}
	}

}
