/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.query.sqm.produce.internal;

import java.util.ArrayList;
import java.util.List;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.internal.CoreLogging;
import org.hibernate.internal.CoreMessageLogger;

/**
 * Delegate for handling function "templates".
 *
 * @author Steve Ebersole
 */
public class PatternRenderer {
	private static final CoreMessageLogger LOG = CoreLogging.messageLogger( PatternRenderer.class );

	private final String pattern;
	private final String[] chunks;
	private final int[] paramIndexes;

	/**
	 * Constructs a template renderer
	 *
	 * @param pattern The template
	 */
	public PatternRenderer(String pattern) {
		this.pattern = pattern;

		final List<String> chunkList = new ArrayList<>();
		final List<Integer> paramList = new ArrayList<>();
		final StringBuilder chunk = new StringBuilder( 10 );
		final StringBuilder index = new StringBuilder( 2 );

		int i = 0;
		final int len = pattern.length();
		while ( i < len ) {
			char c = pattern.charAt( i );
			if ( c == '?' ) {
				chunkList.add( chunk.toString() );
				chunk.delete( 0, chunk.length() );

				while ( ++i < pattern.length() ) {
					c = pattern.charAt( i );
					if ( Character.isDigit( c ) ) {
						index.append( c );
					}
					else {
						chunk.append( c );
						break;
					}
				}

				paramList.add( Integer.valueOf( index.toString() ) );
				index.delete( 0, index.length() );
			}
			else {
				chunk.append( c );
			}
			i++;
		}

		if ( chunk.length() > 0 ) {
			chunkList.add( chunk.toString() );
		}

		chunks = chunkList.toArray( new String[chunkList.size()] );
		paramIndexes = new int[paramList.size()];
		for ( i = 0; i < paramIndexes.length; ++i ) {
			paramIndexes[i] = paramList.get( i );
		}
	}

	public String getPattern() {
		return pattern;
	}

	public int getAnticipatedNumberOfArguments() {
		return paramIndexes.length;
	}

	/**
	 * The rendering code.
	 *
	 * @param args The arguments to inject into the template
	 * @param factory The SessionFactory
	 *
	 * @return The rendered template with replacements
	 */
	@SuppressWarnings({ "UnusedDeclaration" })
	public String render(List args, SessionFactoryImplementor factory) {
		final int numberOfArguments = args.size();
		if ( getAnticipatedNumberOfArguments() > 0 && numberOfArguments != getAnticipatedNumberOfArguments() ) {
			LOG.missingArguments( getAnticipatedNumberOfArguments(), numberOfArguments );
		}
		final StringBuilder buf = new StringBuilder();
		for ( int i = 0; i < chunks.length; ++i ) {
			if ( i < paramIndexes.length ) {
				final int index = paramIndexes[i] - 1;
				final Object arg =  index < numberOfArguments ? args.get( index ) : null;
				if ( arg != null ) {
					buf.append( chunks[i] ).append( arg );
				}
			}
			else {
				buf.append( chunks[i] );
			}
		}
		return buf.toString();
	}
}
