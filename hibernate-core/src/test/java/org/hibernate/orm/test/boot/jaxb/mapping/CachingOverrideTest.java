/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.test.boot.jaxb.mapping;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Steve Ebersole
 */
@DomainModel( annotatedClasses = SimpleEntity.class, xmlMappings = "xml/jaxb/mapping/partial/caching.xml" )
public class CachingOverrideTest {
	@Test
	public void verifyMapping(DomainModelScope scope) {
		scope.withHierarchy( SimpleEntity.class, (entityDescriptor) -> {
			assertThat( entityDescriptor.isCached() ).isTrue();
			assertThat( entityDescriptor.getCacheRegionName() ).isEqualTo( "netherworld" );
			assertThat( entityDescriptor.getCacheConcurrencyStrategy() ).isEqualTo( "transactional" );
		} );
	}
}
