/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.annotations.collectionelement;

import org.hibernate.cfg.AvailableSettings;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

/**
 * Tests @ElementCollection using the "improved" NamingStrategyDelegator which complies
 * with JPA spec.
 *
 * @author Gail Badner
 */
@ServiceRegistry(settings = {
		@Setting(name = AvailableSettings.IMPLICIT_NAMING_STRATEGY, value = "default")
})
public class ImprovedNamingCollectionElementTest extends DefaultNamingCollectionElementTest {

	@Test
	@JiraKey(value = "HHH-9387")
	public void testDefaultTableNameOwnerEntityNameAndPKColumnOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		checkDefaultCollectionTableName( scope.getMetadataImplementor(), Matrix.class, "mvalues", "Mtx_mvalues" );
	}

	@Test
	@JiraKey(value = "HHH-9387")
	public void testDefaultTableNameOwnerPrimaryTableAndEntityNamesOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		checkDefaultCollectionTableName( scope.getMetadataImplementor(), Owner.class, "elements", "OWNER_elements" );
	}

	@Test
	@JiraKey(value = "HHH-9389")
	public void testDefaultJoinColumnOwnerEntityNameAndPKColumnOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Matrix has @Entity(name="Mtx"); entity table name defaults to "Mtx"; owner PK column is configured as "mId"
		checkDefaultJoinColumnName( scope.getMetadataImplementor(), Matrix.class, "mvalues", "Mtx_mId" );
	}

	@Test
	@JiraKey(value = "HHH-9389")
	public void testDefaultJoinColumnOwnerPrimaryTableAndEntityNamesOverride(SessionFactoryScope scope) {
		// NOTE: expected JPA entity names are explicit here (rather than just getting them from the PersistentClass)
		//       to ensure that entity names/tables are not changed (which would invalidate these test cases).

		// Owner has @Entity( name="OWNER") @Table( name="OWNER_TABLE")
		checkDefaultJoinColumnName( scope.getMetadataImplementor(), Owner.class, "elements", "OWNER_id" );
	}
}
