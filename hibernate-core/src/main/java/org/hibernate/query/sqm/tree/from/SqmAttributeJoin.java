/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.from;

import org.hibernate.query.criteria.JpaFetch;
import org.hibernate.query.criteria.JpaJoin;
import org.hibernate.query.sqm.tree.predicate.SqmPredicate;
import org.hibernate.sql.ast.produce.metamodel.spi.Joinable;
import org.hibernate.type.descriptor.java.spi.JavaTypeDescriptor;

/**
 * Models a join based on a mapped attribute reference.
 *
 * @author Steve Ebersole
 */
public interface SqmAttributeJoin<O,T> extends SqmQualifiedJoin<O,T>, JpaFetch<O,T>, JpaJoin<O,T> {
	@Override
	SqmFrom<?,O> getLhs();

	@Override
	Joinable<O,T> getReferencedNavigable();

	@Override
	JavaTypeDescriptor<T> getJavaTypeDescriptor();

	boolean isFetched();

	@Override
	SqmPredicate getJoinPredicate();

	void setJoinPredicate(SqmPredicate predicate);

}
