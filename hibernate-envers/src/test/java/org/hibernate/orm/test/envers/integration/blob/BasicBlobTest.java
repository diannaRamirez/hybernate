/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.envers.integration.blob;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hibernate.testing.transaction.TransactionUtil.doInJPA;
import static org.junit.Assert.fail;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Blob;

import org.hamcrest.Matchers;
import org.hibernate.engine.jdbc.BlobProxy;
import org.hibernate.envers.Audited;
import org.hibernate.orm.test.envers.BaseEnversJPAFunctionalTestCase;
import org.hibernate.orm.test.envers.Priority;
import org.junit.Test;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;

/**
 * @author Chris Cranford
 */
public class BasicBlobTest extends BaseEnversJPAFunctionalTestCase {

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Asset.class };
	}

	@Test
	@Priority(10)
	public void initData() {
		final Path path = Path.of( getClass().getResource( "./blob.txt" ).getPath() );
		doInJPA( this::entityManagerFactory, entityManager -> {
			try {
				final Asset asset = new Asset();
				asset.setFileName( "blob.txt" );

				final InputStream stream = new BufferedInputStream( Files.newInputStream( path ) );
				assertThat( stream.markSupported(), Matchers.is( true ) );

				// We use the method readAllBytes instead of passing the raw stream to the proxy
				// since this is the only guaranteed way that will work across all dialects in a
				// deterministic way.  Postgres and Sybase will automatically close the stream
				// after the blob has been written by the driver, which prevents Envers from
				// then writing the contents of the stream to the audit table.
				//
				// If the driver and dialect are known not to close the input stream after the
				// contents have been written by the driver, then it's safe to pass the stream
				// here instead and the stream will be automatically marked and reset so that
				// Envers can serialize the data after Hibernate has done so.  Dialects like
				// H2, MySQL, Oracle, SQL Server work this way.
				//
				//
				Blob blob = BlobProxy.generateProxy( stream.readAllBytes() );

				asset.setData( blob );
				entityManager.persist( asset );
			}
			catch (Exception e) {
				e.printStackTrace();
				fail( "Failed to persist the entity" );
			}
		} );
	}

	@Audited
	@Entity(name = "Asset")
	public static class Asset {
		@Id
		@GeneratedValue
		private Integer id;
		private String fileName;
		private Blob data;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getFileName() {
			return fileName;
		}

		public void setFileName(String fileName) {
			this.fileName = fileName;
		}

		public Blob getData() {
			return data;
		}

		public void setData(Blob data) {
			this.data = data;
		}
	}

}
