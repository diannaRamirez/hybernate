/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.type;

import java.io.Serializable;

import org.hibernate.type.descriptor.java.LongTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.BigIntTypeDescriptor;

/**
 * A type that maps between {@link java.sql.Types#BIGINT BIGINT} and {@link Long}
 *
 * @author Gavin King
 * @author Steve Ebersole
 */
public class LongType
		extends AbstractSingleColumnStandardBasicType<Long>
		implements PrimitiveType<Long>, DiscriminatorType<Long> {

	public static final LongType INSTANCE = new LongType();

	private static final Long ZERO = (long) 0;

	public LongType() {
		super( BigIntTypeDescriptor.INSTANCE, LongTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "long";
	}

	@Override
	public String[] getRegistrationKeys() {
		return new String[] { getName(), long.class.getName(), Long.class.getName() };
	}

	@Override
	public Serializable getDefaultValue() {
		return ZERO;
	}

	@Override
	public Class getPrimitiveClass() {
		return long.class;
	}

	@Override
	public Long stringToObject(CharSequence sequence) throws Exception {
		return Long.valueOf( sequence.toString() );
	}

}
