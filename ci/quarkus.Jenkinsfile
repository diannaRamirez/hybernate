@Library('hibernate-jenkins-pipeline-helpers@1.5') _

// Avoid running the pipeline on branch indexing
if (currentBuild.getBuildCauses().toString().contains('BranchIndexingCause')) {
  	print "INFO: Build skipped due to trigger being Branch Indexing"
	currentBuild.result = 'NOT_BUILT'
  	return
}

pipeline {
    agent {
        label 'LongDuration'
    }
    tools {
        jdk 'OpenJDK 17 Latest'
    }
    options {
        buildDiscarder(logRotator(numToKeepStr: '3', artifactNumToKeepStr: '3'))
        disableConcurrentBuilds(abortPrevious: true)
    }
    stages {
        stage('Build') {
        	steps {
				script {
					sh './gradlew publishToMavenLocal -PmavenMirror=nexus-load-balancer-c4cf05fd92f43ef8.elb.us-east-1.amazonaws.com --no-daemon'
					script {
						env.HIBERNATE_VERSION = sh (
								script: "grep hibernateVersion gradle/version.properties|cut -d'=' -f2",
								returnStdout: true
						).trim()
					}
					dir('.release/quarkus') {
						sh "git clone -b 3.2 --single-branch https://github.com/quarkusio/quarkus.git . || git reset --hard && git clean -fx && git pull"
						sh "sed -i 's@<hibernate-orm.version>.*</hibernate-orm.version>@<hibernate-orm.version>${env.HIBERNATE_VERSION}</hibernate-orm.version>@' bom/application/pom.xml"
						// Need to override the default maven configuration this way, because there is no other way to do it
						sh "sed -i 's/-Xmx5g/-Xmx1920m/' ./.mvn/jvm.config"
						sh "echo -e '\\n-XX:MaxMetaspaceSize=768m'>>./.mvn/jvm.config"
						sh "./mvnw -Dquickly install"
						// Need to kill the gradle daemons started during the Maven install run
						sh "pkill -f '.*GradleDaemon.*' || true"
						// Need to override the default maven configuration this way, because there is no other way to do it
						sh "sed -i 's/-Xmx1920m/-Xmx1372m/' ./.mvn/jvm.config"
						sh "sed -i 's/MaxMetaspaceSize=768m/MaxMetaspaceSize=512m/' ./.mvn/jvm.config"
						def excludes = "'!integration-tests/kafka-oauth-keycloak,!integration-tests/kafka-sasl-elytron,!integration-tests/hibernate-search-orm-opensearch,!integration-tests/maven,!docs,!integration-tests/mongodb-client,!integration-tests/mongodb-panache,!integration-tests/mongodb-panache-kotlin,!integration-tests/mongodb-devservices,!integration-tests/mongodb-rest-data-panache,!integration-tests/liquibase-mongodb,!extensions/mongodb-client/deployment,!extensions/liquibase-mongodb/deployment,!extensions/panache/mongodb-panache/deployment,!extensions/panache/mongodb-panache-kotlin/deployment,!extensions/panache/mongodb-panache-kotlin/runtime,!extensions/panache/mongodb-rest-data-panache/deployment,!extensions/panache/mongodb-panache-common/deployment'"
						sh "TESTCONTAINERS_RYUK_CONTAINER_PRIVILEGED=true ./mvnw -pl :quarkus-hibernate-orm -amd -pl ${excludes} verify -Dstart-containers -Dtest-containers -Dskip.gradle.build"
					}
				}
			}
		}
    }
    post {
        always {
    		configFileProvider([configFile(fileId: 'job-configuration.yaml', variable: 'JOB_CONFIGURATION_FILE')]) {
            	notifyBuildResult maintainers: (String) readYaml(file: env.JOB_CONFIGURATION_FILE).notification?.email?.recipients
            }
        }
    }
}