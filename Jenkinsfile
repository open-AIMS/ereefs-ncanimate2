// Jenkins Pipeline script executed when a change is detected in Github. This script runs the tests
// for all changes. If the change is to the 'production' branch, this script also packages the
// application and install it to the Jenkins Maven local repository

// Syntax: https://jenkins.io/doc/book/pipeline/syntax/
//     https://jenkins.io/doc/book/pipeline/docker/

pipeline {

    // Default agent is any free agent.
    agent any

    parameters {
        choice(name: 'deployTarget', choices: ['', 'Testing', 'Production'], description: 'Choose the AWS account to deploy to. If no account is selected, it will not be deployed to AWS. IMPORTANT: only the "production" branch can be deployed to the "Production" account.')
        string(name: 'executionEnvironment', defaultValue: 'test', description: 'Enter the environment to create. This is used as a suffix for all components, and should be either "test", "prod", or a developer name (eg: "asmith").')
    }

    environment {
        EREEFS_NCANIMATE_FRAME_BRANCH = "${params.deployTarget == 'Production' ? "production" : "main"}"

        MAVEN_REPO = "/workspace/.m2_${params.deployTarget}/repository"


        // AWS-related
        // -----------
        // Credential ID for deploying to AWS.
        AWS_CREDENTIALS_ID_PROD = "jenkins-ereefs-prod-ncanimate" // TODO Create user in AWS eReefs live account
        AWS_CREDENTIALS_ID_TEST = "jenkins-ereefs-test-ncanimate" // TODO Create user in AWS eReefs test account

        //  AWS CloudFormation Id for project.
        AWS_CLOUD_FORMATION_STACKNAME_PREFIX = "ncanimate"

        // The deployment target based on the users' selection.
        AWS_DEPLOY_TARGET = "${params.deployTarget == 'Production' ? 'prod' : 'testing'}"

        // Docker-related
        // --------------
        // The name of the Docker image that will be built to run the compiled app.
        IMAGE_NAME = "ereefs-ncanimate-${params.executionEnvironment}"

        NCANIMATE_APP_NAME = 'ereefs-ncanimate2'
        NCANIMATE_FRAME_APP_NAME = 'ereefs-ncanimate2-frame'

        NCANIMATE_JAR_FIND_PATTERN = "target/${NCANIMATE_APP_NAME}-*-jar-with-dependencies.jar"
        NCANIMATE_FRAME_JAR_FIND_PATTERN = "ereefs-ncanimate2-frame/target/${NCANIMATE_FRAME_APP_NAME}-*-jar-with-dependencies.jar"

        NCANIMATE_JAR_NAME = "${NCANIMATE_APP_NAME}-jar-with-dependencies.jar"
        NCANIMATE_FRAME_JAR_NAME = "${NCANIMATE_FRAME_APP_NAME}-jar-with-dependencies.jar"

        // AWS account ID depending on the target environment
        // Parameters for connecting to the AWS ECR (container repository).
        ECR_PROD_URL = "https://${EREEFS_AWS_PROD_ACCOUNT_ID}.dkr.ecr.${EREEFS_AWS_REGION}.amazonaws.com"
        ECR_TEST_URL = "https://${EREEFS_AWS_TEST_ACCOUNT_ID}.dkr.ecr.${EREEFS_AWS_REGION}.amazonaws.com"
        ECR_CREDENTIALS_PROD = "ecr:${EREEFS_AWS_REGION}:${AWS_CREDENTIALS_ID_PROD}"
        ECR_CREDENTIALS_TEST = "ecr:${EREEFS_AWS_REGION}:${AWS_CREDENTIALS_ID_TEST}"

        // Retrieve the credentials from the Credentials Manager in Jenkins, for accessing Github Packages.
        GITHUB_PACKAGES_CREDENTIALS = credentials('github-packages')
    }

    stages {

        // NOTE: Use "mvn -Dmaven.repo.local=${MAVEN_REPO} ..." to avoid creating a new Maven repository for each project / branch

        stage('Checkout ereefs-ncanimate2-frame') {
            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            steps {
                // Checkout the ncAnimate2 Frame project in a subdirectory.
                dir ('ereefs-ncanimate2-frame') {
                    git branch: "${EREEFS_NCANIMATE_FRAME_BRANCH}",
                            changelog: false,
                            credentialsId: 'github-reader',
                            poll: false,
                            url: 'git@github.com:open-aims/ereefs-ncanimate2-frame.git'
                }
            }

        }

        // Test ereefs-ncanimate2-frame
        stage('Maven test NcAnimate2-frame') {
            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            // Maven will be executed within it's Docker container.
            agent {
                docker {
                    image 'maven:3.6-alpine'
                    args '-u root' // Used to install dependencies
                    reuseNode true
                }
            }

            // Compile the jar package.
            steps {
                dir ('ereefs-ncanimate2-frame') {
                    sh '''
                        apk update
                        apk add ttf-freefont

                        mvn -B -settings maven-settings.xml -DGITHUB_USERNAME=$GITHUB_PACKAGES_CREDENTIALS_USR -DGITHUB_TOKEN=$GITHUB_PACKAGES_CREDENTIALS_PSW -Dmaven.repo.local=${MAVEN_REPO} test clean
                    '''
                }
            }
        }

        // Build ereefs-ncanimate2-frame
        stage('Maven build and package NcAnimate2-frame') {
            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            // Maven will be executed within it's Docker container.
            agent {
                docker {
                    image 'maven:3.6-alpine'
                    reuseNode true
                }
            }

            // Compile the jar package.
            steps {
                dir ('ereefs-ncanimate2-frame') {
                    sh '''
                        mvn -B -settings maven-settings.xml -DGITHUB_USERNAME=$GITHUB_PACKAGES_CREDENTIALS_USR -DGITHUB_TOKEN=$GITHUB_PACKAGES_CREDENTIALS_PSW -Dmaven.repo.local=${MAVEN_REPO} -DskipTests=true clean package
                    '''
                }
            }
        }

        // Test ereefs-ncanimate2
        stage('Maven test NcAnimate2') {
            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            // Maven will be executed within it's Docker container.
            agent {
                docker {
                    image 'maven:3.6-alpine'
                    args '-u root' // Used to install dependencies
                    reuseNode true
                }
            }

            // Compile and install the library.
            steps {
                sh '''
                    apk update
                    apk add ttf-freefont
                    apk add ffmpeg

                    ln -fs ereefs-ncanimate2-frame/target/ereefs-ncanimate2-frame*jar-with-dependencies.jar .
                    mvn -B -settings maven-settings.xml -DGITHUB_USERNAME=$GITHUB_PACKAGES_CREDENTIALS_USR -DGITHUB_TOKEN=$GITHUB_PACKAGES_CREDENTIALS_PSW -Dmaven.repo.local=${MAVEN_REPO} test clean
                    rm ereefs-ncanimate2-frame*jar-with-dependencies.jar
                '''
            }
        }

        // Build ereefs-ncanimate2
        stage('Maven build and package NcAnimate2') {
            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            // Maven will be executed within it's Docker container.
            agent {
                docker {
                    image 'maven:3.6-alpine'
                    reuseNode true
                }
            }

            // Compile and install the library.
            steps {
                sh '''
                    mvn -B -settings maven-settings.xml -DGITHUB_USERNAME=$GITHUB_PACKAGES_CREDENTIALS_USR -DGITHUB_TOKEN=$GITHUB_PACKAGES_CREDENTIALS_PSW -Dmaven.repo.local=${MAVEN_REPO} -DskipTests=true clean package
                '''
            }
        }

        // Build the Docker image.
        stage('Docker build') {

            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            steps {

                script {
                    sh '''
                        cp ${NCANIMATE_JAR_FIND_PATTERN} "target/${NCANIMATE_JAR_NAME}"
                        cp ${NCANIMATE_FRAME_JAR_FIND_PATTERN} "target/${NCANIMATE_FRAME_JAR_NAME}"
                    '''

                    // Build the Docker image.
                    docker.build(IMAGE_NAME, "--build-arg NCANIMATE_JAR_NAME=${NCANIMATE_JAR_NAME} --build-arg NCANIMATE_FRAME_JAR_NAME=${NCANIMATE_FRAME_JAR_NAME} --force-rm .")
                }

            }
        }

        // Deploy the Docker image and update the CloudFormation Stack.
        stage('Deploy to AWS "TEST" environment') {

            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Testing'
                    }
                }
            }

            steps {

                script {

                    // Update the CloudFormation Stack.
                    withAWS(region: EREEFS_AWS_REGION, credentials: AWS_CREDENTIALS_ID_TEST) {
                        cfnUpdate(
                            stack: "${AWS_CLOUD_FORMATION_STACKNAME_PREFIX}-${params.executionEnvironment}",
                            params: ["Environment=${params.executionEnvironment}", "EcrUserId=${AWS_CREDENTIALS_ID_TEST}"],
                            tags: ["deployTarget=${params.deployTarget}","executionEnvironment=${params.executionEnvironment}"],
                            file: 'cloudformation.yaml',
                            timeoutInMinutes: 10,
                            pollInterval: 5000
                        )
                    }

                    // Credentials for connecting to the AWS ECR repository.
                    docker.withRegistry(ECR_TEST_URL, ECR_CREDENTIALS_TEST) {

                        // Deploy the Docker image.
                        docker.image(IMAGE_NAME).push(BUILD_NUMBER)
                        docker.image(IMAGE_NAME).push("latest")
                    }

                }
            }
        }


        // Deploy the Docker image and update the CloudFormation Stack.
        stage('Deploy to AWS "PRODUCTION" environment') {

            when {
                anyOf {
                    expression {
                        return params.deployTarget == 'Production' && env.BRANCH_NAME == 'production'
                    }
                }
            }

            steps {

                script {

                    // Update the CloudFormation Stack.
                    withAWS(region: EREEFS_AWS_REGION, credentials: AWS_CREDENTIALS_ID_PROD) {
                        cfnUpdate(
                             stack: "${AWS_CLOUD_FORMATION_STACKNAME_PREFIX}-${params.executionEnvironment}",
                             params: ["Environment=${params.executionEnvironment}", "EcrUserId=${AWS_CREDENTIALS_ID_PROD}"],
                             tags: ["deployTarget=${params.deployTarget}","executionEnvironment=${params.executionEnvironment}"],
                             file: 'cloudformation.yaml',
                             timeoutInMinutes: 10,
                             pollInterval: 5000
                        )
                    }

                    // Credentials for connecting to the AWS ECR repository.
                    docker.withRegistry(ECR_PROD_URL, ECR_CREDENTIALS_PROD) {

                        // Deploy the Docker image.
                        docker.image(IMAGE_NAME).push(BUILD_NUMBER)
                        docker.image(IMAGE_NAME).push("latest")
                    }

                }
            }
        }
    }

    // Post-processing.
    post {

        cleanup {

            sh'''

                # Remove any Docker containers that are not in use.
                docker container prune --force

                # Remove any Docker images that are not in use.
                docker image prune --force

                # Remote any Docker networks that are not in use.
                docker network prune --force

            '''
        }

    }

}
