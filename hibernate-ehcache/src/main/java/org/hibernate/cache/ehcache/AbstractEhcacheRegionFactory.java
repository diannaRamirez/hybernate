/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * Copyright (c) 2008, Red Hat Middleware LLC or third-party contributors as
 * indicated by the @author tags or express copyright attribution
 * statements applied by the authors.  All third-party contributions are
 * distributed under license by Red Hat Middleware LLC.
 *
 * This copyrighted material is made available to anyone wishing to use, modify,
 * copy, or redistribute it subject to the terms and conditions of the GNU
 * Lesser General Public License, as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this distribution; if not, write to:
 * Free Software Foundation, Inc.
 * 51 Franklin Street, Fifth Floor
 * Boston, MA  02110-1301  USA
 */
package org.hibernate.cache.ehcache;

import net.sf.ehcache.CacheManager;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.util.ClassLoaderUtil;
import org.hibernate.cache.CacheException;
import org.hibernate.cache.ehcache.management.impl.ProviderMBeanRegistrationHelper;
import org.hibernate.cache.ehcache.nonstop.NonstopAccessStrategyFactory;
import org.hibernate.cache.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.cache.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.cache.ehcache.regions.EhcacheQueryResultsRegion;
import org.hibernate.cache.ehcache.regions.EhcacheTimestampsRegion;
import org.hibernate.cache.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.ehcache.strategy.EhcacheAccessStrategyFactoryImpl;
import org.hibernate.cache.ehcache.management.impl.ProviderMBeanRegistrationHelper;
import org.hibernate.cache.ehcache.nonstop.NonstopAccessStrategyFactory;
import org.hibernate.cache.ehcache.regions.EhcacheCollectionRegion;
import org.hibernate.cache.ehcache.regions.EhcacheEntityRegion;
import org.hibernate.cache.ehcache.regions.EhcacheQueryResultsRegion;
import org.hibernate.cache.ehcache.regions.EhcacheTimestampsRegion;
import org.hibernate.cache.ehcache.strategy.EhcacheAccessStrategyFactory;
import org.hibernate.cache.ehcache.strategy.EhcacheAccessStrategyFactoryImpl;
import org.hibernate.cache.spi.CacheDataDescription;
import org.hibernate.cache.spi.CollectionRegion;
import org.hibernate.cache.spi.EntityRegion;
import org.hibernate.cache.spi.QueryResultsRegion;
import org.hibernate.cache.spi.RegionFactory;
import org.hibernate.cache.spi.TimestampsRegion;
import org.hibernate.cache.spi.access.AccessType;
import org.hibernate.cfg.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Properties;

/**
 * Abstract implementation of an Ehcache specific RegionFactory.
 *
 * @author Chris Dennis
 * @author Greg Luck
 * @author Emmanuel Bernard
 * @author Abhishek Sanoujam
 * @author Alex Snaps
 */
abstract class AbstractEhcacheRegionFactory implements RegionFactory {

	/**
	 * The Hibernate system property specifying the location of the ehcache configuration file name.
	 * <p/>
	 * If not set, ehcache.xml will be looked for in the root of the classpath.
	 * <p/>
	 * If set to say ehcache-1.xml, ehcache-1.xml will be looked for in the root of the classpath.
	 */
	public static final String NET_SF_EHCACHE_CONFIGURATION_RESOURCE_NAME = "net.sf.ehcache.configurationResourceName";

	private static final Logger LOG = LoggerFactory.getLogger(AbstractEhcacheRegionFactory.class);

	/**
	 * MBean registration helper class instance for Ehcache Hibernate MBeans.
	 */
	protected final ProviderMBeanRegistrationHelper mbeanRegistrationHelper = new ProviderMBeanRegistrationHelper();

	/**
	 * Ehcache CacheManager that supplied Ehcache instances for this Hibernate RegionFactory.
	 */
	protected volatile CacheManager manager;

	/**
	 * Settings object for the Hibernate persistence unit.
	 */
	protected Settings settings;

