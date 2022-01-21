#!/usr/bin/env groovy
// shebang tells most editors to treat as groovy (syntax highlights, formatting, etc)

pipeline {

    agent any

    triggers {
      pollSCM('* * * * *')
    }

    stages {
        // 该文件置于项目的主目录下面，等效与于包含了隐示的git url的检查
        // implicit checkout stage

        stage('Build') {
            steps {
                // sh './mvnw clean package'
                bat "mvn -Dmaven.test.failure.ignore=true clean package"
            }
        }
    }
    // post after stages, for entire pipeline,
    // is also an implicit step albeit with explicit config here, unlike implicit checkout stage
    post {
        always {
            junit '**/target/surefire-reports/TEST-*.xml'
            archiveArtifacts 'target/*.jar'
        }
    }
}
