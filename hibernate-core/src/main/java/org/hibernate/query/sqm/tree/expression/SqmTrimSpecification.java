/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.tree.expression;

import org.hibernate.query.sqm.TrimSpec;
import org.hibernate.query.sqm.NodeBuilder;
import org.hibernate.query.sqm.SqmExpressible;
import org.hibernate.query.sqm.SemanticQueryWalker;
import org.hibernate.query.sqm.tree.AbstractSqmNode;
import org.hibernate.query.sqm.tree.SqmCopyContext;
import org.hibernate.query.sqm.tree.SqmTypedNode;

/**
 * Needed to pass TrimSpecification as an SqmExpression when we call out to
 * SqmFunctionTemplates handling TRIM calls as a function argument.
 *
 * @author Steve Ebersole
 */
public class SqmTrimSpecification extends AbstractSqmNode implements SqmTypedNode<Void> {
	private final TrimSpec specification;

	public SqmTrimSpecification(TrimSpec specification, NodeBuilder nodeBuilder) {
		super( nodeBuilder );
		this.specification = specification;
	}

	@Override
	public SqmTrimSpecification copy(SqmCopyContext context) {
		return this;
	}

	public TrimSpec getSpecification() {
		return specification;
	}

	@Override
	public <T> T accept(SemanticQueryWalker<T> walker) {
		return walker.visitTrimSpecification( this );
	}

	@Override
	public String asLoggableText() {
		return specification.name();
	}

	@Override
	public SqmExpressible<Void> getNodeType() {
		return null;
	}

	@Override
	public void appendHqlString(StringBuilder sb) {
		sb.append( specification );
	}
}
