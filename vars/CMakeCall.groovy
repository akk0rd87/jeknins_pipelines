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
                    sh 'chmod +x ${PROJECT_DIR}/gradlew'
                }
            }

            stage('Test') {
                steps {
                    dir("${PROJECT_DIR}") {
                        //for (int i = 0; i < 10; i++) {
                          sh 'cmake -S utests -B utests/build && cmake --build utests/build && ctest --test-dir utests/build'
                        //}
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