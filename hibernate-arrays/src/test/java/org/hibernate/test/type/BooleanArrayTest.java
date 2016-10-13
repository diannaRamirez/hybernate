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
public class BooleanArrayTest extends BaseNonConfigCoreFunctionalTestCase {

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithBooleanArrays.class };
	}

	@Before
	public void setUp() {
		final EntityManager em = openSession();
		EntityTransaction et = em.getTransaction();
		et.begin();
		try {
			em.persist( new TableWithBooleanArrays( 1L, new Boolean[]{} ) );
			em.persist( new TableWithBooleanArrays( 2L, new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );
			em.persist( new TableWithBooleanArrays( 3L, null ) );

			Query q;
			q = em.createNamedQuery( "TableWithBooleanArrays.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", new Boolean[]{ Boolean.TRUE, null, Boolean.FALSE } );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_boolean_arrays(id, the_array) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", new Boolean[]{ Boolean.TRUE, null, Boolean.FALSE } );
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
			TableWithBooleanArrays tableRecord;
			tableRecord = em.find( TableWithBooleanArrays.class, 1L );
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{} ) );

			tableRecord = em.find( TableWithBooleanArrays.class, 2L );
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );

			tableRecord = em.find( TableWithBooleanArrays.class, 3L );
			assertThat( tableRecord.getTheArray(), is( (Object) null ) );

			TypedQuery<TableWithBooleanArrays> tq;

			tq = em.createNamedQuery( "TableWithBooleanArrays.JPQL.getById", TableWithBooleanArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );

			tq = em.createNamedQuery( "TableWithBooleanArrays.JPQL.getByData", TableWithBooleanArrays.class );
			tq.setParameter( "data", new Boolean[]{} );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );

			tq = em.createNamedQuery( "TableWithBooleanArrays.Native.getById", TableWithBooleanArrays.class );
			tq.setParameter( "id", 2L );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheArray(), is( new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } ) );

			tq = em.createNamedQuery( "TableWithBooleanArrays.Native.getByData", TableWithBooleanArrays.class );
			tq.setParameter( "data", new Boolean[]{ Boolean.FALSE, Boolean.FALSE, null, Boolean.TRUE } );
			tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );

		}
		finally {
			em.close();
		}
	}

	@Entity( name = "TableWithBooleanArrays" )
	@Table( name = "table_with_boolean_arrays" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithBooleanArrays.JPQL.getById",
				query = "SELECT t FROM TableWithBooleanArrays t WHERE id = :id" ),
		@NamedQuery( name = "TableWithBooleanArrays.JPQL.getByData",
				query = "SELECT t FROM TableWithBooleanArrays t WHERE theArray = :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithBooleanArrays.Native.getById",
				query = "SELECT * FROM table_with_boolean_arrays t WHERE id = :id",
				resultClass = TableWithBooleanArrays.class ),
		@NamedNativeQuery( name = "TableWithBooleanArrays.Native.getByData",
				query = "SELECT * FROM table_with_boolean_arrays t WHERE the_array = :data",
				resultClass = TableWithBooleanArrays.class ),
		@NamedNativeQuery( name = "TableWithBooleanArrays.Native.insert",
				query = "INSERT INTO table_with_boolean_arrays(id, the_array) VALUES ( :id , :data )" )
	} )
	public static class TableWithBooleanArrays {

		@Id
		private Long id;

		@Column( name = "the_array", columnDefinition = "boolean ARRAY" )
		private Boolean[] theArray;

		public TableWithBooleanArrays() {
		}

		public TableWithBooleanArrays(Long id, Boolean[] theArray) {
			this.id = id;
			this.theArray = theArray;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Boolean[] getTheArray() {
			return theArray;
		}

		public void setTheArray(Boolean[] theArray) {
			this.theArray = theArray;
		}
	}
}
