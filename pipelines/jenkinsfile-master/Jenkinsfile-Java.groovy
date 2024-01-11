#!/usr/bin/groovy

def runShell(String command) {
    def responseCode = sh returnStatus: true, script: "${command} &> tmp.txt"
    def output = readFile(file: "tmp.txt")
    return (output != "")
}

pipeline {
    options {
        disableConcurrentBuilds()
    }

    agent {
        kubernetes {
            yamlFile '.ci/kubernetes/template-secondary.yml'
            defaultContainer 'secondary'
            customWorkspace '/src/new/version'
        }
    }

    // 自定义显示在Jenkins > build with parameters上的参数配置
    // 通过参数的选择来执行不同的构建的逻辑判断
    parameters {
        booleanParam(name: 'cleanJob', defaultValue: true, description: 'Clean the job after after completion.')
        booleanParam(name: 'runDBIntTests', defaultValue: false, description: 'Run integration tests on all DBs.')
        booleanParam(name: 'runXATests', defaultValue: false, description: 'Run legacy XA tests.')
        booleanParam(name: 'useDevMavenRepo', defaultValue: false, description: 'Use DevMavenRepo for dependencies not yet validated by TMT.')
    }

    tools {
        maven 'Maven 3'
    }

    environment {
        // 配置maven在构建时的参数: 开启profile, 使用自定义的本地仓库，使用自定义的settings配置文件
        MVN_ARGS = 'install --batch-mode -T 4 ' +
            '-Pwith-it,mx-artifacts,with-coverage ' +
            '-Dmaven.repo.local=${WORKSPACE}/maven-repo-local ' +
            '-s${WORKSPACE}/.ci/jenkins/workflow/das/settings.xml'
        ARGS_RUN = " "
        LEGACY_XA = " "
        LOGS = "-Dlog4j.info -Dmy.logging.threshold=ERROR"
    }

    // 创建CI自动化集成的不同阶段，在每个阶段执行不同的步骤，在不同Step运行相应的脚本
    stages {
        stage('Initialize') {
            steps {
                script {
                    // 输出version, 确认环境满足要求
                    sh "java -version"
                    sh "mvn -version"

                    if (params.useDevMavenRepo) {
                        MVN_ARGS = MVN_ARGS + ' -DuseDevMavenRepo'
                    }

                    // 调用方法来构建执行模块的pom.xml
                    invokeMaven("component/mvr/parent", "install -DskipTests", false)
                    invokeMaven("component/mvr/bom", "install -DskipTests", false)

                    // 使用-pl来构建执行路线下面的部分项目(模块)，同时设置多个要执行的项目模块
                    // -pl,--projects <arg>
                    // Comma-delimited list of specified reactor projects to build instead of all projects.
                    // A project can be specified by [groupId]:artifactId or by its relative path
                    invokeMaven("component/service-framework", "install -DskipTests -am " +
                            "-pl rest/bom,rest/datalayer-bom,rest/client-bom", false)
                    invokeMaven("component/legacy/common/parent", "install -DskipTests", false)
                }
            }
        }

        stage('Legacy database') {
            steps {
                // 将这里的脚本配置提取到一个公共的方法中 => 进行统一的设置
                script {
                    if (env.BRANCH_NAME.contains("master") || env.BRANCH_NAME.contains("xa")) {
                        LOGS = " "
                        ARGS_RUN = "-Dctong.db.int.tests"
                        //Disable for now, need to run c++ on template-primary
                        //LEGACY_XA = "-Prun-testframework,with-native-build -Dctong.native_build.dir=${WORKSPACE}"
                    } else if (params.runDBIntTests || params.runXATests) {
                        LOGS = " "
                        if (params.runDBIntTests)
                            ARGS_RUN = "-Dctong.db.int.tests"
                        //if (params.runXATests)
                            //LEGACY_XA = "-Prun-testframework,with-native-build -Dctong.native_build.dir=${WORKSPACE}"
                    } else if (env.BRANCH_NAME.contains("oracle")) {
                        ARGS_RUN = "-Dctong.db.int.tests.oracle"
                    } else if (env.BRANCH_NAME.contains("postgres")) {
                        ARGS_RUN = "-Dctong.db.int.tests.postgres"
                    }
                }
                invokeMaven("component/legacy/common/database", ARGS_RUN + " " + LEGACY_XA + " " + LOGS, true)
            }
        }

        stage('Conversion Tool Publisher') {
            steps {
                script {
                    if (env.BRANCH_NAME.contains("master")) {
                        // 基于pom文件，使用maven构建时传递指定的配置参数 -Dbuild
                        // 将项目的构建所需的所有依赖全部提取到/dependency-jars目录中
                        // 同时带有.properties属性配置文件
                        invokeMaven("component/database/conversion-tool", ARGS_RUN + " -Dbuild " + LEGACY_XA + " " + LOGS, false)
                        
                        dir('component/database/conversion-tool/target') {
                            // 将打包的结果文件压缩成一个.zip包
                            sh 'mkdir convtool'
                            sh 'mv -t convtool config.properties dependency-jars/ conversion-tool.jar log4j.properties identity.txt'
                            sh "zip conversion-tool.zip -r convtool"

                            sh 'rm convtool/dependency-jars/postgresql* convtool/dependency-jars/ojdbc* convtool/dependency-jars/mssql-jdbc* convtool/dependency-jars/jconn*'
                            sh "zip conversion-tool-without-drivers.zip -r convtool"
                            archiveArtifacts artifacts: '*.zip', fingerprint: true
                        }
                    }
                }
            }
        }

        stage('Build - hbm') {
            steps {
                invokeMaven("component/legacy/hbm", ARGS_RUN, true)
            }
        }
        stage('Build - database') {
            steps {
                invokeMaven("component/database", ARGS_RUN + " " + LOGS, true)
            }
        }
        stage('Build - rest-datalayer') {
            steps {
                invokeMaven("component/service-framework/rest/rest-datalayer/", ARGS_RUN, true)
            }
        }

        stage('Build - All Legacy') {
            steps {
                script {
                    if (env.BRANCH_NAME.contains("master")) {
                        invokeMaven("component/legacy/assembly", ARGS_RUN, true)
                        invokeMaven("component/legacy/common/tx", ARGS_RUN, true)
                        invokeMaven("component/legacy/eclipse-plugins", ARGS_RUN, true)
                        invokeMaven("component/legacy/extension", ARGS_RUN, true)
                        //invokeMaven("component/legacy/odr", ARGS_RUN, true)
                        invokeMaven("component/spring", ARGS_RUN, true)
                    }
                }
            }
        }

        stage('Conversion tool - regression test') {
            steps {
                invokeMaven("component/database/conversion-tool",
                        "clean install -DskipTests -Dbuild", false)
                dir('component/database/conversion-tool') {
                    sh "./conversion-db.sh " +
                            "-t Oracle -T SQL " +
                            "-h dell719srv -H dell719srv " +
                            "-b 1521 -B 1433 " +
                            "-s DELL719SRV -S dell719srv " +
                            "-u DAS_CONV_TEMPLATE -U INSTAL " +
                            "-p DAS_CONV_TEMPLATE -P INSTALL " +
                            "-d DAS_CONV_TEMPLATE -D DAS_CONV_TOOL " +
                            "-e broker1_QPID_CFG_OBJECTS " +
                            "-r ../ -c FALSE"
                    script {
                        if (runShell('grep -iF \'error\' log.out')) {
                            sh "exit 1"
                        }
                    }
                }
            }
        }
    }
}

