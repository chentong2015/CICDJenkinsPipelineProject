#!/usr/bin/env groovy

library 'pipeline-utils'

pipeline {
    // This pipleline can run in any agent
    agent any

    // User maven wrapper to replace this tool
    // tools {
    // Install the Maven version configured as "M3" and add it to the path.
    // maven "M3"
    // }

    // Define the trigger mode: every minute
    triggers {
        pollSCM('* * * * *')
    }

    stages {
        stage('checkout') {
            steps {
                // 从git上指定的路径获取项目的源代码，运行指定的自动化测试，或者配置
                git url: 'https://github.com/chentong2015/JavaJenkinsFreestyleProject.git', branch: 'main'
            }
        }

        // Each stage will show in the Stage View
        stage('Build') {
            // Just as like the post steps
            steps {
                // Run Maven on a Unix agent.
                // sh "mvn -Dmaven.test.failure.ignore=true clean package"

                // To run Maven on a Windows agent, use
                bat "mvn -Dmaven.test.failure.ignore=true clean package"
            }

            // Like the post step: <publisher>..
            post {
                // If Maven was able to run the tests, even if some of the test
                // failed, record the test results and archive the jar file.
                success {
                    // Set the path for artifact and unit test report
                    junit '**/target/surefire-reports/TEST-*.xml'
                    archiveArtifacts 'target/*.jar'
                }
            }
        }
    }
}
