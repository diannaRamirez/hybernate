/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.query.internal;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import jakarta.persistence.TemporalType;

import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.model.domain.AllowableParameterType;
import org.hibernate.type.CalendarDateType;
import org.hibernate.type.CalendarTimeType;
import org.hibernate.type.CalendarType;
import org.hibernate.type.InstantType;
import org.hibernate.type.OffsetDateTimeType;
import org.hibernate.type.OffsetTimeType;
import org.hibernate.type.TimestampType;
import org.hibernate.type.ZonedDateTimeType;
import org.hibernate.type.descriptor.java.TemporalJavaTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * @author Steve Ebersole
 */
public class BindingTypeHelper {
	/**
	 * Singleton access
	 */
	public static final BindingTypeHelper INSTANCE = new BindingTypeHelper();

	private BindingTypeHelper() {
	}

	public <T> AllowableParameterType<T> resolveTemporalPrecision(
			TemporalType precision,
			AllowableParameterType<T> declaredParameterType,
			TypeConfiguration typeConfiguration) {
		if ( precision != null ) {
			if ( !( declaredParameterType.getExpressableJavaTypeDescriptor() instanceof TemporalJavaTypeDescriptor ) ) {
				throw new UnsupportedOperationException(
						"Cannot treat non-temporal parameter type with temporal precision"
				);
			}
			final TemporalJavaTypeDescriptor<T> temporalJtd = (TemporalJavaTypeDescriptor<T>) declaredParameterType.getExpressableJavaTypeDescriptor();
			if ( temporalJtd.getPrecision() != precision ) {
				return typeConfiguration.getBasicTypeRegistry().resolve(
						temporalJtd.resolveTypeForPrecision( precision, typeConfiguration ),
						TemporalJavaTypeDescriptor.resolveJdbcTypeCode( precision )
				);
			}
		}
		return declaredParameterType;
	}

	public AllowableParameterType determineTypeForTemporalType(TemporalType temporalType, AllowableParameterType baseType, Object bindValue) {
		// todo : for 6.0 make TemporalType part of org.hibernate.type.descriptor.java.JdbcRecommendedSqlTypeMappingContext
		//		then we can just ask the org.hibernate.type.basic.BasicTypeFactory to handle this based on its registry
		//
		//   - or for 6.0 make TemporalType part of the state for those BasicType impls dealing with date/time types
		//
		// 	 - or for 6.0 make TemporalType part of Binder contract
		//
		//   - or add a org.hibernate.type.TemporalType#getVariant(TemporalType)
		//
		//	 - or ...

		// todo : (5.2) review Java type handling for sanity.  This part was done quickly ;)

		final Class javaType;

		// Determine the "value java type" :
		// 		prefer to leverage the bindValue java type (if bindValue not null),
		// 		followed by the java type reported by the baseType,
		// 		fallback to java.sql.Timestamp

		if ( bindValue != null ) {
			javaType = bindValue.getClass();
		}
		else if ( baseType != null ) {
			javaType = baseType.getExpressableJavaTypeDescriptor().getJavaTypeClass();
		}
		else {
			javaType = java.sql.Timestamp.class;
		}

		switch ( temporalType ) {
			case TIMESTAMP: {
				return resolveTimestampTemporalTypeVariant( javaType, baseType );
			}
			case DATE: {
				return resolveDateTemporalTypeVariant( javaType, baseType );
			}
			case TIME: {
				return resolveTimeTemporalTypeVariant( javaType, baseType );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected TemporalType [" + temporalType + "]; expecting TIMESTAMP, DATE or TIME" );
			}
		}
	}

	public JdbcMapping resolveBindType(Object value, JdbcMapping baseType) {
		if ( value == null || !( baseType.getJavaTypeDescriptor() instanceof TemporalJavaTypeDescriptor<?> ) ) {
			return baseType;
		}

		final Class<?> javaType = value.getClass();
		final TemporalType temporalType = ( (TemporalJavaTypeDescriptor<?>) baseType.getJavaTypeDescriptor() ).getPrecision();
		switch ( temporalType ) {
			case TIMESTAMP: {
				return (JdbcMapping) resolveTimestampTemporalTypeVariant( javaType, (AllowableParameterType<?>) baseType );
			}
			case DATE: {
				return (JdbcMapping) resolveDateTemporalTypeVariant( javaType, (AllowableParameterType<?>) baseType );
			}
			case TIME: {
				return (JdbcMapping) resolveTimeTemporalTypeVariant( javaType, (AllowableParameterType<?>) baseType );
			}
			default: {
				throw new IllegalArgumentException( "Unexpected TemporalType [" + temporalType + "]; expecting TIMESTAMP, DATE or TIME" );
			}
		}
	}

	public AllowableParameterType resolveTimestampTemporalTypeVariant(Class javaType, AllowableParameterType baseType) {
		//noinspection unchecked
		if ( baseType.getExpressableJavaTypeDescriptor().getJavaTypeClass().isAssignableFrom( javaType ) ) {
			return baseType;
		}

		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return CalendarType.INSTANCE;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return TimestampType.INSTANCE;
		}

		if ( Instant.class.isAssignableFrom( javaType ) ) {
			return InstantType.INSTANCE;
		}

		if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return OffsetDateTimeType.INSTANCE;
		}

		if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return ZonedDateTimeType.INSTANCE;
		}

		if ( OffsetTime.class.isAssignableFrom( javaType ) ) {
			return OffsetTimeType.INSTANCE;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIMESTAMP" );
	}

	public AllowableParameterType<?> resolveDateTemporalTypeVariant(Class<?> javaType, AllowableParameterType<?> baseType) {
		if ( baseType.getExpressableJavaTypeDescriptor().getJavaTypeClass().isAssignableFrom( javaType ) ) {
			return baseType;
		}

		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return CalendarDateType.INSTANCE;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return TimestampType.INSTANCE;
		}

		if ( Instant.class.isAssignableFrom( javaType ) ) {
			return OffsetDateTimeType.INSTANCE;
		}

		if ( OffsetDateTime.class.isAssignableFrom( javaType ) ) {
			return OffsetDateTimeType.INSTANCE;
		}

		if ( ZonedDateTime.class.isAssignableFrom( javaType ) ) {
			return ZonedDateTimeType.INSTANCE;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#DATE" );
	}

	public AllowableParameterType resolveTimeTemporalTypeVariant(Class javaType, AllowableParameterType baseType) {
		if ( Calendar.class.isAssignableFrom( javaType ) ) {
			return CalendarTimeType.INSTANCE;
		}

		if ( java.util.Date.class.isAssignableFrom( javaType ) ) {
			return TimestampType.INSTANCE;
		}

		throw new IllegalArgumentException( "Unsure how to handle given Java type [" + javaType.getName() + "] as TemporalType#TIME" );
	}
}
