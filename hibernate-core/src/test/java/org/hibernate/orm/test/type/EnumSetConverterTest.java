/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.orm.test.type;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.hibernate.cfg.AvailableSettings;
import org.hibernate.dialect.HANADialect;
import org.hibernate.dialect.HSQLDialect;
import org.hibernate.dialect.OracleDialect;
import org.hibernate.dialect.SybaseASEDialect;
import org.hibernate.engine.spi.SessionImplementor;
import org.hibernate.query.BindableType;
import org.hibernate.query.spi.QueryImplementor;

import org.hibernate.testing.SkipForDialect;
import org.hibernate.testing.junit4.BaseNonConfigCoreFunctionalTestCase;
import org.junit.Test;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.persistence.TypedQuery;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * @author Christian Beikov
 */
@SkipForDialect(value = SybaseASEDialect.class, comment = "Sybase or the driver are trimming trailing zeros in byte arrays")
public class EnumSetConverterTest extends BaseNonConfigCoreFunctionalTestCase {

	private BindableType<Set<MySpecialEnum>> enumSetType;

	@Override
	protected Class[] getAnnotatedClasses() {
		return new Class[]{ TableWithEnumSetConverter.class };
	}

	@Override
	protected void addSettings(Map<String, Object> settings) {
		// Make sure this stuff runs on a dedicated connection pool,
		// otherwise we might run into ORA-21700: object does not exist or is marked for delete
		// because the JDBC connection or database session caches something that should have been invalidated
		settings.put( AvailableSettings.CONNECTION_PROVIDER, "" );
	}

	public void startUp() {
		super.startUp();
		inTransaction( em -> {
			//noinspection unchecked
			enumSetType = (BindableType<Set<MySpecialEnum>>) em.unwrap( SessionImplementor.class )
					.getFactory()
					.getRuntimeMetamodels()
					.getMappingMetamodel()
					.getEntityDescriptor( TableWithEnumSetConverter.class )
					.getPropertyType( "theSet" );
			em.persist( new TableWithEnumSetConverter( 1L, new HashSet<>() ) );
			em.persist( new TableWithEnumSetConverter( 2L, EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );
			em.persist( new TableWithEnumSetConverter( 3L, null ) );

			QueryImplementor q;
			q = em.createNamedQuery( "TableWithEnumSetConverter.Native.insert" );
			q.setParameter( "id", 4L );
			q.setParameter( "data", EnumSet.of( MySpecialEnum.VALUE2, MySpecialEnum.VALUE1, MySpecialEnum.VALUE3 ), enumSetType );
			q.executeUpdate();

			q = em.createNativeQuery( "INSERT INTO table_with_enum_set_convert(id, the_set) VALUES ( :id , :data )" );
			q.setParameter( "id", 5L );
			q.setParameter( "data", EnumSet.of( MySpecialEnum.VALUE2, MySpecialEnum.VALUE1, MySpecialEnum.VALUE3 ), enumSetType );
			q.executeUpdate();
		} );
	}

	@Test
	public void testById() {
		inSession( em -> {
			TableWithEnumSetConverter tableRecord;
			tableRecord = em.find( TableWithEnumSetConverter.class, 1L );
			assertThat( tableRecord.getTheSet(), is( new HashSet<>() ) );

			tableRecord = em.find( TableWithEnumSetConverter.class, 2L );
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );

			tableRecord = em.find( TableWithEnumSetConverter.class, 3L );
			assertThat( tableRecord.getTheSet(), is( (Object) null ) );
		} );
	}

	@Test
	public void testQueryById() {
		inSession( em -> {
			TypedQuery<TableWithEnumSetConverter> tq = em.createNamedQuery( "TableWithEnumSetConverter.JPQL.getById", TableWithEnumSetConverter.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HANADialect.class, comment = "For some reason, HANA can't intersect VARBINARY values, but funnily can do a union...")
	public void testQuery() {
		inSession( em -> {
			TypedQuery<TableWithEnumSetConverter> tq = em.createNamedQuery( "TableWithEnumSetConverter.JPQL.getByData", TableWithEnumSetConverter.class );
			tq.setParameter( "data", new HashSet<>() );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 1L ) );
		} );
	}

	@Test
	public void testNativeQueryById() {
		inSession( em -> {
			TypedQuery<TableWithEnumSetConverter> tq = em.createNamedQuery( "TableWithEnumSetConverter.Native.getById", TableWithEnumSetConverter.class );
			tq.setParameter( "id", 2L );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getTheSet(), is( EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ) ) );
		} );
	}

	@Test
	@SkipForDialect( value = HSQLDialect.class, comment = "HSQL does not like plain parameters in the distinct from predicate")
	@SkipForDialect( value = OracleDialect.class, comment = "Oracle requires a special function to compare XML")
	public void testNativeQuery() {
		inSession( em -> {
			final String op = em.getJdbcServices().getDialect().supportsDistinctFromPredicate() ? "IS NOT DISTINCT FROM" : "=";
			QueryImplementor<TableWithEnumSetConverter> tq = em.createNativeQuery(
					"SELECT * FROM table_with_enum_set_convert t WHERE the_set " + op + " :data",
					TableWithEnumSetConverter.class
			);
			tq.setParameter( "data", EnumSet.of( MySpecialEnum.VALUE1, MySpecialEnum.VALUE2 ), enumSetType );
			TableWithEnumSetConverter tableRecord = tq.getSingleResult();
			assertThat( tableRecord.getId(), is( 2L ) );
		} );
	}

	@Entity( name = "TableWithEnumSetConverter" )
	@Table( name = "table_with_enum_set_convert" )
	@NamedQueries( {
		@NamedQuery( name = "TableWithEnumSetConverter.JPQL.getById",
				query = "SELECT t FROM TableWithEnumSetConverter t WHERE id = :id" ),
		@NamedQuery( name = "TableWithEnumSetConverter.JPQL.getByData",
				query = "SELECT t FROM TableWithEnumSetConverter t WHERE theSet IS NOT DISTINCT FROM :data" ), } )
	@NamedNativeQueries( {
		@NamedNativeQuery( name = "TableWithEnumSetConverter.Native.getById",
				query = "SELECT * FROM table_with_enum_set_convert t WHERE id = :id",
				resultClass = TableWithEnumSetConverter.class ),
		@NamedNativeQuery( name = "TableWithEnumSetConverter.Native.insert",
				query = "INSERT INTO table_with_enum_set_convert(id, the_set) VALUES ( :id , :data )" )
	} )
	public static class TableWithEnumSetConverter {

		@Id
		private Long id;

		@Convert(converter = MyEnumConverter.class)
		@Column( name = "the_set" )
		private Set<MySpecialEnum> theSet;

		public TableWithEnumSetConverter() {
		}

		public TableWithEnumSetConverter(Long id, Set<MySpecialEnum> theSet) {
			this.id = id;
			this.theSet = theSet;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Set<MySpecialEnum> getTheSet() {
			return theSet;
		}

		public void setTheSet(Set<MySpecialEnum> theSet) {
			this.theSet = theSet;
		}
	}

	public enum MySpecialEnum {
		VALUE1, VALUE2, VALUE3
	}
	
	public static class MyEnumConverter implements AttributeConverter<MySpecialEnum, String> {
		@Override
		public String convertToDatabaseColumn(MySpecialEnum attribute) {
			return attribute == null ? null : attribute.name();
		}

		@Override
		public MySpecialEnum convertToEntityAttribute(String dbData) {
			return dbData == null ? null : MySpecialEnum.valueOf( dbData );
		}
	}
}
