/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.spi;

import java.lang.annotation.Annotation;

import org.hibernate.annotations.DialectOverride;
import org.hibernate.dialect.Dialect;
import org.hibernate.models.spi.AnnotationDescriptor;

/**
 * Common contract for {@linkplain DialectOverride.OverridesAnnotation override} annotations defined in {@linkplain DialectOverride}
 *
 * @author Steve Ebersole
 */
public interface DialectOverrider<O extends Annotation> extends Annotation {
	Class<? extends Dialect> dialect();

	DialectOverride.Version before();

	DialectOverride.Version sameOrAfter();

	O override();

	AnnotationDescriptor<O> getOverriddenDescriptor();

	boolean matches(Dialect dialectToMatch);
}