/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.engine.spi;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.hibernate.Filter;
import org.hibernate.Internal;
import org.hibernate.UnknownProfileException;
import org.hibernate.engine.profile.Fetch;
import org.hibernate.engine.profile.FetchProfile;
import org.hibernate.graph.GraphSemantic;
import org.hibernate.graph.spi.RootGraphImplementor;
import org.hibernate.internal.FilterImpl;
import org.hibernate.internal.SessionCreationOptions;
import org.hibernate.loader.ast.spi.CascadingFetchProfile;
import org.hibernate.persister.collection.CollectionPersister;
import org.hibernate.persister.entity.EntityPersister;

import org.checkerframework.checker.nullness.qual.Nullable;

import static java.util.Collections.emptySet;
import static org.hibernate.engine.FetchStyle.SUBSELECT;

/**
 * Centralize all options which can influence the SQL query needed to load an
 * entity.  Currently, such influencers are defined as:<ul>
 * <li>filters</li>
 * <li>fetch profiles</li>
 * <li>internal fetch profile (merge profile, etc)</li>
 * </ul>
 *
 * @author Steve Ebersole
 */
public class LoadQueryInfluencers implements Serializable {

	private final SessionFactoryImplementor sessionFactory;

	private CascadingFetchProfile enabledCascadingFetchProfile;

	//Lazily initialized!
	private HashSet<String> enabledFetchProfileNames;

	//Lazily initialized!
	private HashMap<String,Filter> enabledFilters;

	private boolean subselectFetchEnabled;

	private int batchSize = -1;

	private final EffectiveEntityGraph effectiveEntityGraph = new EffectiveEntityGraph();

