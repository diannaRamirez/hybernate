/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.type.descriptor.converter.internal;

import java.io.Serializable;

import org.hibernate.type.descriptor.converter.spi.EnumValueConverter;
import org.hibernate.type.descriptor.java.EnumJavaType;
import org.hibernate.type.descriptor.java.JavaType;
import org.hibernate.type.descriptor.jdbc.JdbcType;

/**
 * BasicValueConverter handling the conversion of an enum based on
 * JPA {@link jakarta.persistence.EnumType#ORDINAL} strategy (storing the ordinal)
 *
 * @author Steve Ebersole
 *
 * @deprecated we no longer use converters to handle enum mappings
 */
@Deprecated(since="6.3", forRemoval=true)
public class OrdinalEnumValueConverter<E extends Enum<E>, N extends Number> implements EnumValueConverter<E, N>, Serializable {

	private final EnumJavaType<E> enumJavaType;
	private final JdbcType jdbcType;
	private final JavaType<N> relationalJavaType;

	public OrdinalEnumValueConverter(
			EnumJavaType<E> enumJavaType,
			JdbcType jdbcType,
			JavaType<N> relationalJavaType) {
		this.enumJavaType = enumJavaType;
		this.jdbcType = jdbcType;
		this.relationalJavaType = relationalJavaType;
	}

	@Override
	public E toDomainValue(Number relationalForm) {
		return enumJavaType.fromOrdinal( relationalForm == null ? null : relationalForm.intValue() );
	}

	@Override
	public N toRelationalValue(E domainForm) {
		return relationalJavaType.wrap( enumJavaType.toOrdinal( domainForm ), null );
	}

	@Override
	public int getJdbcTypeCode() {
		return jdbcType.getDefaultSqlTypeCode();
	}

	@Override
	public EnumJavaType<E> getDomainJavaType() {
		return enumJavaType;
	}

	@Override
	public JavaType<N> getRelationalJavaType() {
		return relationalJavaType;
	}

	@Override
	public String toSqlLiteral(Object value) {
		return Integer.toString( ( (Enum<?>) value ).ordinal() );
	}
}
