def call(
    String SDKBranch,
    String AgentLabel
) {
    pipeline {
        agent {
            label "${AgentLabel}"
        }

        environment {
            AKKORD_SDK_DIR="akkordsdk"
            AKKORD_SDK_HOME="${WORKSPACE}/${AKKORD_SDK_DIR}/"
        }

        stages {
            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
                }
            }

            stage('Test') {
                steps {
                    dir("${AKKORD_SDK_HOME}") {
                        sh 'cmake -S utests -B utests/build && cmake --build utests/build && ctest --test-dir utests/build'
                    }
                }
            }
        }

        post {
            always {
                dir("${WORKSPACE}") {
                    deleteDir()
                }
            }
        }
    }
}