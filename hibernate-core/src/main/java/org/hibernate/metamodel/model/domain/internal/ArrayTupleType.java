/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.metamodel.model.domain.internal;

import java.util.Arrays;
import java.util.List;

import org.hibernate.metamodel.mapping.MappingModelExpressable;
import org.hibernate.metamodel.model.domain.AllowableFunctionReturnType;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.metamodel.model.domain.TupleType;
import org.hibernate.query.sqm.SqmExpressable;
import org.hibernate.type.descriptor.java.JavaTypeDescriptor;
import org.hibernate.type.descriptor.java.ObjectArrayTypeDescriptor;

/**
 * @author Christian Beikov
 */
public class ArrayTupleType implements TupleType<Object[]>, AllowableParameterType<Object[]>, AllowableFunctionReturnType<Object[]>,
		MappingModelExpressable {

	private final ObjectArrayTypeDescriptor javaTypeDescriptor;
	private final SqmExpressable<?>[] components;

	public ArrayTupleType(SqmExpressable<?>[] components) {
		this.components = components;
		this.javaTypeDescriptor = new ObjectArrayTypeDescriptor( getTypeDescriptors( components ) );
	}

	private static JavaTypeDescriptor<?>[] getTypeDescriptors(SqmExpressable<?>[] components) {
		final JavaTypeDescriptor<?>[] typeDescriptors = new JavaTypeDescriptor<?>[components.length];
		for ( int i = 0; i < components.length; i++ ) {
			typeDescriptors[i] = components[i].getExpressableJavaTypeDescriptor();
		}
		return typeDescriptors;
	}

	@Override
	public int componentCount() {
		return components.length;
	}

	@Override
	public String getComponentName(int index) {
		throw new UnsupportedOperationException( "Array tuple has no component names" );
	}

	@Override
	public List<String> getComponentNames() {
		throw new UnsupportedOperationException( "Array tuple has no component names" );
	}

	@Override
	public SqmExpressable<?> get(int index) {
		return components[index];
	}

	@Override
	public SqmExpressable<?> get(String componentName) {
		throw new UnsupportedOperationException( "Array tuple has no component names" );
	}

	@Override
	public JavaTypeDescriptor<Object[]> getExpressableJavaTypeDescriptor() {
		return javaTypeDescriptor;
	}

	@Override
	public PersistenceType getPersistenceType() {
		return PersistenceType.EMBEDDABLE;
	}

	@Override
	public Class<Object[]> getJavaType() {
		return getExpressableJavaTypeDescriptor().getJavaTypeClass();
	}

	@Override
	public String toString() {
		return "ArrayTupleType" + Arrays.toString( components );
	}
}
