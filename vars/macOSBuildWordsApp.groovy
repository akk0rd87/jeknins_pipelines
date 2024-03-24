def updateVersion(
    String CurrentDir,
    String AppleId,
    String AppBundleId,
    String VersionFile
) {
    dir("${CurrentDir}") {
        withCredentials([file  (credentialsId: 'macOSAPIKey',         variable: 'API_KEY_FILE'),
                         string(credentialsId: 'macOSAPIKeyIssuerId', variable: 'API_ISSUER'),
                         string(credentialsId: 'macOSAPIKeyId'      , variable: 'API_KEY_ID')
        ]) {
            sh '''
                export APP_APPLE_ID='''     + AppleId     + '''
                export APP_BUNDLE_ID='''    + AppBundleId + '''
                export NEW_VERSION_FILE=''' + VersionFile + '''
                python3 "$AKKORD_SDK_HOME"/tools/ios/getAppLastVersion.py
                python3 "$AKKORD_SDK_HOME"/tools/ios/getProfile.py
                chmod +x ''' + VersionFile + '''
                source   ''' + VersionFile + '''
                chmod +x updateversion.sh
                ./updateversion.sh
            '''
        }
    }
}

def uploadApp(
    String CurrentDir,
    String AppleId,
    String AppBundleId,
    String VersionFile
) {
    dir("${CurrentDir}") {
        withCredentials([file  (credentialsId: 'macOSAPIKey',         variable: 'API_KEY_FILE'),
                         string(credentialsId: 'macOSAPIKeyIssuerId', variable: 'API_ISSUER'),
                         string(credentialsId: 'macOSAPIKeyId'      , variable: 'API_KEY_ID')
        ]) {
            sh '''
                export APP_APPLE_ID='''  + AppleId     + '''
                export APP_BUNDLE_ID=''' + AppBundleId + '''
                rm -rf private_keys
                mkdir  private_keys
                source ''' + VersionFile + '''
                chmod +x upload.sh
                ./upload.sh
                rm -rf private_keys
            '''
        }
    }
}

def archiveApp(
    String CurrentDir
) {
    dir("${CurrentDir}") {
        sh 'chmod +x archive.sh && ./archive.sh'
    }
}

def exportApp(
    String CurrentDir
) {
    dir("${CurrentDir}") {
        sh 'chmod +x export.sh && ./export.sh'
    }
}

def cleanBuild(
    String CurrentDir
) {
    dir("${CurrentDir}") {
        sh 'chmod +x clean.sh && ./clean.sh'
    }
}


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
            ASIO_HOME="${WORKSPACE}/asio/"
            P2P_UTILS="${WORKSPACE}/p2putils/"
            OPENSSL_HOME="${WORKSPACE}/openssl/"

            WORDS01="${WORKSPACE}/${ProjectDir}/wordsru1/proj.ios"
            WORDS02="${WORKSPACE}/${ProjectDir}/wordsru2/proj.ios"
            WORDS03="${WORKSPACE}/${ProjectDir}/wordsru3_8/proj.ios"

            NEW_VERSION_FILE01="${WORDS01}/newiOSversion.sh"
            NEW_VERSION_FILE02="${WORDS02}/newiOSversion.sh"
            NEW_VERSION_FILE03="${WORDS03}/newiOSversion.sh"
        }

        stages {
            stage('Checkout openssl') {
                steps {
                    checkout scmGit(branches: [[name: '*/openssl-3.0']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'openssl']], userRemoteConfigs: [[url: 'https://github.com/openssl/openssl.git']])
                    //sh 'git --git-dir=${OPENSSL_HOME}/.git submodule update --init --recursive --force'
                }
            }

            stage('Checkout asio') {
                steps {
                    checkout scmGit(branches: [[name: '*/master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'asio']], userRemoteConfigs: [[url: 'https://github.com/chriskohlhoff/asio.git']])
                    sh 'git --git-dir=${ASIO_HOME}/.git reset --hard c465349fa5cd91a64bb369f5131ceacab2c0c1c3 && git --git-dir=${ASIO_HOME}/.git submodule update --init --recursive --force'
                }
            }

            stage('Checkout p2putils') {
                steps {
                    checkout scmGit(branches: [[name: '*/master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'p2putils']], userRemoteConfigs: [[credentialsId: 'macP2PUtilsGithubDeployKey', url: 'git@github.com:akk0rd87/p2putils.git']])
                }
            }

            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[credentialsId: "macOSGithubSDKKey", url: 'git@github.com:akk0rd87/akk0rdsdk.git']])
                }
            }

            stage("Checkout wordsapp") {
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

            stage('Update version') {
                steps {
                    updateVersion("${WORDS01}/sh/", '960409308' , 'org.popapp.WordsRuFree'       , "${NEW_VERSION_FILE01}")
                    updateVersion("${WORDS02}/sh/", '1080796090', 'org.popapp.WordsRuFree2'      , "${NEW_VERSION_FILE02}")
                    updateVersion("${WORDS03}/sh/", '1112942939', 'org.popapp.sostavslovaizbukv' , "${NEW_VERSION_FILE03}")
                }
            }

            stage('Build openssl') {
                steps {
                    dir("${OPENSSL_HOME}") {
                        sh '''
                          ./Configure no-sock no-ui-console no-err no-asm no-egd no-zlib no-uplink no-camellia no-filenames no-legacy no-stdio no-tests no-engine no-threads ios64-xcrun
                          make clean
                          make
                        '''
                    }
                }
            }

            stage('Archive') {
                steps {
                    archiveApp("${WORDS01}/sh/")
                    archiveApp("${WORDS02}/sh/")
                    archiveApp("${WORDS03}/sh/")
                }
            }

            stage('Export') {
                steps {
                    exportApp("${WORDS01}/sh/")
                    exportApp("${WORDS02}/sh/")
                    exportApp("${WORDS03}/sh/")
                }
            }

            stage('Clean') {
                steps {
                    cleanBuild("${WORDS01}/sh/")
                    cleanBuild("${WORDS02}/sh/")
                    cleanBuild("${WORDS03}/sh/")
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