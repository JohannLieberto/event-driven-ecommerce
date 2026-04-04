pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'your-dockerhub-username'
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
        KARATE_ENV = 'ci'
    }

    stages {

        stage('Checkout') {
            steps {
                echo '=== Checking out source code ==='
                checkout scm
            }
        }

        stage('Build All Services') {
            steps {
                echo '=== Building all microservices ==='
                sh 'mvn clean package -DskipTests -pl eureka-server,api-gateway,order-service,inventory-service,payment-service,shipping-service,notification-service'
            }
        }

        stage('Unit Tests') {
            parallel {
                stage('Test order-service') {
                    steps {
                        sh 'mvn test -pl order-service'
                    }
                    post {
                        always {
                            junit 'order-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Test inventory-service') {
                    steps {
                        sh 'mvn test -pl inventory-service'
                    }
                    post {
                        always {
                            junit 'inventory-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Test payment-service') {
                    steps {
                        sh 'mvn test -pl payment-service'
                    }
                    post {
                        always {
                            junit 'payment-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Test shipping-service') {
                    steps {
                        sh 'mvn test -pl shipping-service'
                    }
                    post {
                        always {
                            junit 'shipping-service/target/surefire-reports/*.xml'
                        }
                    }
                }
                stage('Test notification-service') {
                    steps {
                        sh 'mvn test -pl notification-service'
                    }
                    post {
                        always {
                            junit 'notification-service/target/surefire-reports/*.xml'
                        }
                    }
                }
            }
        }

        stage('Start Infrastructure') {
            steps {
                echo '=== Starting Kafka, Postgres, and all services via Docker Compose ==='
                sh 'docker-compose up -d --build'
                sh 'echo "Waiting 60s for services to start..."'
                sh 'sleep 60'
            }
        }

        stage('Karate API Tests') {
            steps {
                echo '=== Running Karate API and E2E tests ==='
                sh 'mvn test -pl karate-tests -Dkarate.env=ci'
            }
            post {
                always {
                    junit 'karate-tests/target/surefire-reports/*.xml'
                    publishHTML(target: [
                        allowMissing: true,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'karate-tests/target/karate-reports',
                        reportFiles: 'karate-summary.html',
                        reportName: 'Karate Test Report'
                    ])
                }
            }
        }

        stage('Stop Infrastructure') {
            steps {
                echo '=== Tearing down Docker Compose ==='
                sh 'docker-compose down -v'
            }
        }

        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            steps {
                echo '=== Building and pushing Docker images ==='
                script {
                    def services = [
                        'eureka-server', 'api-gateway', 'order-service',
                        'inventory-service', 'payment-service',
                        'shipping-service', 'notification-service'
                    ]
                    services.each { svc ->
                        sh "docker build -t ${DOCKER_REGISTRY}/${svc}:${BUILD_NUMBER} ./${svc}"
                        sh "docker tag ${DOCKER_REGISTRY}/${svc}:${BUILD_NUMBER} ${DOCKER_REGISTRY}/${svc}:latest"
                        sh "docker push ${DOCKER_REGISTRY}/${svc}:${BUILD_NUMBER}"
                        sh "docker push ${DOCKER_REGISTRY}/${svc}:latest"
                    }
                }
            }
        }

        stage('Deploy to Kubernetes') {
            when {
                branch 'main'
            }
            steps {
                echo '=== Deploying to Kubernetes ==='
                sh 'kubectl apply -f k8s/'
                sh "kubectl set image deployment/order-service order-service=${DOCKER_REGISTRY}/order-service:${BUILD_NUMBER} -n ecommerce"
                sh "kubectl set image deployment/inventory-service inventory-service=${DOCKER_REGISTRY}/inventory-service:${BUILD_NUMBER} -n ecommerce"
                sh "kubectl set image deployment/payment-service payment-service=${DOCKER_REGISTRY}/payment-service:${BUILD_NUMBER} -n ecommerce"
                sh "kubectl set image deployment/shipping-service shipping-service=${DOCKER_REGISTRY}/shipping-service:${BUILD_NUMBER} -n ecommerce"
                sh "kubectl set image deployment/notification-service notification-service=${DOCKER_REGISTRY}/notification-service:${BUILD_NUMBER} -n ecommerce"
                sh 'kubectl rollout status deployment/order-service -n ecommerce'
                sh 'kubectl rollout status deployment/payment-service -n ecommerce'
                sh 'kubectl rollout status deployment/shipping-service -n ecommerce'
                sh 'kubectl rollout status deployment/notification-service -n ecommerce'
            }
        }
    }

    post {
        success {
            echo '=== Pipeline PASSED ==='
        }
        failure {
            echo '=== Pipeline FAILED ==='
        }
        always {
            cleanWs()
        }
    }
}
