/*
 * SPDX-License-Identifier: LGPL-2.1-or-later
 * Copyright Red Hat Inc. and Hibernate Authors
 */
package org.hibernate.dialect.aggregate;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hibernate.dialect.Dialect;
import org.hibernate.internal.util.StringHelper;
import org.hibernate.mapping.AggregateColumn;
import org.hibernate.mapping.Column;
import org.hibernate.metamodel.mapping.EmbeddableMappingType;
import org.hibernate.metamodel.mapping.JdbcMapping;
import org.hibernate.metamodel.mapping.SelectableMapping;
import org.hibernate.metamodel.mapping.SelectablePath;
import org.hibernate.sql.ast.SqlAstNodeRenderingMode;
import org.hibernate.sql.ast.SqlAstTranslator;
import org.hibernate.sql.ast.spi.SqlAppender;
import org.hibernate.type.BasicPluralType;
import org.hibernate.type.BasicType;
import org.hibernate.type.descriptor.jdbc.AggregateJdbcType;
import org.hibernate.type.spi.TypeConfiguration;

import org.checkerframework.checker.nullness.qual.Nullable;

import static org.hibernate.type.SqlTypes.ARRAY;
import static org.hibernate.type.SqlTypes.JSON;
import static org.hibernate.type.SqlTypes.JSON_ARRAY;

public class H2AggregateSupport extends AggregateSupportImpl {

	private static final AggregateSupport INSTANCE = new H2AggregateSupport();

	public static @Nullable AggregateSupport valueOf(Dialect dialect) {
		return dialect.getVersion().isSameOrAfter( 2, 2, 220 )
				? H2AggregateSupport.INSTANCE
				: null;
	}

