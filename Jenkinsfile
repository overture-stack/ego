def chartversion = "3.1.1"

pipeline {
    agent {
        kubernetes {
            label 'ego-executor'
            yaml """
apiVersion: v1
kind: Pod
spec:
  containers:
  - name: maven
    command: ['cat']
    tty: true
    image: maven:3.8.5-openjdk-17
  - name: jdk
    tty: true
    image: eclipse-temurin:17.0.6_10-jdk-focal
    env:
      - name: DOCKER_HOST
        value: tcp://localhost:2375
  - name: dind-daemon
    image: docker:18.06-dind
    securityContext:
        privileged: true
        runAsUser: 0
    volumeMounts:
      - name: docker-graph-storage
        mountPath: /var/lib/docker
  - name: docker
    image: docker:18-git
    tty: true
    env:
    - name: DOCKER_HOST
      value: tcp://localhost:2375
    - name: HOME
      value: /home/jenkins/agent
  securityContext:
    runAsUser: 1000
  volumes:
  - name: docker-graph-storage
    emptyDir: {}
"""
        }
    }

    environment {
        gitHubRegistry = 'ghcr.io'
        gitHubRepo = 'overture-stack/ego'
        gitHubImageName = "${gitHubRegistry}/${gitHubRepo}"
        dockerHubImageName = 'overture/ego'
        DEPLOY_TO_DEV = false
        PUBLISH_IMAGE = false

        commit = sh(
            returnStdout: true,
            script: 'git describe --always'
        ).trim()

        version = readMavenPom().getVersion()
        slackNotificationsUrl = credentials('OvertureSlackJenkinsWebhookURL')

    }

    parameters {
        booleanParam(
            name: 'DEPLOY_TO_DEV',
            defaultValue: "${env.DEPLOY_TO_DEV}",
            description: 'Deploys your branch to argo-dev'
        )
        booleanParam(
            name: 'PUBLISH_IMAGE',
            defaultValue: "${env.PUBLISH_IMAGE ?: params.DEPLOY_TO_DEV}",
            description: 'Publishes an image with {git commit} tag'
        )
    }

    options {
        timeout(time: 30, unit: 'MINUTES')
        timestamps()
    }


    stages {
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

        stage('Build image') {
            steps {
                container('docker') {
                    sh "docker build --network=host -f Dockerfile . -t ${gitHubImageName}:${commit}"
                }
            }
        }

        stage('Build Artifact & Publish') {
             when {
                anyOf {
                    branch "main"
                    branch "develop"
                }
            }
            steps {
                container('maven') {
                    configFileProvider(
                        [configFile(fileId: '01ae7759-03a9-47c0-9db6-925aebb50ae1', variable: 'MAVEN_SETTINGS')]) {
                        sh 'mvn -s $MAVEN_SETTINGS clean package deploy -DskipTests'
                    }
                }
            }
        }

        stage('Publish images') {
            when {
                anyOf {
                    branch 'develop'
                    branch 'feature/develop-passport'
                    branch 'main'
                    expression { return params.PUBLISH_IMAGE }
                }
            }
            steps {
                container('docker') {
                    withCredentials([usernamePassword(
                        credentialsId:'OvertureBioGithub',
                        passwordVariable: 'PASSWORD',
                        usernameVariable: 'USERNAME'
                    )]) {
                        sh "docker login ${gitHubRegistry} -u $USERNAME -p $PASSWORD"

                        script {
                            if (env.BRANCH_NAME ==~ 'main') { //push edge and commit tags
                                sh "docker tag ${gitHubImageName}:${commit} ${gitHubImageName}:${version}"
                                sh "docker push ${gitHubImageName}:${version}"

                                sh "docker tag ${gitHubImageName}:${commit} ${gitHubImageName}:latest"
                                sh "docker push ${gitHubImageName}:latest"
                            } else { // push commit tag
                                sh "docker push ${gitHubImageName}:${commit}"
                            }

                            if (env.BRANCH_NAME ==~ 'develop') { // push edge tag
                                sh "docker tag ${gitHubImageName}:${commit} ${gitHubImageName}:edge"
                                sh "docker push ${gitHubImageName}:edge"
                            }
                        }
                    }
                }
                container('docker') {
                    withCredentials([usernamePassword(
                        credentialsId:'OvertureDockerHub',
                        passwordVariable: 'PASSWORD',
                        usernameVariable: 'USERNAME'
                    )]) {
                        sh "docker login -u $USERNAME -p $PASSWORD"

                        script {
                            if (env.BRANCH_NAME ==~ 'main') { //push edge and commit tags
                                sh "docker tag ${gitHubImageName}:${commit} ${dockerHubImageName}:${version}"
                                sh "docker push ${dockerHubImageName}:${version}"

                                sh "docker tag ${gitHubImageName}:${commit} ${dockerHubImageName}:latest"
                                sh "docker push ${dockerHubImageName}:latest"
                            } else { // push commit tag
                                sh "docker tag ${gitHubImageName}:${commit} ${dockerHubImageName}:${commit}"
                                sh "docker push ${dockerHubImageName}:${commit}"
                            }

                            if (env.BRANCH_NAME ==~ 'develop') { // push edge tag
                                sh "docker tag ${gitHubImageName}:${commit} ${dockerHubImageName}:edge"
                                sh "docker push ${dockerHubImageName}:edge"
                            }
                        }
                    }
                }
            }
        }

        stage('Publish tag to github') {
            when {
                branch 'main'
            }
            steps {
                container('node') {
                    withCredentials([
                        usernamePassword(
                            credentialsId: 'OvertureBioGithub',
                            passwordVariable: 'GIT_PASSWORD',
                            usernameVariable: 'GIT_USERNAME'
                        ),
                    ]) {
                        script {
                            // we still want to run the platform deploy even if this fails, hence try-catch
                            try {
                                sh "git tag ${version}"
                                sh "git push https://${GIT_USERNAME}:${GIT_PASSWORD}@github.com/${gitHubRepo} --tags"
                                sh "curl \
                                -X POST \
                                -H 'Content-type: application/json' \
                                    --data '{ \
                                        \"text\":\"New ${gitHubRepo} published succesfully: v.${version}\
                                        \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                                    }' \
                                ${slackNotificationsUrl}"
                            } catch (err) {
                                echo 'There was an error while publishing packages'
                            }
                        }
                    }
                }
            }
        }

        stage('Deploy to Overture QA') {
            when {
                anyOf {
                    branch 'develop'
                    expression { return params.DEPLOY_TO_DEV }
                }
            }
			steps {
				build(job: "/Overture.bio/provision/helm", parameters: [
						[$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'qa' ],
						[$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'ego'],
						[$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'ego'],
						[$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: "${chartversion}"],
						[$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: "https://overture-stack.github.io/charts-server/"],
						[$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: "false" ],
						[$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${commit}" ]
				])
			}
        }

        stage('Deploy to Overture Staging') {
            when {
                anyOf {
                    branch 'main'
                }
            }
            steps {
				build(job: "/Overture.bio/provision/helm", parameters: [
						[$class: 'StringParameterValue', name: 'OVERTURE_ENV', value: 'staging' ],
						[$class: 'StringParameterValue', name: 'OVERTURE_CHART_NAME', value: 'ego'],
						[$class: 'StringParameterValue', name: 'OVERTURE_RELEASE_NAME', value: 'ego'],
						[$class: 'StringParameterValue', name: 'OVERTURE_HELM_CHART_VERSION', value: "${chartversion}"],
						[$class: 'StringParameterValue', name: 'OVERTURE_HELM_REPO_URL', value: "https://overture-stack.github.io/charts-server/"],
						[$class: 'StringParameterValue', name: 'OVERTURE_HELM_REUSE_VALUES', value: "false" ],
						[$class: 'StringParameterValue', name: 'OVERTURE_ARGS_LINE', value: "--set-string image.tag=${version}" ]
				])
            }
        }
    }

    post {
        fixed {
            script {
                if (env.BRANCH_NAME ==~ /(develop|main|test\S*)/) {
                    sh "curl \
                        -X POST \
                        -H 'Content-type: application/json' \
                        --data '{ \
                            \"text\":\"Build Fixed: ${env.JOB_NAME}#${commit} \
                            \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                        }' \
                        ${slackNotificationsUrl}"
                }
            }
        }

        success {
            script {
                if (env.BRANCH_NAME ==~ /(test\S*)/) {
                    sh "curl \
                        -X POST \
                        -H 'Content-type: application/json' \
                        --data '{ \
                            \"text\":\"Build tested: ${env.JOB_NAME}#${commit} \
                            \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                        }' \
                        ${slackNotificationsUrl}"
                }
            }
        }

        unsuccessful {
            script {
                if (env.BRANCH_NAME ==~ /(develop|main|test\S*)/) {
                    sh "curl \
                        -X POST \
                        -H 'Content-type: application/json' \
                        --data '{ \
                            \"text\":\"Build Failed: ${env.JOB_NAME}#${commit} \
                            \n[Build ${env.BUILD_NUMBER}] (${env.BUILD_URL})\" \
                        }' \
                        ${slackNotificationsUrl}"
                }
            }
        }
    }
}
