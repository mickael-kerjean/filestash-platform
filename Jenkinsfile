pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: "5", artifactNumToKeepStr: "1"))
    }
    stages {
        stage("Build") {
            steps {
                script {
                    docker.image("eclipse-temurin:17-jdk").inside {
                        sh "./mvnw package"
                    }
                }
            }
        }
        stage("Release") {
            steps {
                script {
                    docker.image("alpine").inside("--user=root --add-host=hal.filestash.app:10.10.102.2") {
                        withCredentials([sshUserPrivateKey(credentialsId: "app-filestash-hal", keyFileVariable: "SSH_KEY")]) {
                            sh "apk add openssh-client"
                            sh 'scp -i $SSH_KEY -o StrictHostKeyChecking=no target/platform-0.0.1-SNAPSHOT.jar ci@hal.filestash.app:/mnt/me-kerjean-archive/files/artifacts/filestash-platform.jar'
                            sh 'scp -i $SSH_KEY -o StrictHostKeyChecking=no src/main/resources/application.properties ci@hal.filestash.app:/mnt/me-kerjean-archive/files/artifacts/filestash-platform.properties'
                        }
                    }
                }
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
