/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.processor.test.hrPanache;

import java.util.List;

import org.hibernate.annotations.NaturalId;
import org.hibernate.annotations.processing.Find;
import org.hibernate.annotations.processing.HQL;

import io.quarkus.hibernate.reactive.panache.PanacheEntity;
import io.smallrye.mutiny.Uni;
import jakarta.persistence.Entity;

@Entity
public class PanacheBook extends PanacheEntity {
	public @NaturalId String isbn;
	public @NaturalId String title;
	public @NaturalId String author;
	public String text;
	public int pages;

	@Find
	public static native Uni<List<PanacheBook>> findBook(String isbn);

	@HQL("WHERE isbn = :isbn")
	public static native Uni<List<PanacheBook>> hqlBook(String isbn);
}
