/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.constraint;

import java.util.Iterator;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;

import org.hibernate.boot.model.relational.Namespace;
import org.hibernate.mapping.Table;

import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * @author Chris Cranford
 */
@JiraKey(value = "HHH-12320")
public class ForeignKeyConstraintMapsIdTest extends BaseNonConfigCoreFunctionalTestCase {
	@Entity(name = "Post")
	public static class Post {
		@Id
		private Integer id;

		@OneToOne(fetch = FetchType.LAZY)
		@MapsId
		@JoinColumn(name = "PD_ID", foreignKey = @ForeignKey(name = "FK_PD"))
		private PostDetails postDetails;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public PostDetails getPostDetails() {
			return postDetails;
		}

		public void setPostDetails(PostDetails postDetails) {
			this.postDetails = postDetails;
		}
	}

	@Entity(name = "PostDetails")
	public static class PostDetails {
		@Id
		private Integer id;
		private String userName;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getUserName() {
			return userName;
		}

		public void setUserName(String userName) {
			this.userName = userName;
		}
	}

	@Override
	protected Class<?>[] getAnnotatedClasses() {
		return new Class<?>[] { Post.class, PostDetails.class };
	}

	@Test
	public void testForeignKeyNameSetForMapsIdJoinColumn() {
		for ( Namespace namespace : metadata().getDatabase().getNamespaces() ) {
			for ( Table table : namespace.getTables() ) {
				if ( table.getName().equals( "Post" ) ) {
					Iterator<org.hibernate.mapping.ForeignKey> foreignKeyIterator = table.getForeignKeys().values().iterator();
					while ( foreignKeyIterator.hasNext() ) {
						org.hibernate.mapping.ForeignKey foreignKey = foreignKeyIterator.next();
						if ( foreignKey.getColumn( 0 ).getName().equals( "PD_ID" ) ) {
							assertEquals( "FK_PD", foreignKey.getName() );
							return;
						}
					}
				}
			}
		}
		fail( "Expected to find a Foreign Key mapped to column PD_ID but failed to locate it" );
	}
}
