/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Type;
import java.util.List;

import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.usertype.CompositeUserType;

/**
 * Defines a strategy for accessing property values via a CompositeUserType.
 *
 * @author Christian Beikov
 */
public class PropertyAccessStrategyCompositeUserTypeImpl implements PropertyAccessStrategy {

	final CompositeUserType<Object> compositeUserType;
	final List<String> sortedPropertyNames;
	final List<Type> sortedPropertyTypes;

	public PropertyAccessStrategyCompositeUserTypeImpl(
			CompositeUserType<?> compositeUserType,
			List<String> sortedPropertyNames,
			List<Type> sortedPropertyTypes) {
		//noinspection unchecked
		this.compositeUserType = (CompositeUserType<Object>) compositeUserType;
		this.sortedPropertyNames = sortedPropertyNames;
		this.sortedPropertyTypes = sortedPropertyTypes;
	}

	@Override
	public PropertyAccess buildPropertyAccess(Class<?> containerJavaType, final String propertyName, boolean setterRequired) {
		return new PropertyAccessCompositeUserTypeImpl( this, propertyName );
	}
}
