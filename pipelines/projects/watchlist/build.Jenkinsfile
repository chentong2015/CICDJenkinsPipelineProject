#!/usr/bin/env groovy

env.nodeLabel="java17 && node"
env.mailingList = "example@text.com"

pipeline {
    agent { 
        node { 
            label env.nodeLabel
        } 
    }

    environment {
        PROJECT_KEY='FILE-MANAGER'
        PROJECT_NAME='File Manager API'
    }

    stages {
        stage('Print environment') {
            steps {
                sh 'printenv'
                echo "GIT_BRANCH: ${env.GIT_BRANCH}"
                echo "BUILD_ID: ${env.BUILD_ID}"
                echo "NODE_NAME: ${env.NODE_NAME}"
                echo "JOB_NAME: ${env.JOB_NAME}"
                echo "WORKSPACE: ${env.WORKSPACE}"
                echo "EXECUTOR_NUMBER: ${env.EXECUTOR_NUMBER}"
            }
        }

        stage('Checkout Gradle-utils') {
            steps {
                dir('gradle/gradle-utils'){
                    checkout([  
                        $class: 'GitSCM', 
                        userRemoteConfigs: [[
                          credentialsId: '1abb1ce1',
                          url: 'ssh://git@bitbucketxxx/gradle-utils.git'
                        ]]
                    ])
                }  
            } 
        } 

        // TODO. 执行自定义的Build脚本来构建项目
        stage("Build & Tests") {
            steps {
                echo "Build & Tests"
                script {
                    sh """
                        chmod 755 version.sh
                        chmod 755 build.sh
                    """

                    sh './version.sh'
                    def props = readProperties file: 'propsfile'
                    env.VERSION_AND_REVISION = "${props.VERSION_AND_REVISION}"

                    cd ..
                    sh './build.sh --test'
                    cd -
                }
            }
        }

        stage('Quality Sonar Gate') {
            steps {
                script {
                    withSonarQubeEnv('sonarQube') {
                        if (env.GIT_BRANCH == 'origin/master') {
                            env.SONAR_BRANCH_PARAMETER = ""
                        } else {
                            env.SONAR_BRANCH_PARAMETER = "-Dsonar.branch.name=${env.GIT_BRANCH}"
                        }

                        // TODO. 在构建截断执行Sonar代码检测, 并生成检测报告
                        cd ..
                        sh "./gradlew sonar --scan --stacktrace \
                        -Dsonar.host.url=${env.SONAR_HOST_URL} \
                        -Dsonar.projectKey=${PROJECT_KEY} \
                        -Dsonar.projectName='${PROJECT_NAME}' \
                        -Dsonar.projectVersion=${env.VERSION_AND_REVISION} \
                        -Dsonar.qualitygate.wait=false \
                        ${env.SONAR_BRANCH_PARAMETER}"
                        echo "Sonar Analysis report URL: ${env.SONAR_HOST_URL}dashboard?id=${PROJECT_KEY}"
                        cd -
                    }
                }
            }
        }
	}

    // 如果构建失败，发动JOB构建失败信息
	post {
	    failure {
		    script {
					echo "On failure send emails"
					mail to: env.mailingList,
                    subject: "Pipeline Job ${env.JOB_NAME} Failure",
                    body: "Pipeline Failure, Something is wrong with Job ${env.JOB_NAME}"
		    }
	    }
	}
}
