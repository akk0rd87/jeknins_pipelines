def BuildOpenSSL(ANDROID_ARCH, OPENSSL_BUILD_KEY) {
    sh '''
      PATH="${ANDROID_NDK_ROOT}/toolchains/llvm/prebuilt/linux-x86_64/bin:${ANDROID_NDK_ROOT}/toolchains/arm-linux-androideabi-4.9/prebuilt/linux-x86_64/bin:${ANDROID_NDK_ROOT}/prebuilt/linux-x86_64/bin:$PATH"
      OPENSSL_BUILLD_OPTIONS="-D__ANDROID_API__=21 -DAPP_PLATFORM=16 -DTARGET_PLATFORM=16 -DOPENSSL_NO_STDIO no-sock no-ui-console no-err no-asm no-egd no-zlib no-uplink no-camellia no-filenames no-legacy no-stdio no-tests no-engine no-threads"

      OPENSSL_ANDROID_DEST_DIR="${AKKORD_OPENSSL_HOME}/android/''' + ANDROID_ARCH + '''/"

      mkdir -p ${OPENSSL_ANDROID_DEST_DIR}
      cd ${OPENSSL_HOME}
      ./Configure ${OPENSSL_BUILLD_OPTIONS} ''' + OPENSSL_BUILD_KEY + '''
      make clean && make

      cp -r "${OPENSSL_HOME}/include"      "${OPENSSL_ANDROID_DEST_DIR}"
      mv    "${OPENSSL_HOME}/libssl.a"     "${OPENSSL_ANDROID_DEST_DIR}"
      mv    "${OPENSSL_HOME}/libcrypto.a"  "${OPENSSL_ANDROID_DEST_DIR}"
      mv    "${OPENSSL_HOME}/libssl.so"    "${OPENSSL_ANDROID_DEST_DIR}"
      mv    "${OPENSSL_HOME}/libcrypto.so" "${OPENSSL_ANDROID_DEST_DIR}"
    '''
}

def runGradleProjectCMD(String key, String params, String PROJECT, String COMMAND) {
    withCredentials([file(credentialsId: "${key}"   , variable: 'ANDROID_KEYSTORE_KEY'),
                     file(credentialsId: "${params}", variable: 'ANDROID_KEYSTORE_PARAMS'),
                     file(credentialsId: 'GooglePlayApiCredentials' , variable: 'ANDROID_GOOGLEPLAY_CREDS')]) {
        sh '"${WORKSPACE}/wordsapp/' + PROJECT + '/proj.android/gradlew" -p "${WORKSPACE}/wordsapp/' + PROJECT + '/proj.android" ' + COMMAND
    }
}

def runBuildDebug(KeyStoreKeyFile, KeyStoreKeyParams, PROJECT) {
    runGradleProjectCMD("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", PROJECT, '_jenkinsBuildDebug')
}

def runBuildRelease(KeyStoreKeyFile, KeyStoreKeyParams, PROJECT) {
    runGradleProjectCMD("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", PROJECT, '_jenkinsBuildRelease')
}

def runTest(KeyStoreKeyFile, KeyStoreKeyParams, PROJECT) {
    runGradleProjectCMD("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", PROJECT, '_jenkinsTest')
}

def runPublish(KeyStoreKeyFile, KeyStoreKeyParams, PROJECT) {
    runGradleProjectCMD("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", PROJECT, '_jenkinsPublish')
}

