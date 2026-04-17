pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = 'hiteshkhade'
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
                sh '''#!/bin/sh
                    mvn clean package -DskipTests \
                        -pl eureka-server,api-gateway,order-service,inventory-service,payment-service,shipping-service,notification-service \
                        -Dmaven.repo.local=${MAVEN_CACHE} \
                        --no-transfer-progress \
                        -T 1C
                '''
            }
        }

        stage('Unit Tests') {
            parallel {
                stage('Test order-service') {
                    steps {
                        sh '#!/bin/sh\nmvn test -pl order-service --no-transfer-progress -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh '#!/bin/sh\nmvn test -pl inventory-service --no-transfer-progress -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh '#!/bin/sh\nmvn test -pl payment-service --no-transfer-progress -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh '#!/bin/sh\nmvn test -pl shipping-service --no-transfer-progress -Dmaven.repo.local=${MAVEN_CACHE}'
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
                        sh '#!/bin/sh\nmvn test -pl notification-service --no-transfer-progress -Dmaven.repo.local=${MAVEN_CACHE}'
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

        stage('SonarCloud Analysis') {
            steps {
                echo '=== Running SonarCloud analysis ==='
                withSonarQubeEnv('SonarCloud') {
                    sh '''#!/bin/sh
                        mvn sonar:sonar \
                            -Dmaven.repo.local=${MAVEN_CACHE} \
                            --no-transfer-progress \
                            -Dsonar.branch.name=main
                    '''
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo '=== Waiting for SonarCloud Quality Gate ==='
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

        stage('Start Infrastructure') {
            steps {
                echo '=== Force removing any stale containers ==='
                sh '#!/bin/sh\ndocker rm -f zookeeper kafka kafka-ui postgres eureka-server api-gateway order-service inventory-service payment-service shipping-service notification-service 2>/dev/null || true'

                echo '=== Cleaning up any previous Docker Compose state ==='
                sh '#!/bin/sh\ndocker compose -f docker-compose.yml down -v --remove-orphans || true'

                echo '=== Starting all services via Docker Compose ==='
                withCredentials([string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET')]) {
                    sh '#!/bin/sh\ndocker compose -f docker-compose.yml up -d --build'
                }

                echo '=== Waiting for Kafka ==='
                sh '''#!/bin/sh
set -e
RETRIES=36
COUNT=0
until docker exec kafka bash -c "cat /dev/null > /dev/tcp/localhost/9092" >/dev/null 2>&1; do
    COUNT=$((COUNT+1))
    if [ $COUNT -ge $RETRIES ]; then
        echo "ERROR: Kafka did not become ready after 180 seconds. Aborting."
        docker compose -f docker-compose.yml logs kafka
        exit 1
    fi
    echo "Kafka not ready yet... $COUNT/$RETRIES. Retrying in 5s."
    sleep 5
done
echo "Kafka is ready"
'''

                echo '=== Waiting for Postgres ==='
                sh '''#!/bin/sh
set -e
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
echo "Postgres is ready"
'''

                echo '=== Waiting for all services to be healthy ==='
                sh '''#!/bin/sh
set -e
wait_for_healthy() {
    NAME=$1
    RETRIES=48
    COUNT=0
    until [ "$(docker inspect --format="{{.State.Health.Status}}" "$NAME" 2>/dev/null)" = "healthy" ]; do
        COUNT=$((COUNT+1))
        if [ $COUNT -ge $RETRIES ]; then
            echo "ERROR: $NAME did not become healthy after $((RETRIES * 5))s"
            docker compose -f docker-compose.yml logs "$NAME"
            exit 1
        fi
        echo "$NAME not ready yet... $COUNT/$RETRIES"
        sleep 5
    done
    echo "$NAME is ready"
}

wait_for_healthy eureka-server
wait_for_healthy api-gateway
wait_for_healthy order-service
wait_for_healthy inventory-service
wait_for_healthy payment-service
wait_for_healthy shipping-service
wait_for_healthy notification-service

echo "=== Verifying gateway routing for all services ==="
verify_route() {
    SVC=$1
    ROUTE=$2
    RETRIES=24
    COUNT=0
    until docker exec api-gateway wget -qO- "http://localhost:8080${ROUTE}" >/dev/null 2>&1; do
        COUNT=$((COUNT+1))
        if [ $COUNT -ge $RETRIES ]; then
            echo "ERROR: Gateway cannot route to $SVC after $((RETRIES * 5))s"
            exit 1
        fi
        echo "Gateway -> $SVC not ready yet... $COUNT/$RETRIES"
        sleep 5
    done
    echo "Gateway -> $SVC routing verified"
}

verify_route order-service     /api/orders/health
verify_route inventory-service /api/inventory/health
verify_route payment-service   /api/payments/health
verify_route shipping-service  /api/shipments/health
'''
            }
        }

        stage('Karate API Tests') {
            steps {
                echo '=== Running Karate API and E2E tests ==='
                sh '#!/bin/sh\nmvn verify -pl karate-tests -Dkarate.env=ci -Dskip.karate=false --no-transfer-progress -Dmaven.repo.local=${MAVEN_CACHE}'
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
                sh '#!/bin/sh\ndocker compose -f docker-compose.yml down -v'
            }
        }

        stage('Docker Build & Push') {
            steps {
                echo '=== Building and pushing Docker images to DockerHub ==='
                script {
                    withCredentials([usernamePassword(
                        credentialsId: 'dockerhub-credentials',
                        usernameVariable: 'DOCKER_USER',
                        passwordVariable: 'DOCKER_PASS'
                    )]) {
                        sh '#!/bin/sh\necho $DOCKER_PASS | docker login -u $DOCKER_USER --password-stdin'
                        sh "#!/bin/sh\ndocker build -f eureka-server/Dockerfile -t ${DOCKER_REGISTRY}/eureka-server:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/eureka-server:${BUILD_NUMBER} ${DOCKER_REGISTRY}/eureka-server:latest && docker push ${DOCKER_REGISTRY}/eureka-server:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/eureka-server:latest"
                        sh "#!/bin/sh\ndocker build -f api-gateway/Dockerfile -t ${DOCKER_REGISTRY}/api-gateway:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/api-gateway:${BUILD_NUMBER} ${DOCKER_REGISTRY}/api-gateway:latest && docker push ${DOCKER_REGISTRY}/api-gateway:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/api-gateway:latest"
                        sh "#!/bin/sh\ndocker build -f order-service/Dockerfile -t ${DOCKER_REGISTRY}/order-service:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/order-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/order-service:latest && docker push ${DOCKER_REGISTRY}/order-service:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/order-service:latest"
                        sh "#!/bin/sh\ndocker build -f inventory-service/Dockerfile -t ${DOCKER_REGISTRY}/inventory-service:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/inventory-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/inventory-service:latest && docker push ${DOCKER_REGISTRY}/inventory-service:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/inventory-service:latest"
                        sh "#!/bin/sh\ndocker build -f payment-service/Dockerfile -t ${DOCKER_REGISTRY}/payment-service:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/payment-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/payment-service:latest && docker push ${DOCKER_REGISTRY}/payment-service:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/payment-service:latest"
                        sh "#!/bin/sh\ndocker build -f shipping-service/Dockerfile -t ${DOCKER_REGISTRY}/shipping-service:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/shipping-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/shipping-service:latest && docker push ${DOCKER_REGISTRY}/shipping-service:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/shipping-service:latest"
                        sh "#!/bin/sh\ndocker build -f notification-service/Dockerfile -t ${DOCKER_REGISTRY}/notification-service:${BUILD_NUMBER} . && docker tag ${DOCKER_REGISTRY}/notification-service:${BUILD_NUMBER} ${DOCKER_REGISTRY}/notification-service:latest && docker push ${DOCKER_REGISTRY}/notification-service:${BUILD_NUMBER} && docker push ${DOCKER_REGISTRY}/notification-service:latest"
                    }
                }
            }
        }

        stage('Pull & Run from DockerHub') {
            steps {
                echo '=== Pulling latest images from DockerHub and starting containers ==='
                sh '''#!/bin/sh
set -e
docker pull hiteshkhade/eureka-server:latest
docker pull hiteshkhade/api-gateway:latest
docker pull hiteshkhade/order-service:latest
docker pull hiteshkhade/inventory-service:latest
docker pull hiteshkhade/payment-service:latest
docker pull hiteshkhade/shipping-service:latest
docker pull hiteshkhade/notification-service:latest
echo "All images pulled successfully"
'''
                withCredentials([string(credentialsId: 'jwt-secret', variable: 'JWT_SECRET')]) {
                    sh '''#!/bin/sh
set -e
echo "=== Starting containers from pulled DockerHub images ==="
docker compose -f docker-compose.yml up -d --no-build
echo "=== Containers started ==="
docker ps --format "table {{.Names}}\t{{.Status}}\t{{.Ports}}"
'''
                }
            }
        }

    }

    post {
        success {
            echo '=== Pipeline PASSED ==='
        }
        failure {
            echo '=== Pipeline FAILED — dumping logs ==='
            sh '#!/bin/sh\ndocker compose -f docker-compose.yml logs --tail=100 || true'
            sh '#!/bin/sh\ndocker compose -f docker-compose.yml down -v --remove-orphans || true'
        }
        always {
            cleanWs()
        }
    }
}
