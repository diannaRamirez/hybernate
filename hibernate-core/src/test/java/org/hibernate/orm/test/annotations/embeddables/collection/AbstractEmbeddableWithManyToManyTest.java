/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.embeddables.collection;

import org.hibernate.AnnotationException;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.BootstrapServiceRegistry;
import org.hibernate.boot.registry.BootstrapServiceRegistryBuilder;
import org.hibernate.boot.registry.StandardServiceRegistry;

import org.hibernate.testing.util.ServiceRegistryUtil;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Andrea Boriero
 */
public abstract class AbstractEmbeddableWithManyToManyTest {
	@Test
	public void test() {
		try (BootstrapServiceRegistry bsr = new BootstrapServiceRegistryBuilder().build();
			StandardServiceRegistry ssr = ServiceRegistryUtil.serviceRegistryBuilder( bsr ).build()) {
			MetadataSources metadataSources = new MetadataSources( ssr );
			addResources( metadataSources );
			addAnnotatedClasses(metadataSources);

			metadataSources.buildMetadata();
			fail( "Should throw AnnotationException!" );
		}
		catch (AnnotationException expected) {
			assertTrue( expected.getMessage().contains(
					"belongs to an '@Embeddable' class that is contained in an '@ElementCollection' and may not be"
			) );
		}
	}

	protected void addAnnotatedClasses(MetadataSources metadataSources){

	}

	protected void addResources(MetadataSources metadataSources){

	}
}
