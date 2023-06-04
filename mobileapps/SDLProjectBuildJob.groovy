def gradleCall(GRADLE_TASK) {
    withCredentials([file(credentialsId: '${ANDROID_KEYSTOREKEY_FILE}'  , variable: 'ANDROID_KEYSTORE_KEY'),
                     file(credentialsId: '${ANDROID_KEYSTOREPARAM_FILE}', variable: 'ANDROID_KEYSTORE_PARAMS'),
                     file(credentialsId: 'GooglePlayApiCredentials'     , variable: 'ANDROID_GOOGLEPLAY_CREDS')]) {
        sh '${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android/gradlew -p ${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android ' + GRADLE_TASK
    }
}

pipeline {
    agent {
       label "SDL"
    }

    environment {
        ANDROID_SDK_ROOT='/opt/android-sdk'
        AKKORD_SDK_HOME="${WORKSPACE}/akkordsdk/"
        GRADLE_CALL="${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android/gradlew -p ${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android"
    }

    stages {
        stage('Checkout akkordsdk') {
            steps {
                checkout scmGit(branches: [[name: "${AKK0RD_SDK_BRANCH}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'akkordsdk']], userRemoteConfigs: [[credentialsId: 'GithubDeployKey', url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
            }
        }

        stage("Checkout project") {
            steps {
                checkout scmGit(branches: [[name: "${PROJECT_BRANCH}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '${AKK0RD87_GITHUB_PROJECT_NAME}']], userRemoteConfigs: [[credentialsId: '${AKK0RD87_DEPLOY_KEY}', url: 'git@github.com:akk0rd87/${AKK0RD87_GITHUB_PROJECT_NAME}.git']])
                sh 'chmod +x ${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android/gradlew'
            }
        }

        stage('Build debug') {
            steps {
                gradleCall(':app:assembleGooglePlayDebug')
            }
        }

        stage('Build release') {
            steps {
                gradleCall(':app:assembleGooglePlayRelease')
            }
        }

        stage('Publish alpha') {
            steps {
                gradleCall('_publishGooglePlayStoreBundleToAlpha')
            }
        }
    }
}