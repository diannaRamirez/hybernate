/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.loading.multiLoad;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.hibernate.ReadOnlyMode.READ_ONLY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SessionFactory
@DomainModel(annotatedClasses = FindAllTest.Record.class)
public class FindAllTest {
	@Test void test(SessionFactoryScope scope) {
		scope.inTransaction(s-> {
			s.persist(new Record(123L,"hello earth"));
			s.persist(new Record(456L,"hello mars"));
		});
		scope.inTransaction(s-> {
			List<Record> all = s.findAll(Record.class, List.of(456L, 123L, 2L));
			assertEquals("hello mars",all.get(0).message);
			assertEquals("hello earth",all.get(1).message);
			assertNull(all.get(2));
		});
		scope.inTransaction(s-> {
			List<Record> all = s.findAll(Record.class, List.of(456L, 123L), READ_ONLY);
			assertEquals("hello mars",all.get(0).message);
			assertEquals("hello earth",all.get(1).message);
			assertTrue(s.isReadOnly(all.get(0)));
			assertTrue(s.isReadOnly(all.get(1)));
		});
		scope.inTransaction(s-> {
			Record record = s.getReference(Record.class, 456L);
			List<Record> all = s.findAll(Record.class, List.of(456L, 123L));
			assertSame(record, all.get(0));
		});
	}
	@Entity
	static class Record {
		@Id Long id;
		String message;

		Record(Long id, String message) {
			this.id = id;
			this.message = message;
		}

		Record() {
		}
	}
}
