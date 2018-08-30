def branch_part   = env.BRANCH_NAME.replace("features/","").replace("/","").take(20).toLowerCase()
def build_number = env.BUILD_NUMBER.toInteger()
def version  = "0.0.${build_number}"
def docker_tag = "${branch_part}-${version}"

currentBuild.displayName = version

pipeline {
    agent { node { label 'build'}}

    options{
        disableConcurrentBuilds()
        timestamps()
    }

    environment {
        SBT_FLAGS = "-Dsbt.log.noformat=true -Dbranch.name=${branch_part} -Dapplication.version=${version}"
        VERSION = "${version}"
        DOCKER_TAG= "${docker_tag}"
    }

    stages{

        stage('Test formatting & style') {
            steps{
                sh 'sbt ${SBT_FLAGS} scalafmt::test test:scalafmt::test scalastyle test:scalastyle'
            }
        }

        stage('Compile') {
            steps{
                sh 'sbt ${SBT_FLAGS} clean compile'
                sh 'sbt ${SBT_FLAGS} test:compile'
            }
        }

        stage('Test') {
            steps{
                sh 'sbt ${SBT_FLAGS} test'
            }
            post {
                always {
                    junit '**/target/test-reports/*.xml'
                }
            }
        }

        stage('Publish'){
            steps{
                sh "sbt ${SBT_FLAGS} publish"
            }
        }

        stage('Tag commit') {
            when { branch 'master' }
            steps{
                script{
                    try {
                        newTag = "v${version}"
                        tagGitCommit(newTag)
                        pushGitTag('3f722b9b-bb2d-4f4e-bd3f-ce4fb2fdc217', newTag)
                    } catch(e) {
                        println("failed to push version tag to GitHub : ${e}")
                        throw e
                    }
                }
            }
        }
    }
}

def tagGitCommit(tag) {
    sh "git tag ${tag} -m 'Jenkins tagging'"
}

def pushGitTag(credentials, tag) {
    withCredentials([usernameColonPassword(credentialsId: credentials, variable: 'LOGIN')]) {
        sh "git push 'https://${LOGIN}@github.com/Patagona/ebay-integration.git' '${tag}'"
    }
}