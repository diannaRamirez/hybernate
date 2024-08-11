/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.test.mapping.type.java;

import java.sql.Time;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.hibernate.HibernateException;
import org.hibernate.dialect.Dialect;
import org.hibernate.dialect.H2Dialect;
import org.hibernate.engine.jdbc.LobCreator;
import org.hibernate.engine.jdbc.NonContextualLobCreator;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.descriptor.WrapperOptions;
import org.hibernate.type.descriptor.java.CalendarDateJavaType;
import org.hibernate.type.descriptor.java.CalendarJavaType;
import org.hibernate.type.descriptor.java.CalendarTimeJavaType;
import org.hibernate.type.descriptor.java.DateJavaType;
import org.hibernate.type.descriptor.java.InstantJavaType;
import org.hibernate.type.descriptor.java.JdbcDateJavaType;
import org.hibernate.type.descriptor.java.JdbcTimeJavaType;
import org.hibernate.type.descriptor.java.JdbcTimestampJavaType;
import org.hibernate.type.descriptor.java.LocalDateJavaType;
import org.hibernate.type.descriptor.java.LocalDateTimeJavaType;
import org.hibernate.type.descriptor.java.LocalTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetDateTimeJavaType;
import org.hibernate.type.descriptor.java.OffsetTimeJavaType;
import org.hibernate.type.descriptor.java.ZonedDateTimeJavaType;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class DateSubclassesUnwrapTest {

	private static final WrapperOptions wrapperOptions = new WrapperOptions() {
		@Override
		public SharedSessionContractImplementor getSession() {
			return null;
		}

		@Override
		public SessionFactoryImplementor getSessionFactory() {
			return null;
		}

		public boolean useStreamForLobBinding() {
			return false;
		}

		@Override
		public int getPreferredSqlTypeCodeForBoolean() {
			return 0;
		}

		public LobCreator getLobCreator() {
			return NonContextualLobCreator.INSTANCE;
		}

		@Override
		public TimeZone getJdbcTimeZone() {
			return null;
		}

		private final Dialect dialect = new H2Dialect() {
			@Override
			public boolean useConnectionToCreateLob() {
				return false;
			}
		};

		@Override
		public Dialect getDialect() {
			return dialect;
		}
	};

	@Test
	void testJdbcTimestampJavaType() {
		final JdbcTimestampJavaType javaType = JdbcTimestampJavaType.INSTANCE;
		final Date date = new Date();

		assertInstanceOf( Timestamp.class, javaType.unwrap( date, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( date, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( date, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( date, Date.class, wrapperOptions ) );
	}

	@Test
	void testJdbcDateJavaType() {
		final JdbcDateJavaType javaType = JdbcDateJavaType.INSTANCE;
		final Date date = new Date();

		assertInstanceOf( Timestamp.class, javaType.unwrap( date, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( date, java.sql.Date.class, wrapperOptions ) );
		assertThrows( IllegalArgumentException.class, () -> javaType.unwrap( date, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( date, Date.class, wrapperOptions ) );
	}

	@Test
	void testJdbcTimeJavaType() {
		final JdbcTimeJavaType javaType = JdbcTimeJavaType.INSTANCE;
		final Date date = new Date();

		assertInstanceOf( Timestamp.class, javaType.unwrap( date, Timestamp.class, wrapperOptions ) );
		Assertions.assertThrows(
				IllegalArgumentException.class,
				() -> javaType.unwrap( date, java.sql.Date.class, wrapperOptions )
		);
		assertInstanceOf( Time.class, javaType.unwrap( date, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( date, Date.class, wrapperOptions ) );
	}

	@Test
	void testDateJavaType() {
		final DateJavaType javaType = DateJavaType.INSTANCE;
		final Date date = new Date();

		assertInstanceOf( Timestamp.class, javaType.unwrap( date, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( date, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( date, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( date, Date.class, wrapperOptions ) );
	}

	@Test
	void testInstantJavaType() {
		final InstantJavaType javaType = InstantJavaType.INSTANCE;
		final Instant instant = Instant.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( instant, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( instant, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( instant, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( instant, Date.class, wrapperOptions ) );
	}

	@Test
	void testLocalDateJavaType() {
		final LocalDateJavaType javaType = LocalDateJavaType.INSTANCE;
		final LocalDate date = LocalDate.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( date, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( date, java.sql.Date.class, wrapperOptions ) );
		assertThrows( HibernateException.class, () -> javaType.unwrap( date, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( date, Date.class, wrapperOptions ) );
	}

	@Test
	void testLocalDateTimeJavaType() {
		final LocalDateTimeJavaType javaType = LocalDateTimeJavaType.INSTANCE;
		final LocalDateTime dateTime = LocalDateTime.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( dateTime, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( dateTime, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( dateTime, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( dateTime, Date.class, wrapperOptions ) );
	}

	@Test
	void testLocalTimeJavaType() {
		final LocalTimeJavaType javaType = LocalTimeJavaType.INSTANCE;
		final LocalTime time = LocalTime.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( time, Timestamp.class, wrapperOptions ) );
		assertThrows( HibernateException.class, () -> javaType.unwrap( time, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( time, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( time, Date.class, wrapperOptions ) );
	}

	@Test
	void testOffsetDateTimeJavaType() {
		final OffsetDateTimeJavaType javaType = OffsetDateTimeJavaType.INSTANCE;
		final OffsetDateTime dateTime = OffsetDateTime.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( dateTime, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( dateTime, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( dateTime, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( dateTime, Date.class, wrapperOptions ) );
	}

	@Test
	void testOffsetTimeJavaType() {
		final OffsetTimeJavaType javaType = OffsetTimeJavaType.INSTANCE;
		final OffsetTime time = OffsetTime.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( time, Timestamp.class, wrapperOptions ) );
		assertThrows( IllegalArgumentException.class,
					  () -> javaType.unwrap( time, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( time, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( time, Date.class, wrapperOptions ) );
	}

	@Test
	void testZonedDateTimeJavaType() {
		final ZonedDateTimeJavaType javaType = ZonedDateTimeJavaType.INSTANCE;
		final ZonedDateTime dateTime = ZonedDateTime.now();

		assertInstanceOf( Timestamp.class, javaType.unwrap( dateTime, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( dateTime, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( dateTime, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( dateTime, Date.class, wrapperOptions ) );
	}

	@Test
	void testCalendarDateJavaType() {
		final CalendarDateJavaType javaType = CalendarDateJavaType.INSTANCE;
		final Calendar calendar = Calendar.getInstance();

		assertInstanceOf( Timestamp.class, javaType.unwrap( calendar, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( calendar, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( calendar, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( calendar, Date.class, wrapperOptions ) );
	}

	@Test
	void testCalendarJavaType() {
		final CalendarJavaType javaType = CalendarJavaType.INSTANCE;
		final Calendar calendar = Calendar.getInstance();

		assertInstanceOf( Timestamp.class, javaType.unwrap( calendar, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( calendar, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( calendar, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( calendar, Date.class, wrapperOptions ) );
	}

	@Test
	void testCalendarTimeJavaType() {
		final CalendarTimeJavaType javaType = CalendarTimeJavaType.INSTANCE;
		final Calendar calendar = Calendar.getInstance();

		assertInstanceOf( Timestamp.class, javaType.unwrap( calendar, Timestamp.class, wrapperOptions ) );
		assertInstanceOf( java.sql.Date.class, javaType.unwrap( calendar, java.sql.Date.class, wrapperOptions ) );
		assertInstanceOf( Time.class, javaType.unwrap( calendar, Time.class, wrapperOptions ) );
		assertInstanceOf( Date.class, javaType.unwrap( calendar, Date.class, wrapperOptions ) );
	}
}
