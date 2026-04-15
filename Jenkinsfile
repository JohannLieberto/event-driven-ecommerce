pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'HiteshKhade'
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
        KARATE_ENV = 'ci'
        MAVEN_CACHE = "${env.HOME}/.m2/repository"
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
                sh 'mvn clean package -DskipTests -pl eureka-server,api-gateway,order-service,inventory-service,payment-service,shipping-service,notification-service -Dmaven.repo.local=${MAVEN_CACHE}'
            }
        }

        stage('Unit Tests') {
            parallel {
                stage('Test order-service') {
                    steps {
                        sh 'mvn test -pl order-service -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh 'mvn test -pl inventory-service -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh 'mvn test -pl payment-service -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh 'mvn test -pl shipping-service -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh 'mvn test -pl notification-service -Dmaven.repo.local=${MAVEN_CACHE}'
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
                echo '=== Cleaning up any previous Docker Compose state ==='
                sh 'docker compose -f docker-compose.yml down -v --remove-orphans || true'

                echo '=== Starting Kafka, Postgres, Zookeeper and all services via Docker Compose ==='
                sh 'docker compose -f docker-compose.yml up -d --build'

                echo '=== Waiting for infrastructure to be ready ==='
                sh '''
                    echo "=== Waiting for Kafka ==="
                    RETRIES=36
                    COUNT=0
                    until docker exec kafka bash -c 'cat /dev/null > /dev/tcp/localhost/9092' >/dev/null 2>&1; do
                        COUNT=$((COUNT+1))
                        if [ $COUNT -ge $RETRIES ]; then
                            echo "ERROR: Kafka did not become ready after 180 seconds. Aborting."
                            docker compose -f docker-compose.yml logs kafka
                            exit 1
                        fi
                        echo "Kafka not ready yet... $COUNT/$RETRIES. Retrying in 5s."
                        sleep 5
                    done
                    echo "Kafka is ready ✅"

                    echo "=== Waiting for Postgres ==="
                    RETRIES=20
                    COUNT=0
                    until docker exec postgres pg_isready -U postgres >/dev/null 2>&1; do
                        COUNT=$((COUNT+1))
                        if [ $COUNT -ge $RETRIES ]; then
                            echo "ERROR: Postgres did not become ready. Aborting."
                            docker compose -f docker-compose.yml logs postgres
                            exit 1
                        fi
                        echo "Postgres not ready yet... $COUNT/$RETRIES. Retrying in 3s."
                        sleep 3
                    done
                    echo "Postgres is ready ✅"

                    echo "=== Waiting for Eureka Server ==="
                    RETRIES=24
                    COUNT=0
                    until docker inspect --format='{{.State.Health.Status}}' eureka-server 2>/dev/null | grep -q 'healthy'; do
                        COUNT=$((COUNT+1))
                        if [ $COUNT -ge $RETRIES ]; then
                            echo "ERROR: Eureka Server did not become ready. Aborting."
                            docker compose -f docker-compose.yml logs eureka-server
                            exit 1
                        fi
                        echo "Eureka not ready yet... $COUNT/$RETRIES. Retrying in 5s."
                        sleep 5
                    done
                    echo "Eureka is ready ✅"

                    echo "=== Waiting for Application Services ==="
                    for svc in api-gateway order-service inventory-service payment-service shipping-service notification-service; do
                        COUNT=0
                        RETRIES=24
                        until docker inspect --format='{{.State.Health.Status}}' "$svc" 2>/dev/null | grep -q 'healthy'; do
                            COUNT=$((COUNT+1))
                            if [ $COUNT -ge $RETRIES ]; then
                                echo "ERROR: $svc did not become healthy after $((RETRIES * 5))s. Aborting."
                                docker compose -f docker-compose.yml logs "$svc"
                                exit 1
                            fi
                            echo "$svc not ready (attempt $COUNT/$RETRIES)..."
                            sleep 5
                        done
                        echo "$svc is UP ✅"
                    done

                    echo "=== Waiting for services to register in Eureka ==="
                    SERVICES="ORDER-SERVICE INVENTORY-SERVICE PAYMENT-SERVICE SHIPPING-SERVICE NOTIFICATION-SERVICE"
                    MAX_ATTEMPTS=20
                    for SVC in $SERVICES; do
                        COUNT=0
                        echo "Waiting for $SVC to appear in Eureka registry..."
                        until \
                            curl -sf \
                                -H "Accept: application/json" \
                                "http://localhost:8761/eureka/apps/${SVC}" \
                            | python3 -c "
import sys, json
try:
    d = json.load(sys.stdin)
    status = d['application']['instance'][0]['status']
    sys.exit(0 if status == 'UP' else 1)
except Exception:
    sys.exit(1)
" 2>/dev/null; do
                            COUNT=$((COUNT+1))
                            if [ $COUNT -ge $MAX_ATTEMPTS ]; then
                                echo "ERROR: $SVC did not register in Eureka after $((MAX_ATTEMPTS * 15))s. Aborting."
                                docker compose -f docker-compose.yml logs eureka-server
                                exit 1
                            fi
                            echo "$SVC not in Eureka yet... $COUNT/$MAX_ATTEMPTS. Retrying in 15s."
                            sleep 15
                        done
                        echo "$SVC registered in Eureka ✅"
                    done

                    echo "=== All Services Ready ✅ ==="
                '''
            }
        }

        stage('Karate API Tests') {
            steps {
                echo '=== Running Karate API and E2E tests ==='
                sh 'mvn verify -pl karate-tests -Dkarate.env=ci -Dskip.karate=false -Dmaven.repo.local=${MAVEN_CACHE}'
            }
            post {
                always {
                    junit 'karate-tests/target/failsafe-reports/*.xml'
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
                sh 'docker compose -f docker-compose.yml down -v'
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
            sh 'docker compose -f docker-compose.yml logs --tail=50 || true'
        }
        always {
            cleanWs()
        }
    }
}