	@Override
	public String aggregateComponentCustomReadExpression(
			String template,
			String placeholder,
			String aggregateParentReadExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		final int sqlTypeCode = ( (BasicType<?>) aggregateColumn.getValue().getType() ).getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case JSON_ARRAY:
			case JSON:
				switch ( ( (BasicType<?>) column.getValue().getType() ).getJdbcType().getDefaultSqlTypeCode() ) {
					case JSON:
					case JSON_ARRAY:
						return template.replace(
								placeholder,
								"(" + aggregateParentReadExpression + ").\"" + columnExpression + "\""
						);
//					case BINARY:
//					case VARBINARY:
//					case LONG32VARBINARY:
//						// We encode binary data as hex, so we have to decode here
//						return template.replace(
//								placeholder,
//								"decode(" + aggregateParentReadExpression + "->>'" + columnExpression + "','hex')"
//						);
					case ARRAY:
						final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) column.getValue().getType();
						switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
//							case BOOLEAN:
//							case TINYINT:
//							case SMALLINT:
//							case INTEGER:
//							case BIGINT:
//							case FLOAT:
//							case DOUBLE:
//								// For types that are natively supported in jsonb we can use jsonb_array_elements,
//								// but note that we can't use that for string types,
//								// because casting a jsonb[] to text[] will not omit the quotes of the jsonb text values
//								return template.replace(
//										placeholder,
//										"cast(array(select jsonb_array_elements(" + aggregateParentReadExpression + "->'" + columnExpression + "')) as " + column.getTypeName() + ')'
//								);
//							case BINARY:
//							case VARBINARY:
//							case LONG32VARBINARY:
//								// We encode binary data as hex, so we have to decode here
//								return template.replace(
//										placeholder,
//										"array(select decode(jsonb_array_elements_text(" + aggregateParentReadExpression + "->'" + columnExpression + "'),'hex'))"
//								);
							default:
								return template.replace(
										placeholder,
										"cast((" + aggregateParentReadExpression + ").\"" + columnExpression + "\" as " + column.getTypeName() + ')'
								);
						}
					default:
						return template.replace(
								placeholder,
								"cast(stringdecode(btrim(nullif(cast((" + aggregateParentReadExpression + ").\"" + columnExpression + "\" as varchar),'null'),'\"')) as " + column.getTypeName() + ')'
						);
				}
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + sqlTypeCode );
	}

	private static String jsonCustomWriteExpression(String customWriteExpression, JdbcMapping jdbcMapping) {
//		final int sqlTypeCode = jdbcMapping.getJdbcType().getDefaultSqlTypeCode();
//		switch ( sqlTypeCode ) {
//			case BINARY:
//			case VARBINARY:
//			case LONG32VARBINARY:
//				// We encode binary data as hex
//				return "to_jsonb(encode(" + customWriteExpression + ",'hex'))";
//			case ARRAY:
//				final BasicPluralType<?, ?> pluralType = (BasicPluralType<?, ?>) jdbcMapping;
//				switch ( pluralType.getElementType().getJdbcType().getDefaultSqlTypeCode() ) {
//					case BINARY:
//					case VARBINARY:
//					case LONG32VARBINARY:
//						// We encode binary data as hex
//						return "to_jsonb(array(select encode(unnest(" + customWriteExpression + "),'hex')))";
//					default:
//						return "to_jsonb(" + customWriteExpression + ")";
//				}
//			default:
//				return "to_jsonb(" + customWriteExpression + ")";
//		}
		return customWriteExpression;
	}

	@Override
	public String aggregateComponentAssignmentExpression(
			String aggregateParentAssignmentExpression,
			String columnExpression,
			AggregateColumn aggregateColumn,
			Column column) {
		final int sqlTypeCode = ( (BasicType<?>) aggregateColumn.getValue().getType() ).getJdbcType().getDefaultSqlTypeCode();
		switch ( sqlTypeCode ) {
			case JSON:
			case JSON_ARRAY:
				// For JSON we always have to replace the whole object
				return aggregateParentAssignmentExpression;
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + sqlTypeCode );
	}

	@Override
	public boolean requiresAggregateCustomWriteExpressionRenderer(int aggregateSqlTypeCode) {
		switch ( aggregateSqlTypeCode ) {
			case JSON:
				return true;
		}
		return false;
	}

	@Override
	public WriteExpressionRenderer aggregateCustomWriteExpressionRenderer(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columnsToUpdate,
			TypeConfiguration typeConfiguration) {
		final int aggregateSqlTypeCode = aggregateColumn.getJdbcMapping().getJdbcType().getDefaultSqlTypeCode();
		switch ( aggregateSqlTypeCode ) {
			case JSON:
				return jsonAggregateColumnWriter( aggregateColumn, columnsToUpdate );
		}
		throw new IllegalArgumentException( "Unsupported aggregate SQL type: " + aggregateSqlTypeCode );
	}

	private WriteExpressionRenderer jsonAggregateColumnWriter(
			SelectableMapping aggregateColumn,
			SelectableMapping[] columns) {
		return new RootJsonWriteExpression( aggregateColumn, columns );
	}

	interface JsonWriteExpression {
		void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression);
	}
	private static class AggregateJsonWriteExpression implements JsonWriteExpression {
		private final LinkedHashMap<String, JsonWriteExpression> subExpressions = new LinkedHashMap<>();

		protected void initializeSubExpressions(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			final AggregateJdbcType aggregateJdbcType = (AggregateJdbcType) aggregateColumn.getJdbcMapping().getJdbcType();
			final EmbeddableMappingType embeddableMappingType = aggregateJdbcType.getEmbeddableMappingType();
			final int jdbcValueCount = embeddableMappingType.getJdbcValueCount();
			final List<SelectableMapping> columnsToUpdate = Arrays.asList( columns );
			for ( int i = 0; i < jdbcValueCount; i++ ) {
				final SelectableMapping selectableMapping = embeddableMappingType.getJdbcValueSelectable( i );
				final SelectablePath selectablePath = selectableMapping.getSelectablePath();
				final SelectablePath[] parts = selectablePath.getParts();
				AggregateJsonWriteExpression currentAggregate = this;
				for ( int j = 1; j < parts.length - 1; j++ ) {
					currentAggregate = (AggregateJsonWriteExpression) currentAggregate.subExpressions.computeIfAbsent(
							parts[j].getSelectableName(),
							k -> new AggregateJsonWriteExpression()
					);
				}
				if ( columnsToUpdate.contains( selectableMapping ) ) {
					currentAggregate.subExpressions.put(
							parts[parts.length - 1].getSelectableName(),
							new BasicJsonWriteExpression(
									selectableMapping,
									jsonCustomWriteExpression(
											selectableMapping.getWriteExpression(),
											selectableMapping.getJdbcMapping()
									)
							)
					);
				}
				else {
					currentAggregate.subExpressions.put(
							parts[parts.length - 1].getSelectableName(),
							new PassThroughExpression( selectableMapping )
					);
				}
			}
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( "json_object" );
			char separator = '(';
			for ( Map.Entry<String, JsonWriteExpression> entry : subExpressions.entrySet() ) {
				final String column = entry.getKey();
				final JsonWriteExpression value = entry.getValue();
				final String subPath = path + ".\"" + column + "\"";
				sb.append( separator );
				if ( value instanceof AggregateJsonWriteExpression ) {
					sb.append( '\'' );
					sb.append( column );
					sb.append( "',coalesce(" );
					sb.append( subPath );
					sb.append( ",'{}')" );
					value.append( sb, subPath, translator, expression );
				}
				else {
					value.append( sb, subPath, translator, expression );
				}
				separator = ',';
			}
			sb.append( ')' );
		}
	}

	private static class RootJsonWriteExpression extends AggregateJsonWriteExpression
			implements WriteExpressionRenderer {
		private final boolean nullable;
		private final String path;

		RootJsonWriteExpression(SelectableMapping aggregateColumn, SelectableMapping[] columns) {
			this.nullable = aggregateColumn.isNullable();
			this.path = aggregateColumn.getSelectionExpression();
			initializeSubExpressions( aggregateColumn, columns );
		}

		@Override
		public void render(
				SqlAppender sqlAppender,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression aggregateColumnWriteExpression,
				String qualifier) {
			final String basePath;
			if ( qualifier == null || qualifier.isBlank() ) {
				basePath = path;
			}
			else {
				basePath = qualifier + "." + path;
			}
			sqlAppender.append( "(select " );
			append( sqlAppender, "(t.v)", translator, aggregateColumnWriteExpression );
			sqlAppender.append( " from (values( " );
			if ( nullable ) {
				sqlAppender.append( "coalesce(" );
			}
			sqlAppender.append( basePath );
			if ( nullable ) {
				sqlAppender.append( basePath );
				sqlAppender.append( ",json_object())" );
			}
			sqlAppender.append( ")) t(v)) " );
		}
	}
	private static class BasicJsonWriteExpression implements JsonWriteExpression {

		private final SelectableMapping selectableMapping;
		private final String customWriteExpressionStart;
		private final String customWriteExpressionEnd;

		BasicJsonWriteExpression(SelectableMapping selectableMapping, String customWriteExpression) {
			this.selectableMapping = selectableMapping;
			if ( customWriteExpression.equals( "?" ) ) {
				this.customWriteExpressionStart = "";
				this.customWriteExpressionEnd = "";
			}
			else {
				final String[] parts = StringHelper.split( "?", customWriteExpression );
				assert parts.length == 2;
				this.customWriteExpressionStart = parts[0];
				this.customWriteExpressionEnd = parts[1];
			}
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( '\'' );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( "'," );
			sb.append( customWriteExpressionStart );
			// We use NO_UNTYPED here so that expressions which require type inference are casted explicitly,
			// since we don't know how the custom write expression looks like where this is embedded,
			// so we have to be pessimistic and avoid ambiguities
			translator.render( expression.getValueExpression( selectableMapping ), SqlAstNodeRenderingMode.NO_UNTYPED );
			sb.append( customWriteExpressionEnd );
		}
	}
	private static class PassThroughExpression implements JsonWriteExpression {

		private final SelectableMapping selectableMapping;

		PassThroughExpression(SelectableMapping selectableMapping) {
			this.selectableMapping = selectableMapping;
		}

		@Override
		public void append(
				SqlAppender sb,
				String path,
				SqlAstTranslator<?> translator,
				AggregateColumnWriteExpression expression) {
			sb.append( '\'' );
			sb.append( selectableMapping.getSelectableName() );
			sb.append( "'," );
			sb.append( path );
		}
	}

}
