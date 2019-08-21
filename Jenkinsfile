def commit = "UNKNOWN"
def version = "UNKNOWN"

pipeline {
    agent {
        kubernetes {
            label 'ego-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: jdk
    tty: true
    image: openjdk:11
    env: 
      - name: DOCKER_HOST 
        value: tcp://localhost:2375 
  - name: dind-daemon 
    image: docker:18.06-dind
    securityContext: 
        privileged: true 
    volumeMounts: 
      - name: docker-graph-storage 
        mountPath: /var/lib/docker 
  - name: helm
    image: alpine/helm:2.12.3
    command:
    - cat
    tty: true
  - name: docker
    image: docker:18-git
    tty: true
    volumeMounts:
    - mountPath: /var/run/docker.sock
      name: docker-sock
  volumes:
  - name: docker-sock
    hostPath:
      path: /var/run/docker.sock
      type: File
  - name: docker-graph-storage 
    emptyDir: {}
"""
        }
    }
    stages {
        stage('Prepare') {
            steps {
                script {
                    commit = sh(returnStdout: true, script: 'git describe --always').trim()
                }
                script {
                    version = readMavenPom().getVersion()
                }
            }
        }
        stage('Test') {
            environment {
                SELENIUM_TEST_TYPE = 'BROWSERSTACK'
                BROWSERSTACK_USERNAME = credentials('browserstack_user')
                BROWSERSTACK_ACCESS_KEY = credentials('browserstack_key')
                FACEBOOK_CLIENT_CLIENTID = '1196783150487854'
                FACEBOOK_CLIENT_CLIENTSECRET = credentials('facebook_selenium_secret')
                FACEBOOK_USER = 'immxgmqvsf_1551302168@tfbnw.net'
                FACEBOOK_PASS = credentials('facebook_pass')
            }
            steps {
                container('jdk') {
                    sh "./mvnw test"
                }
            }
        }
        stage('Build & Publish Develop') {
            when {
                branch "develop"
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --network=host -f Dockerfile.prod . -t overture/ego:edge"
                    sh "docker push overture/ego:edge"
                }
            }
        }

        stage('Release & tag') {
          when {
            branch "master"
          }
          steps {
                container('docker') {
                    withCredentials([usernamePassword(credentialsId: 'OvertureBioGithub', passwordVariable: 'GIT_PASSWORD', usernameVariable: 'GIT_USERNAME')]) {
                        sh "git tag ${version}"
                        sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/overture-stack/ego --tags"
                    }
                    withCredentials([usernamePassword(credentialsId:'OvertureDockerHub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                        sh 'docker login -u $USERNAME -p $PASSWORD'
                    }
                    sh "docker build --network=host -f Dockerfile.prod . -t overture/ego:latest -t overture/ego:${version}"
                    sh "docker push overture/ego:${version}"
                    sh "docker push overture/ego:latest"
                }
            }
        }

    }
}