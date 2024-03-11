def call(
    String ProjectDir,
    String ProjectURL,
    String DeployKey,
    String ProjectBranch,
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
            PROJECT_DIR="${WORKSPACE}/${ProjectDir}/"
        }

        stages {
            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
                }
            }

            stage("Checkout project") {
                steps {
                    checkout scmGit(branches: [[name: "${ProjectBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${ProjectDir}"]], userRemoteConfigs: [[credentialsId: "${DeployKey}", url: "${ProjectURL}"]])
                }
            }

            stage('cmake') {
                steps {
                    dir("${PROJECT_DIR}") {
                        sh 'cmake -S . -B ./build && cmake --build ./build'
                    }
                }
            }

            stage('Test') {
                steps {
                    dir("${PROJECT_DIR}") {
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