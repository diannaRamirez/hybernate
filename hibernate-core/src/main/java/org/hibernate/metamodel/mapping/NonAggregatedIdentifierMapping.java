/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.mapping;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.mapping.IndexedConsumer;
import org.hibernate.metamodel.mapping.internal.IdClassEmbeddable;
import org.hibernate.metamodel.mapping.internal.VirtualIdEmbeddable;
import org.hibernate.sql.results.graph.FetchOptions;
import org.hibernate.sql.results.graph.embeddable.EmbeddableValuedFetchable;

/**
 * A "non-aggregated" composite identifier, which means that the entity itself
 * does not define a singular representation of its identifier like an
 * {@link jakarta.persistence.EmbeddedId} does.
 *
 * An IdClass can be used to provide a simple, singular representation of the
 * identifier for easier reference in API calls.  JPA requires using an IdClass
 * when mapping such identifiers. Hibernate supports mapping such identifiers
 * with or without the IdClass; without, the entity itself is used as the
 * identifier.
 *
 * @see jakarta.persistence.IdClass
 * @see jakarta.persistence.MapsId
 */
public interface NonAggregatedIdentifierMapping extends CompositeIdentifierMapping, EmbeddableValuedFetchable, FetchOptions {
	/**
	 * The virtual-id representation of this id mapping
	 */
	VirtualIdEmbeddable getVirtualIdEmbeddable();

	/**
	 * The id-class representation of this id mapping
	 */
	IdClassEmbeddable getIdClassEmbeddable();

	/**
	 * The id-class, if there is one, otherwise the virtual-id.
	 */
	IdentifierValueMapper getIdentifierValueMapper();

	/**
	 * Think of an AttributeConverter for id values to account for representation
	 * difference between virtual and id-class mappings
	 */
	interface IdentifierValueMapper extends EmbeddableMappingType {
		EmbeddableValuedModelPart getEmbeddedPart();

		/**
		 * Extract the identifier out of the given entity, returning the mapper's
		 * representation
		 */
		Object getIdentifier(Object entity, SharedSessionContractImplementor session);

		/**
		 * Extract the identifier out of the given entity, returning the mapper's
		 * representation
		 */
		void setIdentifier(Object entity, Object id, SharedSessionContractImplementor session);

		/**
		 * Convenience method to iterate the attributes for this mapper's representation
		 */
		default void forEachAttribute(IndexedConsumer<SingularAttributeMapping> consumer) {
			getEmbeddedPart().getEmbeddableTypeDescriptor().forEachAttributeMapping( (IndexedConsumer) consumer );
		}
	}
}
