/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.jpa.criteria.basic;

import java.util.List;

import org.hibernate.testing.orm.junit.EntityManagerFactoryScope;
import org.hibernate.testing.orm.junit.JiraKey;
import org.hibernate.testing.orm.junit.Jpa;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * @author Jan Schatteman
 */
@Jpa(
		annotatedClasses = {Wall.class},
		jpaComplianceEnabled = false
)
@JiraKey( value = "HHH-17493" )
public class NegatedPredicateTest {

	@BeforeEach
	public void setup(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> {
					Wall wall1 = new Wall();
					wall1.setColor( "yellow" );
					Wall wall2 = new Wall();
					wall2.setColor( "green" );
					Wall wall3 = new Wall();
					wall3.setColor( "red" );
					entityManager.persist( wall1 );
					entityManager.persist( wall2 );
					entityManager.persist( wall3 );
				}
		);
	}

	@AfterEach
	public void tearDown(EntityManagerFactoryScope scope) {
		scope.inTransaction(
				entityManager -> entityManager.createQuery( "delete from Wall" ).executeUpdate()
		);
	}

	@Test
	public void testNegatedPredicate(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
					Root<Wall> root = query.from( Wall.class );
					query.select( root ).where(
						cb.not(
								cb.or(
									cb.equal(root.get( "color" ), "yellow"),
									cb.equal(root.get( "color" ), "red")
								)
						)
					);
					Wall result = entityManager.createQuery( query ).getSingleResult();
					assertNotNull( result );
					assertEquals("green", result.getColor());
				}
		);
	}

	@Test
	public void testDoubleNegatedPredicate(EntityManagerFactoryScope scope) {
		scope.inEntityManager(
				entityManager -> {
					CriteriaBuilder cb = entityManager.getCriteriaBuilder();
					CriteriaQuery<Wall> query = cb.createQuery( Wall.class );
					Root<Wall> root = query.from( Wall.class );
					query.select( root ).where(
							cb.not(
									cb.not(
											cb.or(
													cb.equal(root.get( "color" ), "yellow"),
													cb.equal(root.get( "color" ), "red")
											)
									)
							)
					);
					query.orderBy( cb.asc(root.get("id")) );
					List<Wall> result = entityManager.createQuery( query ).getResultList();
					assertEquals( 2, result.size() );
					assertEquals("yellow", result.get(0).getColor());
					assertEquals("red", result.get(1).getColor());
				}
		);
	}
}
