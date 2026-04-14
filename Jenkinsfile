pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'HiteshKhade'
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
                            jacoco(
                                execPattern: 'order-service/target/jacoco.exec',
                                classPattern: 'order-service/target/classes',
                                sourcePattern: 'order-service/src/main/java',
                                exclusionPattern: '**/dto/**,**/entity/**,**/model/**,**/*Application.class'
                            )
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
                            jacoco(
                                execPattern: 'inventory-service/target/jacoco.exec',
                                classPattern: 'inventory-service/target/classes',
                                sourcePattern: 'inventory-service/src/main/java',
                                exclusionPattern: '**/dto/**,**/entity/**,**/model/**,**/*Application.class'
                            )
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
                            jacoco(
                                execPattern: 'payment-service/target/jacoco.exec',
                                classPattern: 'payment-service/target/classes',
                                sourcePattern: 'payment-service/src/main/java',
                                exclusionPattern: '**/dto/**,**/entity/**,**/model/**,**/*Application.class'
                            )
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
                            jacoco(
                                execPattern: 'shipping-service/target/jacoco.exec',
                                classPattern: 'shipping-service/target/classes',
                                sourcePattern: 'shipping-service/src/main/java',
                                exclusionPattern: '**/dto/**,**/entity/**,**/model/**,**/*Application.class'
                            )
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
                            jacoco(
                                execPattern: 'notification-service/target/jacoco.exec',
                                classPattern: 'notification-service/target/classes',
                                sourcePattern: 'notification-service/src/main/java',
                                exclusionPattern: '**/dto/**,**/entity/**,**/model/**,**/*Application.class'
                            )
                        }
                    }
                }
            }
        }

        stage('Start Infrastructure') {
            steps {
                echo '=== Starting persistent infra (Kafka, Zookeeper, Postgres) ==='
                sh 'docker compose up -d zookeeper kafka kafka-ui postgres'

                echo '=== Waiting for Kafka to become fully healthy ==='
                sh '''
                    RETRIES=24
                    COUNT=0
                    until docker exec kafka kafka-topics.sh --bootstrap-server localhost:9092 --list > /dev/null 2>&1; do
                        COUNT=$((COUNT + 1))
                        if [ $COUNT -ge $RETRIES ]; then
                            echo "ERROR: Kafka did not become ready after 120 seconds. Aborting."
                            docker compose logs kafka
                            exit 1
                        fi
                        echo "Kafka not ready yet... attempt $COUNT/$RETRIES. Retrying in 5s."
                        sleep 5
                    done
                    echo "Kafka is ready after $((COUNT * 5))s."
                '''

                echo '=== Building and starting application services ==='
                sh 'docker compose up -d --build eureka-server api-gateway order-service inventory-service payment-service shipping-service notification-service'

                echo '=== Waiting for all Spring services to register with Eureka ==='
                sh 'sleep 45'
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
                echo '=== Tearing down app services (keeping infra volumes) ==='
                sh 'docker compose down'
            }
        }

        stage('Docker Build & Push') {
            when {
                branch 'main'
            }
            steps {
                echo '=== Building and pushing Docker images to DockerHub (HiteshKhade) ==='
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh 'echo $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
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
            echo '=== Pipeline FAILED - check logs above ==='
            sh 'docker compose logs --tail=50 || true'
        }
        always {
            cleanWs()
        }
    }
}
