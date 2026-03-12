pipeline {
    agent any

    tools {
        maven 'Maven 3'
        jdk 'JDK-17'
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m -Dorg.jenkinsci.plugins.durabletask.BourneShellScript.HEARTBEAT_CHECK_INTERVAL=86400'
        COMPOSE_PROJECT_NAME = 'ecommerce-ci'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Checking out source code...'
                checkout scm
            }
        }

        stage('Build') {
            steps {
                echo 'Building all modules...'
                sh 'mvn clean compile -DskipTests'
            }
        }

        stage('Unit Tests') {
            steps {
                echo 'Running unit tests...'
                sh 'mvn test'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Integration Tests') {
            steps {
                echo 'Running integration tests...'
                sh 'mvn verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
                }
            }
        }

        stage('Package') {
            steps {
                echo 'Packaging applications...'
                sh 'mvn package -DskipTests'
            }
        }

        stage('Code Quality Analysis') {
            steps {
                echo 'Running SonarQube analysis...'
                withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
                    withSonarQubeEnv('SonarQube-Local') {
                        sh '''
                            mvn sonar:sonar \
                                -Dsonar.projectKey=event-driven-ecommerce \
                                -Dsonar.projectName="Event-Driven E-Commerce" \
                                -Dsonar.token=$SONAR_TOKEN \
                                -Dsonar.coverage.jacoco.xmlReportPaths=target/site/jacoco/jacoco.xml,order-service/target/site/jacoco/jacoco.xml,inventory-service/target/site/jacoco/jacoco.xml,api-gateway/target/site/jacoco/jacoco.xml,eureka-server/target/site/jacoco/jacoco.xml,config-server/target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo 'Checking quality gate...'
                sleep(time: 30, unit: 'SECONDS')
                timeout(time: 15, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: false
                }
            }
        }

        stage('Build Docker Images') {
            steps {
                echo 'Building Docker images...'
                sh "docker build -t event-driven-ecommerce/order-service:${BUILD_NUMBER} order-service/"
                sh "docker build -t event-driven-ecommerce/inventory-service:${BUILD_NUMBER} inventory-service/"
                sh "docker build -t event-driven-ecommerce/eureka-server:${BUILD_NUMBER} eureka-server/"
                sh "docker build -t event-driven-ecommerce/config-server:${BUILD_NUMBER} config-server/"
                sh "docker build -t event-driven-ecommerce/api-gateway:${BUILD_NUMBER} api-gateway/"
            }
        }

        stage('Deploy to Local Environment') {
            steps {
                echo 'Deploying CI app services (project: ecommerce-ci)...'
                sh 'docker compose -p ecommerce-ci up -d --force-recreate --no-deps order-service inventory-service eureka-server config-server api-gateway order-db inventory-db'
            }
        }
    }

    post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed! Check logs above.'
        }
        always {
            echo 'Publishing coverage report...'
            publishHTML([
                reportDir: 'order-service/target/site/jacoco',
                reportFiles: 'index.html',
                reportName: 'Order Service Coverage Report',
                keepAll: true,
                alwaysLinkToLastBuild: true,
                allowMissing: true
            ])
        }
    }
}
