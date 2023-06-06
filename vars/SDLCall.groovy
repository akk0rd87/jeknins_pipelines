def gradleCall(GRADLE_TASK) {
    withCredentials([file(credentialsId: '${ANDROID_KEYSTOREKEY_FILE}'  , variable: 'ANDROID_KEYSTORE_KEY'),
                    file(credentialsId: '${ANDROID_KEYSTOREPARAM_FILE}', variable: 'ANDROID_KEYSTORE_PARAMS'),
                    file(credentialsId: 'GooglePlayApiCredentials'     , variable: 'ANDROID_GOOGLEPLAY_CREDS')]) {
        sh "${WORKSPACE}/${ProjectName}/proj.android/gradlew -p ${WORKSPACE}/${ProjectName}/proj.android " + GRADLE_TASK
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
                    sh "chmod +x ${WORKSPACE}/${ProjectName}/proj.android/gradlew"
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