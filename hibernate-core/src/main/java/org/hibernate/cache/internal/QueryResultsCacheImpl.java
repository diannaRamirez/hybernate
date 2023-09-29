/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.cache.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.hibernate.HibernateException;
import org.hibernate.cache.spi.QueryKey;
import org.hibernate.cache.spi.QueryResultsCache;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.TimestampsCache;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.event.jfr.CacheGetEvent;
import org.hibernate.event.jfr.CachePutEvent;
import org.hibernate.event.jfr.internal.JfrEventManager;

import static org.hibernate.cache.spi.SecondLevelCacheLogger.L2CACHE_LOGGER;

/**
 * The standard implementation of the Hibernate QueryCache interface.  Works
 * hind-in-hand with {@link TimestampsCache} to help in recognizing
 * stale query results.
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class QueryResultsCacheImpl implements QueryResultsCache {

	private final QueryResultsRegion cacheRegion;
	private final TimestampsCache timestampsCache;

	QueryResultsCacheImpl(
			QueryResultsRegion cacheRegion,
			TimestampsCache timestampsCache) {
		this.cacheRegion = cacheRegion;
		this.timestampsCache = timestampsCache;
	}

	@Override
	public QueryResultsRegion getRegion() {
		return cacheRegion;
	}

	@Override
	public boolean put(
			final QueryKey key,
			final List<?> results,
			final SharedSessionContractImplementor session) throws HibernateException {
		if ( L2CACHE_LOGGER.isDebugEnabled() ) {
			L2CACHE_LOGGER.debugf( "Caching query results in region: %s; timestamp=%s",
					cacheRegion.getName(),
					session.getCacheTransactionSynchronization().getCachingTimestamp() );
		}

		final CacheItem cacheItem = new CacheItem(
				session.getCacheTransactionSynchronization().getCachingTimestamp(),
				deepCopy( results )
		);

		final CachePutEvent cachePutEvent = JfrEventManager.beginCachePutEvent();
		try {
			session.getEventListenerManager().cachePutStart();
			cacheRegion.putIntoCache( key, cacheItem, session );
		}
		finally {
			JfrEventManager.completeCachePutEvent(
					cachePutEvent,
					session,
					cacheRegion,
					true,
					JfrEventManager.CacheActionDescription.QUERY_RESULT
			);
			session.getEventListenerManager().cachePutEnd();
		}

		return true;
	}

	private static <T> List<T> deepCopy(List<T> results) {
		return new ArrayList<>( results );
	}

	@Override
	public List<?> get(
			final QueryKey key,
			final Set<String> spaces,
			final SharedSessionContractImplementor session) throws HibernateException {
		final boolean loggerDebugEnabled = L2CACHE_LOGGER.isDebugEnabled();
		if ( loggerDebugEnabled ) {
			L2CACHE_LOGGER.debugf( "Checking cached query results in region: %s", cacheRegion.getName() );
		}

		final CacheItem cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( loggerDebugEnabled ) {
				L2CACHE_LOGGER.debug( "Query results were not found in cache" );
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( loggerDebugEnabled ) {
				L2CACHE_LOGGER.debug( "Cached query results were not up-to-date" );
			}
			return null;
		}

		if ( loggerDebugEnabled ) {
			L2CACHE_LOGGER.debug( "Returning cached query results" );
		}

		return deepCopy( cacheItem.results );
	}

	@Override
	public List<?> get(
			final QueryKey key,
			final String[] spaces,
			final SharedSessionContractImplementor session) throws HibernateException {
		final boolean loggerDebugEnabled = L2CACHE_LOGGER.isDebugEnabled();
		if ( loggerDebugEnabled ) {
			L2CACHE_LOGGER.debugf( "Checking cached query results in region: %s", cacheRegion.getName() );
		}

		final CacheItem cacheItem = getCachedData( key, session );
		if ( cacheItem == null ) {
			if ( loggerDebugEnabled ) {
				L2CACHE_LOGGER.debug( "Query results were not found in cache" );
			}
			return null;
		}

		if ( !timestampsCache.isUpToDate( spaces, cacheItem.timestamp, session ) ) {
			if ( loggerDebugEnabled ) {
				L2CACHE_LOGGER.debug( "Cached query results were not up-to-date" );
			}
			return null;
		}

		if ( loggerDebugEnabled ) {
			L2CACHE_LOGGER.debug( "Returning cached query results" );
		}

		return deepCopy( cacheItem.results );
	}

	private CacheItem getCachedData(QueryKey key, SharedSessionContractImplementor session) {
		CacheItem cachedItem = null;
		final CacheGetEvent cacheGetEvent = JfrEventManager.beginCacheGetEvent();
		try {
			session.getEventListenerManager().cacheGetStart();
			cachedItem = (CacheItem) cacheRegion.getFromCache( key, session );
		}
		finally {
			JfrEventManager.completeCacheGetEvent(
					cacheGetEvent,
					session,
					cacheRegion,
					cachedItem != null
			);
			session.getEventListenerManager().cacheGetEnd( cachedItem != null );
		}
		return cachedItem;
	}

	@Override
	public String toString() {
		return "QueryResultsCache(" + cacheRegion.getName() + ')';
	}

	public static class CacheItem implements Serializable {
		private final Long timestamp;
		private final List<?> results;

		CacheItem(long timestamp, List<?> results) {
			this.timestamp = Long.valueOf( timestamp );
			this.results = results;
		}
	}
}
