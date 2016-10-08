/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.test.type.descriptor.java;

import org.hibernate.type.ArrayTypes;

/**
 * @author Jordan Gigov
 */
public class LongArrayDescriptorTest extends AbstractDescriptorTest<Long[]> {
	final Long[] original = new Long[]{ 13l, -2l, 666l };
	final Long[] copy = new Long[]{ 13l, -2l, 666l };
	final Long[] different = new Long[]{ -2l, 666l, 13l };

	public LongArrayDescriptorTest() {
		super(ArrayTypes.LONG.getJavaTypeDescriptor());
	}

	@Override
	protected Data<Long[]> getTestData() {
		return new Data<Long[]>( original, copy, different );
	}

	@Override
	protected boolean shouldBeMutable() {
		return false;
	}
	
}
