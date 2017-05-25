/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.dialect.function;
import java.util.List;

import org.hibernate.QueryException;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.query.sqm.produce.spi.SqmFunctionTemplate;
import org.hibernate.type.spi.StandardSpiBasicTypes;
import org.hibernate.type.Type;

/**
 * Defines the basic template support for <tt>TRIM</tt> functions
 *
 * @author Steve Ebersole
 */
public abstract class TrimFunctionTemplate implements SqmFunctionTemplate {
	@Override
	public boolean hasArguments() {
		return true;
	}

	@Override
	public boolean hasParenthesesIfNoArguments() {
		return false;
	}

	@Override
	public Type getReturnType(Type firstArgument) throws QueryException {
		return StandardSpiBasicTypes.STRING;
	}

	@Override
	public String render(Type firstArgument, List args, SessionFactoryImplementor factory) throws QueryException {
		final Options options = new Options();
		final String trimSource;

		if ( args.size() == 1 ) {
			// we have the form: trim(trimSource)
			trimSource = (String) args.get( 0 );
		}
		else if ( "from".equalsIgnoreCase( (String) args.get( 0 ) ) ) {
			// we have the form: trim(from trimSource).
			//      This is functionally equivalent to trim(trimSource)
			trimSource = (String) args.get( 1 );
		}
		else {
			// otherwise, a trim-specification and/or a trim-character
			// have been specified;  we need to decide which options
			// are present and "do the right thing"
			//
			// potentialTrimCharacterArgIndex = 1 assumes that a
			// trim-specification has been specified.  we handle the
			// exception to that explicitly
			int potentialTrimCharacterArgIndex = 1;
			final String firstArg = (String) args.get( 0 );
			if ( "leading".equalsIgnoreCase( firstArg ) ) {
				options.setTrimSpecification( Specification.LEADING );
			}
			else if ( "trailing".equalsIgnoreCase( firstArg ) ) {
				options.setTrimSpecification( Specification.TRAILING );
			}
			else if ( "both".equalsIgnoreCase( firstArg ) ) {
				// already the default in Options
			}
			else {
				potentialTrimCharacterArgIndex = 0;
			}

			final String potentialTrimCharacter = (String) args.get( potentialTrimCharacterArgIndex );
			if ( "from".equalsIgnoreCase( potentialTrimCharacter ) ) {
				trimSource = (String) args.get( potentialTrimCharacterArgIndex + 1 );
			}
			else if ( potentialTrimCharacterArgIndex + 1 >= args.size() ) {
				trimSource = potentialTrimCharacter;
			}
			else {
				options.setTrimCharacter( potentialTrimCharacter );
				if ( "from".equalsIgnoreCase( (String) args.get( potentialTrimCharacterArgIndex + 1 ) ) ) {
					trimSource = (String) args.get( potentialTrimCharacterArgIndex + 2 );
				}
				else {
					trimSource = (String) args.get( potentialTrimCharacterArgIndex + 1 );
				}
			}
		}
		return render( options, trimSource, factory );
	}

	protected abstract String render(Options options, String trimSource, SessionFactoryImplementor factory);

	protected static class Options {
		public static final String DEFAULT_TRIM_CHARACTER = "' '";

		private String trimCharacter = DEFAULT_TRIM_CHARACTER;
		private Specification trimSpecification = Specification.BOTH;

		public String getTrimCharacter() {
			return trimCharacter;
		}

		public void setTrimCharacter(String trimCharacter) {
			this.trimCharacter = trimCharacter;
		}

		public Specification getTrimSpecification() {
			return trimSpecification;
		}

		public void setTrimSpecification(Specification trimSpecification) {
			this.trimSpecification = trimSpecification;
		}
	}

	protected static class Specification {
		public static final Specification LEADING = new Specification( "leading" );
		public static final Specification TRAILING = new Specification( "trailing" );
		public static final Specification BOTH = new Specification( "both" );

		private final String name;

		private Specification(String name) {
			this.name = name;
		}

		public String getName() {
			return name;
		}
	}
}
