/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.boot.models.annotations.internal;

import java.lang.annotation.Annotation;

import org.hibernate.boot.jaxb.mapping.spi.JaxbMapKeyJoinColumnImpl;
import org.hibernate.boot.models.JpaAnnotations;
import org.hibernate.boot.models.xml.internal.db.ForeignKeyProcessing;
import org.hibernate.boot.models.xml.spi.XmlDocumentContext;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.models.spi.SourceModelBuildingContext;

import org.jboss.jandex.AnnotationInstance;

import jakarta.persistence.MapKeyJoinColumn;

import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJandexValue;
import static org.hibernate.boot.models.internal.OrmAnnotationHelper.extractJdkValue;

@SuppressWarnings({ "ClassExplicitlyAnnotation", "unused" })
@jakarta.annotation.Generated("org.hibernate.orm.build.annotations.ClassGeneratorProcessor")
public class MapKeyJoinColumnJpaAnnotation implements MapKeyJoinColumn {
	private String name;
	private String referencedColumnName;
	private boolean unique;
	private boolean nullable;
	private boolean insertable;
	private boolean updatable;
	private String columnDefinition;
	private String options;
	private String table;
	private jakarta.persistence.ForeignKey foreignKey;

	/**
	 * Used in creating dynamic annotation instances (e.g. from XML)
	 */
	public MapKeyJoinColumnJpaAnnotation(SourceModelBuildingContext modelContext) {
		this.name = "";
		this.referencedColumnName = "";
		this.unique = false;
		this.nullable = false;
		this.insertable = true;
		this.updatable = true;
		this.columnDefinition = "";
		this.options = "";
		this.table = "";
		this.foreignKey = JpaAnnotations.FOREIGN_KEY.createUsage( modelContext );
	}

	/**
	 * Used in creating annotation instances from JDK variant
	 */
	public MapKeyJoinColumnJpaAnnotation(MapKeyJoinColumn annotation, SourceModelBuildingContext modelContext) {
		this.name = annotation.name();
		this.referencedColumnName = annotation.referencedColumnName();
		this.unique = annotation.unique();
		this.nullable = annotation.nullable();
		this.insertable = annotation.insertable();
		this.updatable = annotation.updatable();
		this.columnDefinition = annotation.columnDefinition();
		this.options = annotation.options();
		this.table = annotation.table();
		this.foreignKey = extractJdkValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMN, "foreignKey", modelContext );
	}

	/**
	 * Used in creating annotation instances from Jandex variant
	 */
	public MapKeyJoinColumnJpaAnnotation(AnnotationInstance annotation, SourceModelBuildingContext modelContext) {
		this.name = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMN, "name", modelContext );
		this.referencedColumnName = extractJandexValue(
				annotation,
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				"referencedColumnName",
				modelContext
		);
		this.unique = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMN, "unique", modelContext );
		this.nullable = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMN, "nullable", modelContext );
		this.insertable = extractJandexValue(
				annotation,
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				"insertable",
				modelContext
		);
		this.updatable = extractJandexValue(
				annotation,
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				"updatable",
				modelContext
		);
		this.columnDefinition = extractJandexValue(
				annotation,
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				"columnDefinition",
				modelContext
		);
		this.options = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMN, "options", modelContext );
		this.table = extractJandexValue( annotation, JpaAnnotations.MAP_KEY_JOIN_COLUMN, "table", modelContext );
		this.foreignKey = extractJandexValue(
				annotation,
				JpaAnnotations.MAP_KEY_JOIN_COLUMN,
				"foreignKey",
				modelContext
		);
	}

	@Override
	public Class<? extends Annotation> annotationType() {
		return MapKeyJoinColumn.class;
	}

	@Override
	public String name() {
		return name;
	}

	public void name(String value) {
		this.name = value;
	}


	@Override
	public String referencedColumnName() {
		return referencedColumnName;
	}

	public void referencedColumnName(String value) {
		this.referencedColumnName = value;
	}


	@Override
	public boolean unique() {
		return unique;
	}

	public void unique(boolean value) {
		this.unique = value;
	}


	@Override
	public boolean nullable() {
		return nullable;
	}

	public void nullable(boolean value) {
		this.nullable = value;
	}


	@Override
	public boolean insertable() {
		return insertable;
	}

	public void insertable(boolean value) {
		this.insertable = value;
	}


	@Override
	public boolean updatable() {
		return updatable;
	}

	public void updatable(boolean value) {
		this.updatable = value;
	}


	@Override
	public String columnDefinition() {
		return columnDefinition;
	}

	public void columnDefinition(String value) {
		this.columnDefinition = value;
	}


	@Override
	public String options() {
		return options;
	}

	public void options(String value) {
		this.options = value;
	}


	@Override
	public String table() {
		return table;
	}

	public void table(String value) {
		this.table = value;
	}


	@Override
	public jakarta.persistence.ForeignKey foreignKey() {
		return foreignKey;
	}

	public void foreignKey(jakarta.persistence.ForeignKey value) {
		this.foreignKey = value;
	}


	public void apply(JaxbMapKeyJoinColumnImpl jaxbColumn, XmlDocumentContext xmlDocumentContext) {
		if ( StringHelper.isNotEmpty( jaxbColumn.getName() ) ) {
			name( jaxbColumn.getName() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getTable() ) ) {
			table( jaxbColumn.getTable() );
		}

		if ( jaxbColumn.isNullable() != null ) {
			nullable( jaxbColumn.isNullable() );
		}

		if ( jaxbColumn.isInsertable() != null ) {
			insertable( jaxbColumn.isInsertable() );
		}
		if ( jaxbColumn.isInsertable() != null ) {
			updatable( jaxbColumn.isUpdatable() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getColumnDefinition() ) ) {
			columnDefinition( jaxbColumn.getColumnDefinition() );
		}
		if ( StringHelper.isNotEmpty( jaxbColumn.getOptions() ) ) {
			options( jaxbColumn.getOptions() );
		}

		if ( jaxbColumn.isUnique() != null ) {
			unique( jaxbColumn.isUnique() );
		}

		if ( StringHelper.isNotEmpty( jaxbColumn.getReferencedColumnName() ) ) {
			referencedColumnName( jaxbColumn.getReferencedColumnName() );
		}

		if ( jaxbColumn.getForeignKey() != null ) {
			foreignKey( ForeignKeyProcessing.createNestedForeignKeyAnnotation(
					jaxbColumn.getForeignKey(),
					xmlDocumentContext
			) );
		}
	}
}