pipeline {
    agent any

    environment {
        ANDROID_SDK_ROOT='/opt/android-sdk'
        ANDROID_KEYSTORE_HOME='/opt/android-keys/'
        AKKORD_SDK_HOME="${WORKSPACE}/akkordsdk/"
        GRADLE_CALL="${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android/gradlew -p ${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android"
    }

    stages {
        stage('Checkout akkordsdk') {
            steps {
                checkout scmGit(branches: [[name: '*/master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'akkordsdk']], userRemoteConfigs: [[credentialsId: 'GithubDeployKey', url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
            }
        }

        stage("Checkout project") {
            steps {
                checkout scmGit(branches: [[name: '*/master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${AKK0RD87_GITHUB_PROJECT_NAME}']], userRemoteConfigs: [[credentialsId: '${AKK0RD87_DEPLOY_KEY}', url: 'git@github.com:akk0rd87/${AKK0RD87_GITHUB_PROJECT_NAME}.git']])
                sh 'chmod +x ${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android/gradlew'
            }
        }

        stage('Build debug') {
            steps {
                sh '${GRADLE_CALL} :app:assembleGooglePlayDebug'
            }
        }

        stage('Build release') {
            steps {
                sh '${GRADLE_CALL} :app:assembleGooglePlayRelease'
            }
        }

        stage('Publish alpha') {
            steps {
                sh '${GRADLE_CALL} _publishGooglePlayStoreBundleToAlpha'
            }
        }
    }
}