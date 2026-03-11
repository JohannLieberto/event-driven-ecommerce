pipeline {
    agent any

    tools {
        maven 'Maven 3'
        jdk 'JDK-17'
    }

    environment {
        MAVEN_OPTS = '-Xmx1024m'
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

        stage('Test & Coverage') {
            steps {
                echo 'Running tests and generating JaCoCo coverage report...'
                sh 'mvn clean verify'
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: '**/target/surefire-reports/TEST-*.xml'
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
                                -Dsonar.login=$SONAR_TOKEN \
                                -Dsonar.host.url=http://sonarqube:9000 \
                                -Dsonar.coverage.jacoco.xmlReportPaths=**/target/site/jacoco/jacoco.xml
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo 'Checking quality gate...'
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
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
                echo 'Deploying to local Docker environment...'
                sh 'docker-compose down --remove-orphans'
                sh 'docker-compose up -d'
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
    }
}
