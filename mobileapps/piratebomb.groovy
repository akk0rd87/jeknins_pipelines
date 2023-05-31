pipeline {
    agent any
    stages {
        stage('build PirateBomb') {
            steps {
                build job: 'PirateBomb'
            }

        }
    }
}