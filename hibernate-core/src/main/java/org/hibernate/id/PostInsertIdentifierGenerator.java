/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.id;

import org.hibernate.service.ServiceRegistry;
import org.hibernate.tuple.GenerationTiming;
import org.hibernate.generator.InDatabaseGenerator;
import org.hibernate.type.Type;

import java.util.Properties;

/**
 * The counterpart of {@link IdentifierGenerator} for values generated by the database.
 * This interface is no longer the only way to handle database-generate identifiers.
 * Any {@link InDatabaseGenerator} with timing {@link GenerationTiming#INSERT} may now
 * be used.
 *
 * @see IdentifierGenerator
 *
 * @author Gavin King
 */
public interface PostInsertIdentifierGenerator extends InDatabaseGenerator, Configurable {

	/**
	 * @return {@code true}
	 */
	@Override
	default boolean generatedOnInsert() {
		return true;
	}

	/**
	 * @return {@code false}
	 */
	@Override
	default boolean generatedOnUpdate() {
		return false;
	}

	/**
	 * @return {@code false}, since we don't usually have a meaningful property value
	 *         for generated identifiers
	 */
	@Override
	default boolean writePropertyValue() {
		return false;
	}

	/**
	 * Noop default implementation. May be overridden by subtypes.
	 */
	@Override
	default void configure(Type type, Properties params, ServiceRegistry serviceRegistry) {}
}