def call(
    String ProjectDir,
    String ProjectURL,
    String DeployKey,
    String KeyStoreKeyFile,
    String KeyStoreKeyParams,
    String SDKBranch,
    String ProjectBranch
) {
    pipeline {
        agent any

        environment {
            AKKORD_SDK_DIR="akkordsdk"
            AKKORD_SDK_HOME="${WORKSPACE}/${AKKORD_SDK_DIR}/"
            ANDROID_NDK_HOME="${ANDROID_SDK_ROOT}/ndk/25.1.8937393"
            ANDROID_NDK_ROOT="${ANDROID_NDK_HOME}"
            ASIO_HOME="${WORKSPACE}/asio/"
            P2PCLIENT_HOME="${WORKSPACE}/p2putils/p2pclient/include"
            OPENSSL_HOME="${WORKSPACE}/openssl/"
            AKKORD_OPENSSL_HOME="${WORKSPACE}/openssl_builds/"
            GRADLE_CALL="${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android/gradlew -p ${WORKSPACE}/${AKK0RD87_GITHUB_PROJECT_NAME}/proj.android"
        }

        stages {
            stage('Checkout openssl') {
                steps {
                    checkout scmGit(branches: [[name: '*/openssl-3.0']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'openssl']], userRemoteConfigs: [[url: 'https://github.com/openssl/openssl.git']])
                    //sh 'git --git-dir=${OPENSSL_HOME}/.git submodule update --init --recursive --force'
                }
            }

            stage('Build openssl') {
                steps {
                    BuildOpenSSL('x86'        , 'android-x86'    )
                    BuildOpenSSL('x86_64'     , 'android-x86_64 ')
                    BuildOpenSSL('armeabi-v7a', 'android-arm'    )
                    BuildOpenSSL('arm64-v8a'  , 'android-arm64'  )
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
                    checkout scmGit(branches: [[name: '*/master']], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: 'p2putils']], userRemoteConfigs: [[credentialsId: 'p2pUtilsDeployKey', url: 'git@github.com:akk0rd87/p2putils.git']])
                    sh 'chmod +x "${WORKSPACE}/p2putils/sharedlib/build.sh"'
                }
            }

            stage('Build sharedwrapper') {
                steps {
                    sh 'cd "${WORKSPACE}/p2putils/sharedlib" && ./build.sh'
                    sh 'cd "${WORKSPACE}"'
                }
            }

            stage('Checkout akkordsdk') {
                steps {
                    checkout scmGit(branches: [[name: "${SDKBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${AKKORD_SDK_DIR}"]], userRemoteConfigs: [[url: 'https://github.com/akk0rd87/akk0rdsdk.git']])
                }
            }

            stage('Checkout wordsapp') {
                steps {
                    checkout scmGit(branches: [[name: "${ProjectBranch}"]], extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: "${ProjectDir}"]], userRemoteConfigs: [[credentialsId: "${DeployKey}", url: "${ProjectURL}"]])
                    sh 'chmod +x "${WORKSPACE}/wordsapp/wordsru1/proj.android/gradlew"'
                    sh 'chmod +x "${WORKSPACE}/wordsapp/wordsru2/proj.android/gradlew"'
                    sh 'chmod +x "${WORKSPACE}/wordsapp/wordsru3_8/proj.android/gradlew"'
                }
            }

            stage('Build wordsru1 debug') {
                steps {
                    runBuildDebug("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru1')
                }
            }

            stage('Build wordsru2 debug') {
                steps {
                    runBuildDebug("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru2')
                }
            }

            stage('Build wordsru3 debug') {
                steps {
                    runBuildDebug("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru3_8')
                }
            }

            stage('Build wordsru1 release') {
                steps {
                    runBuildRelease("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru1')
                }
            }

            stage('Build wordsru2 release') {
                steps {
                    runBuildRelease("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru2')
                }
            }

            stage('Build wordsru3 release') {
                steps {
                    runBuildRelease("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru3_8')
                }
            }

            stage('Test wordsru1') {
                steps {
                    runTest("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru1')
                }
            }

            stage('Test wordsru2') {
                steps {
                    runTest("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru2')
                }
            }

            stage('Test wordsru3') {
                steps {
                    runTest("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru3_8')
                }
            }

            stage('Publish wordsru1') {
                steps {
                    runPublish("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru1')
                }
            }

            stage('Publish wordsru2') {
                steps {
                    runPublish("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru2')
                }
            }

            stage('Publish wordsru3') {
                steps {
                    runPublish("${KeyStoreKeyFile}", "${KeyStoreKeyParams}", 'wordsru3_8')
                }
            }
        }
    }
}