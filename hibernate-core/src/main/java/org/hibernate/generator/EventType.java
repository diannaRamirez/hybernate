/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.generator;

/**
 * Enumerates event types that can result in generation of a new value.
 * A {@link Generator} must specify which events it responds to, by
 * implementing {@link Generator#getEventTypes()}.
 * <p>
 * We usually work with {@linkplain EventTypeSets sets} of event types,
 * even though there are only two types.
 *
 * @author Gavin King
 *
 * @since 6.2
 *
 * @see Generator
 * @see org.hibernate.annotations.Generated
 * @see org.hibernate.annotations.CurrentTimestamp
 * @see EventTypeSets
 */
public enum EventType {
	INSERT,
	UPDATE;
}
