package org.hibernate.userguide.mapping.basic;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.SybaseDialect;
import org.hibernate.testing.orm.junit.DomainModel;
import org.hibernate.testing.orm.junit.ServiceRegistry;
import org.hibernate.testing.orm.junit.SessionFactory;
import org.hibernate.testing.orm.junit.SessionFactoryScope;
import org.hibernate.testing.orm.junit.Setting;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DomainModel(annotatedClasses = NormalizedZonedTest.Zoned.class)
@SessionFactory
@ServiceRegistry(settings = @Setting(name = AvailableSettings.TIMEZONE_DEFAULT_STORAGE, value = "NORMALIZE_UTC"))
public class NormalizedZonedTest {

	@Test void test(SessionFactoryScope scope) {
		ZonedDateTime nowZoned = ZonedDateTime.now().withZoneSameInstant( ZoneId.of("CET") );
		OffsetDateTime nowOffset = OffsetDateTime.now().withOffsetSameInstant( ZoneOffset.ofHours(3) );
		long id = scope.fromTransaction( s-> {
			Zoned z = new Zoned();
			z.zonedDateTime = nowZoned;
			z.offsetDateTime = nowOffset;
			s.persist(z);
			return z.id;
		});
		scope.inSession( s-> {
			Zoned z = s.find(Zoned.class, id);
			if ( scope.getSessionFactory().getJdbcServices().getDialect() instanceof SybaseDialect) {
				// Sybase with jTDS driver has 1/300th sec precision
				assertEquals( nowZoned.toInstant().toEpochMilli()/10, z.zonedDateTime.toInstant().toEpochMilli()/10 );
				assertEquals( nowOffset.toInstant().toEpochMilli()/10, z.offsetDateTime.toInstant().toEpochMilli()/10 );
			}
			else {
				assertEquals( nowZoned.toInstant(), z.zonedDateTime.toInstant() );
				assertEquals( nowOffset.toInstant(), z.offsetDateTime.toInstant() );
			}
			assertEquals( ZoneId.of("Z"), z.zonedDateTime.getZone() );
			assertEquals( ZoneOffset.ofHours(0), z.offsetDateTime.getOffset() );
		});
	}

	@Entity
	public static class Zoned {
		@Id
		@GeneratedValue Long id;
		ZonedDateTime zonedDateTime;
		OffsetDateTime offsetDateTime;
	}
}
