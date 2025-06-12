pipeline {

    agent {
        label "master"
    }

    tools {
        maven "Maven"
    }

    environment {
        NEXUS_VERSION = "nexus3"
        NEXUS_PROTOCOL = "http"
        NEXUS_URL = "you-ip-addr-here:8081"
        NEXUS_REPOSITORY = "maven-nexus-repo"
        NEXUS_CREDENTIAL_ID = "nexus-user-credentials" // 44620c50-1589-*** 用户名称加密码
    }

    stages {
        stage("Clone code from VCS") {
            steps {
                script {
                    git 'https://github.com/javaee/cargotracker.git';
                }
            }
        }

        stage("Maven Build") {
            steps {
                script {
                    sh "mvn package -DskipTests=true"
                }
            }
        }

        stage("Publish to Nexus Repository Manager") {
            steps {
                script {
                    pom = readMavenPom file: "pom.xml";
                    filesByGlob = findFiles(glob: "target/*.${pom.packaging}");
                    artifactPath = filesByGlob[0].path;
                    artifactExists = fileExists artifactPath;

                    if(artifactExists) {
                        echo "*** File: ${artifactPath}, group: ${pom.groupId}, " +
                                "packaging: ${pom.packaging}, version ${pom.version}";
                        nexusArtifactUploader(
                                nexusVersion: NEXUS_VERSION,
                                protocol: NEXUS_PROTOCOL,
                                nexusUrl: NEXUS_URL,
                                groupId: pom.groupId,
                                version: pom.version,
                                repository: NEXUS_REPOSITORY,
                                credentialsId: NEXUS_CREDENTIAL_ID,

                                // 支持同时上传多个artifact到nexus
                                artifacts: [
                                        [artifactId: pom.artifactId, classifier: '', file: artifactPath, type: pom.packaging],
                                        [artifactId: pom.artifactId, classifier: '', file: "pom.xml", type: "pom"]
                                ]
                        );
                    } else {
                        error "*** File: ${artifactPath}, could not be found";
                    }
                }
            }
        }
    }
}