	private Boolean readOnly;

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory) {
		this.sessionFactory = sessionFactory;
		batchSize = sessionFactory.getSessionFactoryOptions().getDefaultBatchFetchSize();
		subselectFetchEnabled = sessionFactory.getSessionFactoryOptions().isSubselectFetchEnabled();
	}

	public LoadQueryInfluencers(SessionFactoryImplementor sessionFactory, SessionCreationOptions options) {
		this.sessionFactory = sessionFactory;
		batchSize = options.getDefaultBatchFetchSize();
		subselectFetchEnabled = options.isSubselectFetchEnabled();
		for (FilterDefinition filterDefinition : sessionFactory.getAutoEnabledFilters()) {
			FilterImpl filter = new FilterImpl( filterDefinition );
			if ( enabledFilters == null ) {
				this.enabledFilters = new HashMap<>();
			}
			enabledFilters.put( filterDefinition.getFilterName(), filter );
		}
	}

	public EffectiveEntityGraph applyEntityGraph(@Nullable RootGraphImplementor<?> rootGraph, @Nullable GraphSemantic graphSemantic) {
		final EffectiveEntityGraph effectiveEntityGraph = getEffectiveEntityGraph();
		if ( graphSemantic != null ) {
			if ( rootGraph == null ) {
				throw new IllegalArgumentException( "Graph semantic specified, but no RootGraph was supplied" );
			}
			effectiveEntityGraph.applyGraph( rootGraph, graphSemantic );
		}
		return effectiveEntityGraph;
	}

	public SessionFactoryImplementor getSessionFactory() {
		return sessionFactory;
	}


	// internal fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public <T> T fromInternalFetchProfile(CascadingFetchProfile profile, Supplier<T> supplier) {
		final CascadingFetchProfile previous = enabledCascadingFetchProfile;
		enabledCascadingFetchProfile = profile;
		try {
			return supplier.get();
		}
		finally {
			enabledCascadingFetchProfile = previous;
		}
	}

	/**
	 * Fetch-profile to apply, if one, when building the result-graph
	 * for cascade fetching - for example, the result-graph used when
	 * handling a {@linkplain org.hibernate.Session#merge merge} to
	 * immediately load additional based on {@linkplain jakarta.persistence.CascadeType#MERGE}
	 */
	public CascadingFetchProfile getEnabledCascadingFetchProfile() {
		return enabledCascadingFetchProfile;
	}

	public boolean hasEnabledCascadingFetchProfile() {
		return enabledCascadingFetchProfile != null;
	}

	/**
	 * Set the effective {@linkplain #getEnabledCascadingFetchProfile() cascading fetch-profile}
	 */
	public void setEnabledCascadingFetchProfile(CascadingFetchProfile enabledCascadingFetchProfile) {
		this.enabledCascadingFetchProfile = enabledCascadingFetchProfile;
	}

	/**
	 * @deprecated Use {@link #getEnabledCascadingFetchProfile} instead
	 */
	@Deprecated( since = "6.0" )
	public String getInternalFetchProfile() {
		return getEnabledCascadingFetchProfile().getLegacyName();
	}

	/**
	 * @deprecated Use {@link #setEnabledCascadingFetchProfile} instead
	 */
	@Deprecated( since = "6.0" )
	public void setInternalFetchProfile(String internalFetchProfile) {
		setEnabledCascadingFetchProfile( CascadingFetchProfile.fromLegacyName( internalFetchProfile ) );
	}


	// filter support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFilters() {
		return enabledFilters != null && !enabledFilters.isEmpty();
	}

	public Map<String,Filter> getEnabledFilters() {
		if ( enabledFilters == null ) {
			return Collections.emptyMap();
		}
		else {
			// First, validate all the enabled filters...
			for ( Filter filter : enabledFilters.values() ) {
				//TODO: this implementation has bad performance
				filter.validate();
			}
			return enabledFilters;
		}
	}

	/**
	 * Returns a Map of enabled filters that have the applyToLoadById
	 * flag set to true
	 * @return a Map of enabled filters that have the applyToLoadById
	 * flag set to true
	 */
	public Map<String, Filter> getEnabledFiltersForFind() {
		return getEnabledFilters()
				.entrySet()
				.stream()
				.filter(f -> f.getValue().isApplyToLoadById())
				.collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	/**
	 * Returns an unmodifiable Set of enabled filter names.
	 * @return an unmodifiable Set of enabled filter names.
	 */
	public Set<String> getEnabledFilterNames() {
		if ( enabledFilters == null ) {
			return emptySet();
		}
		else {
			return Collections.unmodifiableSet( enabledFilters.keySet() );
		}
	}

	public @Nullable Filter getEnabledFilter(String filterName) {
		if ( enabledFilters == null ) {
			return null;
		}
		else {
			return enabledFilters.get( filterName );
		}
	}

	public Filter enableFilter(String filterName) {
		FilterImpl filter = new FilterImpl( sessionFactory.getFilterDefinition( filterName ) );
		if ( enabledFilters == null ) {
			this.enabledFilters = new HashMap<>();
		}
		enabledFilters.put( filterName, filter );
		return filter;
	}

	public void disableFilter(String filterName) {
		if ( enabledFilters != null ) {
			enabledFilters.remove( filterName );
		}
	}

	public Object getFilterParameterValue(String filterParameterName) {
		final String[] parsed = parseFilterParameterName( filterParameterName );
		if ( enabledFilters == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
		}
		final FilterImpl filter = (FilterImpl) enabledFilters.get( parsed[0] );
		if ( filter == null ) {
			throw new IllegalArgumentException( "Filter [" + parsed[0] + "] currently not enabled" );
		}
		return filter.getParameter( parsed[1] );
	}

	public static String [] parseFilterParameterName(String filterParameterName) {
		int dot = filterParameterName.lastIndexOf( '.' );
		if ( dot <= 0 ) {
			throw new IllegalArgumentException(
					"Invalid filter-parameter name format [" + filterParameterName + "]; expecting {filter-name}.{param-name}"
			);
		}
		final String filterName = filterParameterName.substring( 0, dot );
		final String parameterName = filterParameterName.substring( dot + 1 );
		return new String[] { filterName, parameterName };
	}


	// fetch profile support ~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	public boolean hasEnabledFetchProfiles() {
		return enabledFetchProfileNames != null && !enabledFetchProfileNames.isEmpty();
	}

	public Set<String> getEnabledFetchProfileNames() {
		return enabledFetchProfileNames == null ? emptySet() : enabledFetchProfileNames;
	}

	private void checkFetchProfileName(String name) {
		if ( !sessionFactory.containsFetchProfileDefinition( name ) ) {
			throw new UnknownProfileException( name );
		}
	}

	public boolean isFetchProfileEnabled(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		return enabledFetchProfileNames != null && enabledFetchProfileNames.contains( name );
	}

	public void enableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		if ( enabledFetchProfileNames == null ) {
			this.enabledFetchProfileNames = new HashSet<>();
		}
		enabledFetchProfileNames.add( name );
	}

	public void disableFetchProfile(String name) throws UnknownProfileException {
		checkFetchProfileName( name );
		if ( enabledFetchProfileNames != null ) {
			enabledFetchProfileNames.remove( name );
		}
	}

	@Internal
	public @Nullable HashSet<String> adjustFetchProfiles(@Nullable Set<String> disabledFetchProfiles, @Nullable Set<String> enabledFetchProfiles) {
		final HashSet<String> oldFetchProfiles =
				hasEnabledFetchProfiles() ? new HashSet<>( enabledFetchProfileNames ) : null;
		if ( disabledFetchProfiles != null && enabledFetchProfileNames != null ) {
			enabledFetchProfileNames.removeAll( disabledFetchProfiles );
		}
		if ( enabledFetchProfiles != null ) {
			if ( enabledFetchProfileNames == null ) {
				enabledFetchProfileNames = new HashSet<>();
			}
			enabledFetchProfileNames.addAll( enabledFetchProfiles );
		}
		return oldFetchProfiles;
	}

	@Internal
	public void setEnabledFetchProfileNames(HashSet<String> enabledFetchProfileNames) {
		this.enabledFetchProfileNames = enabledFetchProfileNames;
	}

	public EffectiveEntityGraph getEffectiveEntityGraph() {
		return effectiveEntityGraph;
	}

	public Boolean getReadOnly() {
		return readOnly;
	}

	public void setReadOnly(Boolean readOnly) {
		this.readOnly = readOnly;
	}

	public int getBatchSize() {
		return batchSize;
	}

	public void setBatchSize(int batchSize) {
		this.batchSize = batchSize;
	}

	public int effectiveBatchSize(CollectionPersister persister) {
		int persisterBatchSize = persister.getBatchSize();
		// persister-specific batch size overrides global setting
		// (note that due to legacy, -1 means no explicit setting)
		return persisterBatchSize >= 0 ? persisterBatchSize : batchSize;
	}

	public boolean effectivelyBatchLoadable(CollectionPersister persister) {
		return batchSize > 1 || persister.isBatchLoadable();
	}

	public int effectiveBatchSize(EntityPersister persister) {
		int persisterBatchSize = persister.getBatchSize();
		// persister-specific batch size overrides global setting
		// (note that due to legacy, -1 means no explicit setting)
		return persisterBatchSize >= 0 ? persisterBatchSize : batchSize;
	}

	public boolean effectivelyBatchLoadable(EntityPersister persister) {
		return batchSize > 1 || persister.isBatchLoadable();
	}

	public boolean getSubselectFetchEnabled() {
		return subselectFetchEnabled;
	}

	public void setSubselectFetchEnabled(boolean subselectFetchEnabled) {
		this.subselectFetchEnabled = subselectFetchEnabled;
	}

	public boolean effectiveSubselectFetchEnabled(CollectionPersister persister) {
		return subselectFetchEnabled
			|| persister.isSubselectLoadable()
			|| isSubselectFetchEnabledInProfile( persister );
	}

	private boolean isSubselectFetchEnabledInProfile(CollectionPersister persister) {
		if ( hasEnabledFetchProfiles() ) {
			for ( String profile : getEnabledFetchProfileNames() ) {
				final FetchProfile fetchProfile = persister.getFactory().getFetchProfile( profile )	;
				if ( fetchProfile != null ) {
					final Fetch fetch = fetchProfile.getFetchByRole( persister.getRole() );
					if ( fetch != null && fetch.getMethod() == SUBSELECT) {
						return true;
					}
				}
			}
		}
		return false;
	}

	public boolean hasSubselectLoadableCollections(EntityPersister persister) {
		return persister.hasSubselectLoadableCollections()
			|| subselectFetchEnabled && persister.hasCollections()
			|| hasSubselectLoadableCollectionsEnabledInProfile( persister );
	}

	private boolean hasSubselectLoadableCollectionsEnabledInProfile(EntityPersister persister) {
		if ( hasEnabledFetchProfiles() ) {
			for ( String profile : getEnabledFetchProfileNames() ) {
				if ( persister.getFactory().getFetchProfile( profile )
						.hasSubselectLoadableCollectionsEnabled( persister ) ) {
					return true;
				}
			}
		}
		return false;
	}

}
