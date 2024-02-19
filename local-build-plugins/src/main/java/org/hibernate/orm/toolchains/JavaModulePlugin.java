/*
 * Hibernate, Relational Persistence for Idiomatic Java
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later.
 * See the lgpl.txt file in the root directory or http://www.gnu.org/licenses/lgpl-2.1.html.
 */
package org.hibernate.orm.toolchains;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.inject.Inject;

import org.gradle.api.Action;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.compile.CompileOptions;
import org.gradle.api.tasks.compile.JavaCompile;
import org.gradle.api.tasks.javadoc.Javadoc;
import org.gradle.api.tasks.testing.Test;
import org.gradle.jvm.toolchain.JavaLanguageVersion;
import org.gradle.jvm.toolchain.JavaToolchainService;

/**
 * @author Steve Ebersole
 */
public class JavaModulePlugin implements Plugin<Project> {
	private final JavaToolchainService toolchainService;

	@Inject
	public JavaModulePlugin(JavaToolchainService toolchainService) {
		this.toolchainService = toolchainService;
	}

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply( JdkVersionPlugin.class );

		final JdkVersionConfig jdkVersionsConfig = project.getExtensions().getByType( JdkVersionConfig.class );

		final JavaPluginExtension javaPluginExtension = project.getExtensions().getByType( JavaPluginExtension.class );

		final SourceSetContainer sourceSets = javaPluginExtension.getSourceSets();
		final SourceSet mainSourceSet = sourceSets.getByName( SourceSet.MAIN_SOURCE_SET_NAME );
		final SourceSet testSourceSet = sourceSets.getByName( SourceSet.TEST_SOURCE_SET_NAME );

		final JavaCompile mainCompileTask = (JavaCompile) project.getTasks().getByName( mainSourceSet.getCompileJavaTaskName() );
		final JavaCompile testCompileTask = (JavaCompile) project.getTasks().getByName( testSourceSet.getCompileJavaTaskName() );
		final Test testTask = (Test) project.getTasks().findByName( testSourceSet.getName() );

		if ( !jdkVersionsConfig.isExplicitlyConfigured() ) {
			mainCompileTask.setSourceCompatibility( jdkVersionsConfig.getMainReleaseVersion().toString() );
			mainCompileTask.setTargetCompatibility( jdkVersionsConfig.getMainReleaseVersion().toString() );

			testCompileTask.setSourceCompatibility( jdkVersionsConfig.getTestCompileVersion().toString() );
			testCompileTask.setTargetCompatibility( jdkVersionsConfig.getTestCompileVersion().toString() );
		}
		else {
			javaPluginExtension.getToolchain().getLanguageVersion().set( jdkVersionsConfig.getMainCompileVersion() );

			configureCompileTasks( project );
			configureTestTasks( project );
			configureJavadocTasks( project, mainSourceSet );

			configureCompileTask( mainCompileTask, jdkVersionsConfig.getMainReleaseVersion() );
			configureCompileTask( testCompileTask, jdkVersionsConfig.getTestReleaseVersion() );

			testCompileTask.getJavaCompiler().set(
					toolchainService.compilerFor( javaToolchainSpec -> {
						javaToolchainSpec.getLanguageVersion().set( jdkVersionsConfig.getTestCompileVersion() );
					} )
			);
			if ( testTask != null ) {
				testTask.getJavaLauncher().set(
						toolchainService.launcherFor( javaToolchainSpec -> {
							javaToolchainSpec.getLanguageVersion().set( jdkVersionsConfig.getTestLauncherVersion() );
						} )
				);

				final String launcherArgs = jdkVersionsConfig.getTest().getLauncherArgs();
				if ( launcherArgs != null ) {
					testTask.jvmArgs( (Object[]) launcherArgs.split( " " ) );
				}
			}
		}
	}

	private void configureCompileTask(JavaCompile compileTask, JavaLanguageVersion releaseVersion) {
		final CompileOptions compileTaskOptions = compileTask.getOptions();
		compileTaskOptions.getRelease().set( releaseVersion.asInt() );
		// Needs add-opens because of https://github.com/gradle/gradle/issues/15538
		compileTaskOptions.getForkOptions().getJvmArgs().add( "--add-opens" );
		compileTaskOptions.getForkOptions().getJvmArgs().add( "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED" );
	}

	private void configureCompileTasks(Project project) {
		project.getTasks().withType( JavaCompile.class ).configureEach( new Action<JavaCompile>() {
			@Override
			public void execute(JavaCompile compileTask) {
				getJvmArgs( compileTask ).addAll(
						Arrays.asList(
								project.property( "toolchain.compiler.jvmargs" ).toString().split( " " )
						)
				);
				compileTask.doFirst(
						new Action<Task>() {
							@Override
							public void execute(Task task) {
								project.getLogger().lifecycle(
										"Compiling with '{}'",
										compileTask.getJavaCompiler().get().getMetadata().getInstallationPath()
								);
							}
						}
				);
			}
		} );
	}

	private void configureTestTasks(Project project) {
		project.getTasks().withType( Test.class ).configureEach( new Action<Test>() {
			@Override
			public void execute(Test testTask) {
				getJvmArgs( testTask ).addAll(
						Arrays.asList(
								project.property( "toolchain.launcher.jvmargs" ).toString().split( " " )
						)
				);
				if ( project.hasProperty( "test.jdk.launcher.args" ) ) {
					getJvmArgs( testTask ).addAll(
							Arrays.asList(
								project.getProperties().get( "test.jdk.launcher.args" ).toString().split( " " )
							)
					);
				}
				testTask.doFirst(
						new Action<Task>() {
							@Override
							public void execute(Task task) {
								project.getLogger().lifecycle(
										"Testing with '{}'",
										testTask.getJavaLauncher().get().getMetadata().getInstallationPath()
								);
							}
						}
				);
			}
		} );
	}

	private void configureJavadocTasks(Project project, SourceSet mainSourceSet) {
		project.getTasks().named( mainSourceSet.getJavadocTaskName(), Javadoc.class, (task) -> {
			task.getOptions().setJFlags( javadocFlags( project ) );
			task.doFirst( new Action<Task>() {
				@Override
				public void execute(Task t) {
					project.getLogger().lifecycle(
							"Generating javadoc with '{}'",
							task.getJavadocTool().get().getMetadata().getInstallationPath()
					);
				}
			} );
		} );
	}

	private static List<String> javadocFlags(Project project) {
		final String jvmArgs = project.property( "toolchain.javadoc.jvmargs" ).toString();
		final String[] splits = jvmArgs.split( " " );
		return Arrays.asList( splits ).stream().filter( (split) -> !split.isEmpty() ).collect( Collectors.toList() );
	}

	public static List<String> getJvmArgs(JavaCompile compileTask) {
		final List<String> existing = compileTask
				.getOptions()
				.getForkOptions()
				.getJvmArgs();
		if ( existing == null ) {
			final List<String> target = new ArrayList<>();
			compileTask.getOptions().getForkOptions().setJvmArgs( target );
			return target;
		}
		else {
			return existing;
		}
	}

	public static List<String> getJvmArgs(Test testTask) {
		final List<String> existing = testTask.getJvmArgs();
		if ( existing == null || !( existing instanceof ArrayList ) ) {
			final List<String> target = new ArrayList<>();
			testTask.setJvmArgs( target );
			return target;
		}
		else {
			return existing;
		}
	}
}
