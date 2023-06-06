def gradleCall(String key, String params, String  GRADLE_TASK) {
    withCredentials([file(credentialsId: "${key}"   , variable: 'ANDROID_KEYSTORE_KEY'),
                     file(credentialsId: "${params}", variable: 'ANDROID_KEYSTORE_PARAMS'),
                     file(credentialsId: 'GooglePlayApiCredentials' , variable: 'ANDROID_GOOGLEPLAY_CREDS')]) {
        sh '${PROJECT_DIR}/gradlew -p ${PROJECT_DIR} ' + GRADLE_TASK
    }
}

def call(String ProjectName, String DeployKey, String KeyStoreKeyFile, String KeyStoreKeyParams, String SDKBranch, String ProjectBranch) {
    pipeline {
        agent any

        environment {
            ANDROID_SDK_ROOT='/opt/android-sdk'
            AKKORD_SDK_DIR="akkordsdk"
            AKKORD_SDK_HOME="${WORKSPACE}/${AKKORD_SDK_DIR}/"
            PROJECT_DIR="${WORKSPACE}/${ProjectName}/proj.android/"
        }

        stages {
            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
                }
            }

            stage("Checkout project") {
                steps {
                    checkout scmGit(branches: [[name: "${ProjectBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${ProjectName}"]], userRemoteConfigs: [[credentialsId: "${DeployKey}", url: "git@github.com:akk0rd87/${ProjectName}.git"]])
                    sh 'chmod +x ${PROJECT_DIR}/gradlew'
                }
            }

            stage('Build debug') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", ':app:assembleGooglePlayDebug')
                }
            }

            stage('Build release') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", ':app:assembleGooglePlayRelease')
                }
            }

            stage('Publish') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", '_publishGooglePlayStoreBundleToAlpha')
                }
            }
        }
    }
}