	/**
	 * {@link EhcacheAccessStrategyFactory} for creating various access strategies
	 */
	protected final EhcacheAccessStrategyFactory accessStrategyFactory =
		new NonstopAccessStrategyFactory(new EhcacheAccessStrategyFactoryImpl());

	/**
	 * Whether to optimize for minimals puts or minimal gets.
	 * <p/>
	 * Indicates whether when operating in non-strict read/write or read-only mode
	 * Hibernate should optimize the access patterns for minimal puts or minimal gets.
	 * In Ehcache we default to minimal puts since this should have minimal to no
	 * affect on unclustered users, and has great benefit for clustered users.
	 * <p/>
	 * This setting can be overridden by setting the "hibernate.cache.use_minimal_puts"
	 * property in the Hibernate configuration.
	 *
	 * @return true, optimize for minimal puts
	 */
	public boolean isMinimalPutsEnabledByDefault() {
		return true;
	}

	/**
	 * {@inheritDoc}
	 */
	public long nextTimestamp() {
		return net.sf.ehcache.util.Timestamper.next();
	}

	/**
	 * {@inheritDoc}
	 */
	public EntityRegion buildEntityRegion(String regionName, Properties properties, CacheDataDescription metadata) throws CacheException {
		return new EhcacheEntityRegion(accessStrategyFactory, getCache(regionName), settings, metadata, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public CollectionRegion buildCollectionRegion(String regionName, Properties properties, CacheDataDescription metadata)
		throws CacheException {
		return new EhcacheCollectionRegion(accessStrategyFactory, getCache(regionName), settings, metadata, properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public QueryResultsRegion buildQueryResultsRegion(String regionName, Properties properties) throws CacheException {
		return new EhcacheQueryResultsRegion(accessStrategyFactory, getCache(regionName), properties);
	}

	/**
	 * {@inheritDoc}
	 */
	public TimestampsRegion buildTimestampsRegion(String regionName, Properties properties) throws CacheException {
		return new EhcacheTimestampsRegion(accessStrategyFactory, getCache(regionName), properties);
	}

	private Ehcache getCache(String name) throws CacheException {
		try {
			Ehcache cache = manager.getEhcache(name);
			if (cache == null) {
				LOG.warn("Couldn't find a specific ehcache configuration for cache named [" + name + "]; using defaults.");
				manager.addCache(name);
				cache = manager.getEhcache(name);
				LOG.debug("started EHCache region: " + name);
			}
			HibernateUtil.validateEhcache(cache);
			return cache;
		} catch (net.sf.ehcache.CacheException e) {
			throw new CacheException(e);
		}

	}

	/**
	 * Load a resource from the classpath.
	 */
	protected static URL loadResource(String configurationResourceName) {
		ClassLoader standardClassloader = ClassLoaderUtil.getStandardClassLoader();
		URL url = null;
		if (standardClassloader != null) {
			url = standardClassloader.getResource(configurationResourceName);
		}
		if (url == null) {
			url = AbstractEhcacheRegionFactory.class.getResource(configurationResourceName);
		}

		LOG.debug("Creating EhCacheRegionFactory from a specified resource: {}.  Resolved to URL: {}", configurationResourceName, url);
		if (url == null) {

			LOG.warn("A configurationResourceName was set to {} but the resource could not be loaded from the classpath." +
							 "Ehcache will configure itself using defaults.", configurationResourceName);
		}
		return url;
	}

	/**
	 * Default access-type used when the configured using JPA 2.0 config.  JPA 2.0 allows <code>@Cacheable(true)</code> to be attached to an
	 * entity without any access type or usage qualification.
	 * <p/>
	 * We are conservative here in specifying {@link AccessType#READ_WRITE} so as to follow the mantra of "do no harm".
	 * <p/>
	 * This is a Hibernate 3.5 method.
	 */
	public AccessType getDefaultAccessType() {
		return AccessType.READ_WRITE;
	}

}
