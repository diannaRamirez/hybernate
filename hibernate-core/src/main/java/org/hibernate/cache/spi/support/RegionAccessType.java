/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.spi.support;

/**
 * @author Steve Ebersole
 */
public enum RegionAccessType {
	ENTITY,
	NATURAL_ID,
	COLLECTION,
	QUERY_RESULTS,
	TIMESTAMPS
}
