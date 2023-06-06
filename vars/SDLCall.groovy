def gradleCall(GRADLE_TASK) {
    withCredentials([file(credentialsId: "${KeyStoreKeyFile}"       , variable: 'ANDROID_KEYSTORE_KEY'),
                     file(credentialsId: "${KeyStoreKeyParams}"     , variable: 'ANDROID_KEYSTORE_PARAMS'),
                     file(credentialsId: 'GooglePlayApiCredentials' , variable: 'ANDROID_GOOGLEPLAY_CREDS')]) {
        sh '${PROJECT_DIR}/gradlew -p ${PROJECT_DIR} ' + GRADLE_TASK
    }
}

def call(
  String ProjectName,
  String DeployKey,
  String KeyStoreKeyFile,
  String KeyStoreKeyParams,
  String SDKBranch,
  String ProjectBranch
) {
    pipeline {
        agent any

        environment {
            ANDROID_SDK_ROOT='/opt/android-sdk'
            AKKORD_SDK_HOME="${WORKSPACE}/akkordsdk/"
            PROJECT_DIR="${WORKSPACE}/${ProjectName}/proj.android/"
        }

        stages {
            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'akkordsdk']], userRemoteConfigs: [[url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
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
}