def invokeMaven(projectDir, options, runSonar) {
    if (!runSonar) {
        sh "source component/mvn-settings.sh && mvn -f " + projectDir + "/pom.xml $MVN_ARGS " + options
        return
    }
    
    String sonarBranchPluginOptions = "";
    if (env.BRANCH_NAME.contains("master")) {
        sonarBranchPluginOptions = " -Dsonar.branch.name=master"
    } else {
        sonarBranchPluginOptions = " -Dsonar.pullrequest.branch=${env.BRANCH_NAME} -Dsonar.pullrequest.key=${env.BRANCH_NAME}"
    }

    withSonarQubeEnv('Sonar') {
        def cmd = "source component/mvn-settings.sh && mvn -f " + projectDir + "/pom.xml $MVN_ARGS " + options +
                sonarBranchPluginOptions +
                " -Djacoco.append=true" +
                " -Djdk.tls.client.protocols=TLSv1.2" +
                " -Dsonar.java.source=11" +
				" -Dsonar.jacoco.reportPaths=./target/jacoco.exec" + 
				" -Dsonar.jacoco.itReportPath=./target/jacoco-it.exec" + 
                " org.sonarsource.scanner.maven:sonar-maven-plugin:3.7.0.1746:sonar"
        sh cmd
    }
    
    fail = true
    maxRetry = 200
    for (i = 0; i < maxRetry; i++) {
        try {
            timeout(time: 2, unit: 'SECONDS') {
                def qualityGate = waitForQualityGate(abortPipeline: true)
                if ('OK' == qualityGate.status || 'FAILED' == qualityGate.status || 'ERROR' == qualityGate.status) {
                    fail = 'FAILED' == qualityGate.status || 'ERROR' == qualityGate.status
                    maxRetry = 0;
                }
            }
        } catch (Exception e) {
            if (i == maxRetry - 1) {
                throw e
            }
        }
    }
	
    if (fail) {
        error "Java quality gate did not pass."
    }
}
