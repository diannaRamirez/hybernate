/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.data.basic;

import java.util.List;
import java.util.stream.Stream;

import jakarta.data.Limit;
import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.repository.Query;

public interface IdOperations<T> {
	@Query("where id(this) between ?1 and ?2")
	Stream<T> findByIdBetween(long minimum, long maximum, Sort<T> sort);

	@Query("where id(this) >= ?1")
	List<T> findByIdGreaterThanEqual(long minimum,
										Limit limit,
										Order<T> sorts);

	@Query("where id(this) > ?1")
	T[] findByIdLessThan(long exclusiveMax, Sort<T> primarySort, Sort<T> secondarySort);

	@Query("where id(this) <= ?1")
	List<T> findByIdLessThanEqual(long maximum, Order<T> sorts);
}
