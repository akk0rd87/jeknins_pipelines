def call(
    String ProjectDir,
    String ProjectURL,
    String AppAppleId,
    String AppBundleId,
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
            PROJECT_DIR="${WORKSPACE}/${ProjectDir}/proj.ios/"
            PROJECT_SH_DIR="${PROJECT_DIR}/sh"
            NEW_VERSION_FILE="${WORKSPACE}/${ProjectDir}/newiOSversion.sh"
        }

        stages {
            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[credentialsId: "macOSGithubSDKKey", url: 'git@github.com:akk0rd87/akk0rdsdk.git']])
                }
            }

            stage("Checkout project") {
                steps {
                    checkout scmGit(branches: [[name: "${ProjectBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${ProjectDir}"]], userRemoteConfigs: [[credentialsId: "${DeployKey}", url: "${ProjectURL}"]])
                }
            }

            stage("Build SDL") {
                steps {
                    dir("${AKKORD_SDK_HOME}/libraries/SDL/ios/") {
                        sh './lipo_create.sh'
                    }
                }
            }

            stage("Build SDL_image") {
                steps {
                    dir("${AKKORD_SDK_HOME}/libraries/SDL_image/ios/") {
                        sh './lipo_create.sh'
                    }
                }
            }

            stage('Test akkordsdk') {
                steps {
                    dir("${AKKORD_SDK_HOME}") {
                        sh 'cmake -S utests -B utests/build && cmake --build utests/build && ctest --test-dir utests/build'
                    }
                }
            }

            stage('Pod: reintegrate') {
                steps {
                    dir("${PROJECT_DIR}") {
                        sh './reintegratePod.sh'
                    }
                }
            }

            stage('Test project') {
                steps {
                    dir("${PROJECT_SH_DIR}") {
                        sh 'chmod +x test.sh && ./test.sh'
                    }
                }
            }

            stage('Update version') {
                environment {
                    APP_APPLE_ID="${AppAppleId}"
                    APP_BUNDLE_ID="${AppBundleId}"
                    EXPORT_OPTIONS_FILE="${PROJECT_SH_DIR}/exportOptions.plist"
                }
                steps {
                    dir("${PROJECT_SH_DIR}") {
                        withCredentials([file  (credentialsId: 'macOSAPIKey',         variable: 'API_KEY_FILE'),
                                         string(credentialsId: 'macOSAPIKeyIssuerId', variable: 'API_ISSUER'),
                                         string(credentialsId: 'macOSAPIKeyId'      , variable: 'API_KEY_ID')
                        ]) {
                            sh '''
                                python3 "$AKKORD_SDK_HOME"/tools/ios/getAppLastVersion.py
                                python3 "$AKKORD_SDK_HOME"/tools/ios/getProfile.py
                                chmod +x "$NEW_VERSION_FILE"
                                source "$NEW_VERSION_FILE"
                                chmod +x updateversion.sh
                                ./updateversion.sh
                            '''
                        }
                    }
                }
            }

            stage('Archive') {
                steps {
                    dir("${PROJECT_SH_DIR}") {
                        sh 'chmod +x archive.sh && ./archive.sh'
                    }
                }
            }

            stage('Export') {
                steps {
                    dir("${PROJECT_SH_DIR}") {
                        sh 'chmod +x export.sh && ./export.sh'
                    }
                }
            }

            stage('Upload') {
                environment {
                    APP_APPLE_ID="${AppAppleId}"
                    APP_BUNDLE_ID="${AppBundleId}"
                }
                steps {
                    dir("${PROJECT_SH_DIR}") {
                        withCredentials([file  (credentialsId: 'macOSAPIKey',         variable: 'API_KEY_FILE'),
                                         string(credentialsId: 'macOSAPIKeyIssuerId', variable: 'API_ISSUER'),
                                         string(credentialsId: 'macOSAPIKeyId'      , variable: 'API_KEY_ID')
                        ]) {
                            sh '''
                            rm -rf private_keys
                            mkdir  private_keys
                            mv "$API_KEY_FILE" private_keys/
                            source "$NEW_VERSION_FILE"
                            chmod +x upload.sh
                            ./upload.sh
                            rm -rf private_keys
                            '''
                        }
                    }
                }
            }

            stage('Clean') {
                steps {
                    dir("${PROJECT_SH_DIR}") {
                        sh '''
                        rm -rf private_keys
                        chmod +x clean.sh
                        ./clean.sh
                        '''
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