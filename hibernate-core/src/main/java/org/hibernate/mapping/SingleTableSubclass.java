/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.mapping;

import java.util.Iterator;

import org.hibernate.MappingException;
import org.hibernate.boot.model.domain.EntityMapping;
import org.hibernate.boot.spi.MetadataBuildingContext;
import org.hibernate.internal.util.collections.JoinedIterator;
import org.hibernate.type.descriptor.java.spi.EntityJavaDescriptor;

/**
 * @author Gavin King
 */
public class SingleTableSubclass extends Subclass {

	public SingleTableSubclass(
			EntityMapping superclass,
			EntityJavaDescriptor javaTypeDescriptor,
			MetadataBuildingContext metadataBuildingContext) {
		super( superclass, javaTypeDescriptor, metadataBuildingContext );
	}

	@Override
	@SuppressWarnings("unchecked")
	protected Iterator getNonDuplicatedPropertyIterator() {
		return new JoinedIterator(
				getSuperclass().getUnjoinedPropertyIterator(),
				getUnjoinedPropertyIterator()
		);
	}

	@Override
	protected Iterator getDiscriminatorColumnIterator() {
		if ( isDiscriminatorInsertable() && !getDiscriminator().hasFormula() ) {
			return getDiscriminator().getColumnIterator();
		}
		else {
			return super.getDiscriminatorColumnIterator();
		}
	}

	@Override
	public Object accept(PersistentClassVisitor mv) {
		return mv.accept( this );
	}

	@Override
	public void validate() throws MappingException {
		if ( getDiscriminator() == null ) {
			throw new MappingException(
					"No discriminator found for " + getEntityName()
							+ ". Discriminator is needed when 'single-table-per-hierarchy' "
							+ "is used and a class has subclasses"
			);
		}
		super.validate();
	}
}
