/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.boot.model.domain.internal;

import javax.persistence.metamodel.Type.PersistenceType;

import org.hibernate.boot.model.domain.EntityMappingHierarchy;
import org.hibernate.boot.model.domain.MappedSuperclassMapping;
import org.hibernate.boot.model.domain.PersistentAttributeMapping;
import org.hibernate.boot.model.domain.spi.MappedSuperclassImplementor;
import org.hibernate.metamodel.model.creation.spi.RuntimeModelCreationContext;
import org.hibernate.metamodel.model.domain.spi.IdentifiableTypeDescriptor;
import org.hibernate.type.descriptor.java.spi.MappedSuperclassJavaDescriptor;

/**
 * @author Chris Cranford
 */
public abstract class AbstractMappedSuperclassMapping
		extends AbstractIdentifiableTypeMapping
		implements MappedSuperclassImplementor {

	public AbstractMappedSuperclassMapping(
			EntityMappingHierarchy entityMappingHierarchy,
			MappedSuperclassJavaDescriptor javaTypeDescriptor) {
		super( entityMappingHierarchy, javaTypeDescriptor );
	}

	@Override
	public void addDeclaredPersistentAttribute(PersistentAttributeMapping attribute) {
		for ( PersistentAttributeMapping existingAttribute : getDeclaredPersistentAttributes() ) {
			if ( attribute.getName().equals( existingAttribute.getName() ) ) {
				return;
			}
		}
		super.addDeclaredPersistentAttribute( attribute );
	}

	@Override
	public MappedSuperclassMapping getSuperManagedTypeMapping() {
		return (MappedSuperclassMapping) super.getSuperManagedTypeMapping();
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.MAPPED_SUPERCLASS;
	}

	@Override
	public <X> IdentifiableTypeDescriptor<X> makeRuntimeDescriptor(RuntimeModelCreationContext creationContext) {
		return creationContext.getRuntimeModelDescriptorFactory().createMappedSuperclassDescriptor(
				this,
				creationContext
		);
	}
}
