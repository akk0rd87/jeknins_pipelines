def call(
    String ProjectDir,
    String ProjectURL,
    String DeployKey,
    String ProjectBranch,
    String AgentLabel
) {
    pipeline {
        agent {
            label "${AgentLabel}"
        }

        environment {
            PROJECT_DIR="${WORKSPACE}/${ProjectDir}/"
        }

        stages {
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