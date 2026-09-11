pipeline {

    agent any

    environment {
        AWS_REGION = 'ap-south-1'
        ECR_REGISTRY = '196893792695.dkr.ecr.ap-south-1.amazonaws.com'
        ECR_REPOSITORY = 'counselx-auth'
        IMAGE_NAME = "${ECR_REGISTRY}/${ECR_REPOSITORY}:${BUILD_NUMBER}"
    }

    stages {

        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Maven Build') {
            steps {
                sh '''
                    chmod +x mvnw
                    ./mvnw clean package -DskipTests
                '''
            }
        }

        stage('Build Image') {
            steps {
                sh '''
                    ./mvnw spring-boot:build-image \
                    -DskipTests \
                    "-Dspring-boot.build-image.imageName=counselx-auth:${BUILD_NUMBER}"
                '''
            }
        }

        stage('ECR Login') {
            steps {
                sh '''
                    aws ecr get-login-password \
                    --region ${AWS_REGION} | \
                    docker login \
                    --username AWS \
                    --password-stdin \
                    ${ECR_REGISTRY}
                '''
            }
        }

        stage('Push Image') {
            steps {
                sh '''
                    docker tag \
                    counselx-auth:${BUILD_NUMBER} \
                    ${IMAGE_NAME}

                    docker push ${IMAGE_NAME}
                '''
            }
        }
    }

    post {
        success {
            echo 'CounselX Auth Service CI SUCCESS ✅'
        }

        failure {
            echo 'CounselX Auth Service CI FAILED ❌'
        }
    }
}