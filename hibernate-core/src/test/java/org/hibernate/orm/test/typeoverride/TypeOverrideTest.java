/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.typeoverride;

import org.hibernate.boot.MetadataBuilder;
import org.hibernate.dialect.AbstractHANADialect;
import org.hibernate.dialect.CockroachDialect;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.PostgreSQL81Dialect;
import org.hibernate.dialect.PostgreSQLDialect;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.type.descriptor.jdbc.BlobJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.IntegerJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.JdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarbinaryJdbcTypeDescriptor;
import org.hibernate.type.descriptor.jdbc.VarcharJdbcTypeDescriptor;

import org.hibernate.testing.orm.junit.BaseSessionFactoryFunctionalTest;
import org.hibernate.testing.orm.junit.SkipForDialect;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

/**
 * @author Gail Badner
 */
public class TypeOverrideTest extends BaseSessionFactoryFunctionalTest {

	@Override
	protected String[] getOrmXmlFiles() {
		return new String[] { "org/hibernate/orm/test/typeoverride/Entity.hbm.xml" };
	}

	@Override
	protected void applyMetadataBuilder(MetadataBuilder metadataBuilder) {
		metadataBuilder.applyBasicType( StoredPrefixedStringType.INSTANCE );
	}

	@Test
	public void testStandardBasicSqlTypeDescriptor() {
		// no override
		assertSame( IntegerJdbcTypeDescriptor.INSTANCE, remapSqlTypeDescriptor( IntegerJdbcTypeDescriptor.INSTANCE ) );

		// A few dialects explicitly override BlobTypeDescriptor.DEFAULT
		if ( CockroachDialect.class.isInstance( getDialect() ) ) {
			assertSame(
					VarbinaryJdbcTypeDescriptor.INSTANCE,
					getDialect().remapSqlTypeDescriptor( BlobJdbcTypeDescriptor.DEFAULT )
			);
		}
		else if ( PostgreSQL81Dialect.class.isInstance( getDialect() ) || PostgreSQLDialect.class.isInstance( getDialect() ) ) {
			assertSame(
					BlobJdbcTypeDescriptor.BLOB_BINDING,
					getDialect().remapSqlTypeDescriptor( BlobJdbcTypeDescriptor.DEFAULT )
			);
		}
		else if ( SybaseDialect.class.isInstance( getDialect() ) ) {
			assertSame(
					BlobJdbcTypeDescriptor.PRIMITIVE_ARRAY_BINDING,
					getDialect().remapSqlTypeDescriptor( BlobJdbcTypeDescriptor.DEFAULT )
			);
		}
		else if ( AbstractHANADialect.class.isInstance( getDialect() ) ) {
			assertSame(
					( (AbstractHANADialect) getDialect() ).getBlobTypeDescriptor(),
					getDialect().remapSqlTypeDescriptor( BlobJdbcTypeDescriptor.DEFAULT )
			);
		}
		else {
			assertSame(
					BlobJdbcTypeDescriptor.DEFAULT,
					getDialect().remapSqlTypeDescriptor( BlobJdbcTypeDescriptor.DEFAULT )
			);
		}
	}

	@Test
	public void testNonStandardSqlTypeDescriptor() {
		// no override
		JdbcTypeDescriptor jdbcTypeDescriptor = new IntegerJdbcTypeDescriptor() {
			@Override
			public boolean canBeRemapped() {
				return false;
			}
		};
		assertSame( jdbcTypeDescriptor, remapSqlTypeDescriptor( jdbcTypeDescriptor ) );
	}

	@Test
	public void testDialectWithNonStandardSqlTypeDescriptor() {
		assertNotSame( VarcharJdbcTypeDescriptor.INSTANCE, StoredPrefixedStringType.INSTANCE.getJdbcTypeDescriptor() );
		final Dialect dialect = new H2DialectOverridePrefixedVarcharSqlTypeDesc();
		final JdbcTypeDescriptor remapped = remapSqlTypeDescriptor(
				dialect,
				StoredPrefixedStringType.PREFIXED_VARCHAR_TYPE_DESCRIPTOR
		);
		assertSame( VarcharJdbcTypeDescriptor.INSTANCE, remapped );
	}

	private JdbcTypeDescriptor remapSqlTypeDescriptor(JdbcTypeDescriptor jdbcTypeDescriptor) {
		return remapSqlTypeDescriptor( sessionFactory().getDialect(), jdbcTypeDescriptor );
	}

	private JdbcTypeDescriptor remapSqlTypeDescriptor(Dialect dialect, JdbcTypeDescriptor jdbcTypeDescriptor) {
		return dialect.remapSqlTypeDescriptor( jdbcTypeDescriptor );
	}

	@AfterEach
	public void tearDown() {
		inTransaction(
				session ->
						session.createQuery( "delete from Entity" ).executeUpdate()
		);
	}

	@Test
	public void testInsert() {
		Entity e = new Entity( "name" );
		inTransaction(
				session ->
						session.save( e )
		);

		inTransaction(
				session -> {
					Entity entity = session.get( Entity.class, e.getId() );
					assertFalse( entity.getName().startsWith( StoredPrefixedStringType.PREFIX ) );
					assertEquals( "name", entity.getName() );
					session.delete( entity );
				}
		);
	}

	@Test
	@SkipForDialect(dialectClass = SybaseDialect.class, matchSubTypes = true, reason = "HHH-6426")
	public void testRegisteredFunction() {
		Entity e = new Entity( "name " );
		inTransaction(
				session ->
						session.save( e )
		);

		inTransaction(
				session -> {
					Entity entity = session.get( Entity.class, e.getId() );
					assertFalse( entity.getName().startsWith( StoredPrefixedStringType.PREFIX ) );
					assertEquals( "name ", entity.getName() );
				}
		);

		inTransaction(
				session ->
						session.delete( e )
		);
	}
}





