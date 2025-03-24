pipeline {
    agent any
    options {
        buildDiscarder(logRotator(numToKeepStr: "5", artifactNumToKeepStr: "1"))
    }
    stages {
        stage("Build") {
            steps {
                sh "./mvnw package"
            }
        }
        stage("Release") {
            steps {
                sh "scp target/platform-0.0.1-SNAPSHOT.jar ci@hal.filestash.app:/mnt/me-kerjean-archive/files/artifacts/filestash-platform.jar"
                sh "scp src/main/resources/application.properties ci@hal.filestash.app:/mnt/me-kerjean-archive/files/artifacts/filestash-platform.properties"
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}
