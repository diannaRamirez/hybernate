/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.property.access.internal;

import java.lang.reflect.Method;

import org.hibernate.internal.util.ReflectHelper;
import org.hibernate.property.access.spi.Getter;
import org.hibernate.property.access.spi.GetterMethodImpl;
import org.hibernate.property.access.spi.PropertyAccess;
import org.hibernate.property.access.spi.PropertyAccessStrategy;
import org.hibernate.property.access.spi.Setter;
import org.hibernate.property.access.spi.SetterMethodImpl;

import org.jboss.logging.Logger;

/**
 * PropertyAccessor for accessing the wrapped property via get/set pair, which may be nonpublic.
 *
 * @author Steve Ebersole
 *
 * @see PropertyAccessStrategyBasicImpl
 */
public class PropertyAccessBasicImpl implements PropertyAccess, java.io.Externalizable {
	private static final Logger log = Logger.getLogger( PropertyAccessBasicImpl.class );

	private PropertyAccessStrategyBasicImpl strategy;
	private Class containerJavaType;
	private String propertyName;

	private transient GetterMethodImpl getter;
	private transient SetterMethodImpl setter;

	public PropertyAccessBasicImpl() {}

	public PropertyAccessBasicImpl(
			PropertyAccessStrategyBasicImpl strategy,
			Class containerJavaType,
			final String propertyName) {
		this.strategy = strategy;
		this.containerJavaType = containerJavaType;
		this.propertyName = propertyName;

		initTransients();
	}

	private void initTransients() {
		final Method getterMethod = ReflectHelper.findGetterMethod( containerJavaType, propertyName );
		this.getter = new GetterMethodImpl( containerJavaType, propertyName, getterMethod );

		final Method setterMethod = ReflectHelper.findSetterMethod( containerJavaType, propertyName, getterMethod.getReturnType() );
		this.setter = new SetterMethodImpl( containerJavaType, propertyName, setterMethod );

	}

	@Override
	public PropertyAccessStrategy getPropertyAccessStrategy() {
		return strategy;
	}

	@Override
	public Getter getGetter() {
		return getter;
	}

	@Override
	public Setter getSetter() {
		return setter;
	}

	public void writeExternal(java.io.ObjectOutput out) throws java.io.IOException {
		out.writeObject(strategy);
		out.writeObject(containerJavaType);
		out.writeObject(propertyName);
	}

	public void readExternal(java.io.ObjectInput in) throws java.io.IOException, ClassNotFoundException {
		strategy = (PropertyAccessStrategyBasicImpl)in.readObject();
		containerJavaType = (Class)in.readObject();
		propertyName = (String)in.readObject();

		initTransients();
	}
}
