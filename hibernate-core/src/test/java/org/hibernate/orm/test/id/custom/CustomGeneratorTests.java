/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.id.custom;

import org.hibernate.mapping.BasicValue;
import org.hibernate.mapping.Property;

import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.DomainModelScope;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DomainModel( annotatedClasses = TheEntity.class )
@SessionFactory
public class CustomGeneratorTests {
	@Test
	public void verifyModel(DomainModelScope scope) {
		scope.withHierarchy( TheEntity.class, (descriptor) -> {
			final Property idProperty = descriptor.getIdentifierProperty();
			final BasicValue value = (BasicValue) idProperty.getValue();

			assertThat( value.getCustomIdGeneratorCreator() ).isNotNull();

			final String strategy = value.getIdentifierGeneratorStrategy();
			assertThat( strategy ).isEqualTo( "assigned" );
		} );
	}

	@Test
	public void basicUseTest(SessionFactoryScope scope) {
		scope.inTransaction( (session) -> {
			session.persist( new TheEntity( "steve" ) );
		} );
	}
}
