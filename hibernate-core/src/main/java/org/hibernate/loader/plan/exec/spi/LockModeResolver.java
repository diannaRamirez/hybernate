/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.loader.plan.exec.spi;

import java.io.Serializable;

import org.hibernate.LockMode;
import org.hibernate.loader.plan.spi.EntityReference;

/**
 * @author Steve Ebersole
 */
public interface LockModeResolver extends Serializable {
	public LockMode resolveLockMode(EntityReference entityReference);
}
