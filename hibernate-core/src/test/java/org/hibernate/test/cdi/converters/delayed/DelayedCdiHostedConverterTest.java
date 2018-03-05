/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.test.cdi.converters.delayed;

import javax.enterprise.inject.se.SeContainer;
import javax.enterprise.inject.se.SeContainerInitializer;

import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.jpa.event.spi.JpaIntegrator;
import org.hibernate.tool.schema.Action;

import org.hibernate.testing.junit4.BaseUnitTestCase;
import org.hibernate.test.cdi.converters.ConverterBean;
import org.hibernate.test.cdi.converters.MonitorBean;
import org.hibernate.test.cdi.converters.TheEntity;
import org.junit.Test;

import static org.hibernate.testing.transaction.TransactionUtil2.inTransaction;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Steve Ebersole
 */
public class DelayedCdiHostedConverterTest extends BaseUnitTestCase {
	@Test
	public void testIt() {
		MonitorBean.reset();

		final SeContainerInitializer cdiInitializer = SeContainerInitializer.newInstance()
				.disableDiscovery()
				.addBeanClasses( MonitorBean.class, ConverterBean.class );
		try ( final SeContainer cdiContainer = cdiInitializer.initialize() ) {
			BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder()
					.applyIntegrator( new JpaIntegrator() )
					.build();

			final StandardServiceRegistry ssr = new StandardServiceRegistryBuilder( bsr )
					.applySetting( AvailableSettings.HBM2DDL_AUTO, Action.CREATE_DROP )
					.applySetting( AvailableSettings.CDI_BEAN_MANAGER, cdiContainer.getBeanManager() )
					.applySetting( AvailableSettings.DELAY_CDI_ACCESS, "true" )
					.build();

			final SessionFactoryImplementor sessionFactory;

			try {
				sessionFactory = (SessionFactoryImplementor) new MetadataSources( ssr )
						.addAnnotatedClass( TheEntity.class )
						.buildMetadata()
						.getSessionFactoryBuilder()
						.build();
			}
			catch ( Exception e ) {
				StandardServiceRegistryBuilder.destroy( ssr );
				throw e;
			}

			// The CDI bean should not have been built immediately...
			assertFalse( MonitorBean.wasInstantiated() );
			assertEquals( 0, MonitorBean.currentFromDbCount() );
			assertEquals( 0, MonitorBean.currentToDbCount() );

			try {
				inTransaction(
						sessionFactory,
						session -> session.persist( new TheEntity( 1, "me", 5 ) )
				);

				// The CDI bean should have been built on first use
				assertTrue( MonitorBean.wasInstantiated() );
				assertEquals( 0, MonitorBean.currentFromDbCount() );
				assertEquals( 1, MonitorBean.currentToDbCount() );

				inTransaction(
						sessionFactory,
						session -> {
							TheEntity it = session.find( TheEntity.class, 1 );
							assertNotNull( it );
						}
				);

				assertEquals( 1, MonitorBean.currentFromDbCount() );
				assertEquals( 1, MonitorBean.currentToDbCount() );
			}
			finally {
				inTransaction(
						sessionFactory,
						session -> {
							session.createQuery( "delete TheEntity" ).executeUpdate();
						}
				);

				sessionFactory.close();
			}
		}
	}
}
