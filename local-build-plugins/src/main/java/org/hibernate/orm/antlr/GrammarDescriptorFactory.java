/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html
 */
package org.hibernate.orm.antlr;

import org.gradle.api.NamedDomainObjectFactory;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.TaskProvider;

/**
 * @author Steve Ebersole
 */
public class GrammarDescriptorFactory implements NamedDomainObjectFactory<SplitGrammarDescriptor> {
	private final AntlrSpec antlrSpec;
	private final TaskProvider<Task> groupingTask;
	private final Project project;

	public GrammarDescriptorFactory(AntlrSpec antlrSpec, TaskProvider<Task> groupingTask, Project project) {
		this.antlrSpec = antlrSpec;
		this.groupingTask = groupingTask;
		this.project = project;
	}

	@Override
	public SplitGrammarDescriptor create(String name) {
		final SplitGrammarDescriptor descriptor = new SplitGrammarDescriptor( name, antlrSpec, project.getObjects() );

		final SplitGrammarGenerationTask generatorTask = project.getTasks().create(
				determineTaskName( name ),
				SplitGrammarGenerationTask.class,
				descriptor,
				antlrSpec
		);
		generatorTask.setDescription( "Performs Antlr grammar generation for the `" + name + "` grammar" );
		generatorTask.setGroup( "antlr" );
		groupingTask.configure( (task) -> task.dependsOn( generatorTask ) );

		return descriptor;
	}

	private String determineTaskName(String grammarName) {
		final String titularGrammarName = Character.toTitleCase( grammarName.charAt(0) ) + grammarName.substring(1);
		return "generate" + titularGrammarName + "Parser";
	}
}
