/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.serialization.entity;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * The class should be in a package that is different from the test
 * so that the test does not have access to the private ID field.
 *
 * @author Gail Badner
 */
@Entity
@Cacheable
public class WithSimpleId {
@Id
private Long id;
}
