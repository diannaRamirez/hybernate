/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */

package org.hibernate.spatial.dialect.cockroachdb;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.query.sqm.function.SqmFunctionRegistry;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.spatial.FunctionKey;
import org.hibernate.spatial.GeolatteGeometryType;
import org.hibernate.spatial.HSMessageLogger;
import org.hibernate.spatial.JTSGeometryType;
import org.hibernate.spatial.contributor.ContributorImplementor;
import org.hibernate.spatial.dialect.postgis.PGGeometryTypeDescriptor;
import org.hibernate.spatial.dialect.postgis.PostgisSqmFunctionDescriptors;

public class CockroachDbContributor implements ContributorImplementor {

	private final ServiceRegistry serviceRegistry;

	public CockroachDbContributor(ServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public void contributeTypes(TypeContributions typeContributions) {
		HSMessageLogger.LOGGER.typeContributions( this.getClass().getCanonicalName() );
		typeContributions.contributeType( new GeolatteGeometryType( PGGeometryTypeDescriptor.INSTANCE_WKB_2 ) );
		typeContributions.contributeType( new JTSGeometryType( PGGeometryTypeDescriptor.INSTANCE_WKB_2 ) );
	}

	@Override
	public void contributeFunctions(SqmFunctionRegistry functionRegistry) {
		HSMessageLogger.LOGGER.functionContributions( this.getClass().getCanonicalName() );
		PostgisSqmFunctionDescriptors postgisFunctions = new PostgisSqmFunctionDescriptors( getServiceRegistry() );

		postgisFunctions.asMap()
				.forEach( (key, desc) -> {
					if ( isUnsupported( key ) ) {
						return;
					}
					functionRegistry.register( key.getName(), desc );
					key.getAltName().ifPresent( altName -> functionRegistry.registerAlternateKey(
							altName,
							key.getName()
					) );
				} );
	}

	private boolean isUnsupported(FunctionKey key) {
		return key.getName().equalsIgnoreCase( "st_union" );
	}

	@Override
	public ServiceRegistry getServiceRegistry() {
		return this.serviceRegistry;
	}
}
