/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.jpa.boot.internal;

import org.hibernate.SessionFactory;
import org.hibernate.SessionFactoryObserver;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.service.spi.ServiceRegistryImplementor;

class ServiceRegistryCloser implements SessionFactoryObserver {
	/**
	 * Singleton access
	 */
	public static final ServiceRegistryCloser INSTANCE = new ServiceRegistryCloser();

	@Override
	public void sessionFactoryCreated(SessionFactory sessionFactory) {
		// nothing to do
	}

	@Override
	public void sessionFactoryClosed(SessionFactory sessionFactory) {
		final SessionFactoryImplementor factoryImplementor = (SessionFactoryImplementor) sessionFactory;
		final ServiceRegistryImplementor serviceRegistry = factoryImplementor.getServiceRegistry();
		serviceRegistry.destroy();
		final ServiceRegistryImplementor basicRegistry =
				(ServiceRegistryImplementor) serviceRegistry.getParentServiceRegistry();
		if ( basicRegistry != null ) {
			basicRegistry.destroy();
		}
	}
}
