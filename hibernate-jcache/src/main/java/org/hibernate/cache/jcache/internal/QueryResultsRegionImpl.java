/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.jcache.internal;

import javax.cache.Cache;

import org.hibernate.cache.spi.support.AbstractRegion;
import org.hibernate.cache.spi.QueryResultsRegion;

/**
 * @author Chris Dennis
 * @author Alex Snaps
 * @author Steve Ebersole
 */
public class QueryResultsRegionImpl extends AbstractRegion implements QueryResultsRegion {
	public QueryResultsRegionImpl(
			String name,
			JCacheRegionFactory regionFactory,
			Cache cache) {
		super( name, regionFactory, new JCacheAccessImpl( cache ) );
	}
}
