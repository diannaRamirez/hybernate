/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.uniqueconstraint;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.Table;

import org.hibernate.cfg.Configuration;
import org.hibernate.cfg.Environment;
import org.hibernate.dialect.TiDBDialect;
import org.hibernate.exception.ConstraintViolationException;

import org.hibernate.testing.TestForIssue;
import org.hibernate.testing.junit4.BaseCoreFunctionalTestCase;
import org.junit.Test;

import java.sql.SQLIntegrityConstraintViolationException;

import static org.hibernate.testing.transaction.TransactionUtil.doInHibernate;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Vlad Mihalcea
 */
@TestForIssue(jiraKey = "HHH-11236")
public class UniqueConstraintThrowsConstraintViolationExceptionTest extends BaseCoreFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class[] { Customer.class };
	}

	@Test
	public void testUniqueConstraintWithEmptyColumnName() {
		doInHibernate( this::sessionFactory, session -> {
			Customer customer1 = new Customer();
			customer1.customerId = "123";
			session.persist( customer1 );
		} );
		try {
			doInHibernate( this::sessionFactory, session -> {
				Customer customer1 = new Customer();
				customer1.customerId = "123";
				session.persist( customer1 );
			} );
			fail( "Should throw" );
		}
		catch ( PersistenceException e ) {
			Object expectException = ConstraintViolationException.class;
			if (getDialect() instanceof TiDBDialect) {
				expectException = SQLIntegrityConstraintViolationException.class;
			}

			assertEquals(
					expectException,
					e.getCause().getClass()
			);
		}
	}

	@Override
	protected boolean isCleanupTestDataRequired() {
		return true;
	}

	@Entity(name = "Customer")
	@Table(name = "CUSTOMER")
	public static class Customer {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO)
		@Column(name = "CUSTOMER_ACCOUNT_NUMBER")
		public Long customerAccountNumber;

		@Basic
		@Column(name = "CUSTOMER_ID", unique = true)
		public String customerId;

		@Basic
		@Column(name = "BILLING_ADDRESS")
		public String billingAddress;

		public Customer() {
		}
	}
}
