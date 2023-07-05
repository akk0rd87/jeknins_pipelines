def gradleCall(String key, String params, String GRADLE_TASK) {
    withCredentials([file(credentialsId: "${key}"   , variable: 'ANDROID_KEYSTORE_KEY'),
                     file(credentialsId: "${params}", variable: 'ANDROID_KEYSTORE_PARAMS'),
                     file(credentialsId: 'GooglePlayApiCredentials' , variable: 'ANDROID_GOOGLEPLAY_CREDS')]) {
        sh '${PROJECT_DIR}/gradlew -p ${PROJECT_DIR} ' + GRADLE_TASK + ' --no-daemon'
    }
}

def call(
    String ProjectDir,
    String ProjectURL,
    String DeployKey,
    String KeyStoreKeyFile,
    String KeyStoreKeyParams,
    String ProjectBranch,
    String AgentLabel
) {
    pipeline {
        parameters {
          string(name: 'AKK0RD_SDK_BRANCH', defaultValue: 'master')
        }

        agent {
            label "${AgentLabel}"
        }

        options {
            skipDefaultCheckout()
        }

        environment {
            AKKORD_SDK_DIR="akkordsdk"
            AKKORD_SDK_HOME="${WORKSPACE}/${AKKORD_SDK_DIR}/"
            PROJECT_DIR="${WORKSPACE}/${ProjectDir}/proj.android/"
        }

        stages {
            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${AKK0RD_SDK_BRANCH}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
                }
            }

            stage("Checkout project") {
                steps {
                    checkout scmGit(branches: [[name: "${ProjectBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${ProjectDir}"]], userRemoteConfigs: [[credentialsId: "${DeployKey}", url: "${ProjectURL}"]])
                    sh 'chmod +x ${PROJECT_DIR}/gradlew'
                }
            }

            stage('Build debug') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", '_jenkinsBuildDebug')
                }
            }

            stage('Build release') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", '_jenkinsBuildRelease')
                }
            }

            stage('Test') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", '_jenkinsTest')
                }
            }

            stage('Publish') {
                steps {
                    gradleCall("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", '_jenkinsPublish')
                }
            }
        }
    }
}