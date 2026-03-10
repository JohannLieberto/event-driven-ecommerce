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
<<<<<<< HEAD
                echo '🔗 Running integration tests...'
                sh 'mvn verify'
=======
                echo 'Running integration tests...'
                sh 'mvn verify -DskipUnitTests'
>>>>>>> 13954f99f3d3dfea4c96c85cbcbd77ddd29c736f
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
<<<<<<< HEAD
    steps {
        echo 'Running SonarQube analysis...'
        withCredentials([string(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN')]) {
            withSonarQubeEnv('SonarQube-Local') {
                sh '''
                    mvn sonar:sonar \
                        -Dsonar.projectKey=event-driven-ecommerce \
                        -Dsonar.projectName="Event-Driven E-Commerce" \
                        -Dsonar.login=$SONAR_TOKEN
                '''
            }
        }
    }
}

        stage('Quality Gate') {
            steps {
                echo '🚦 Checking quality gate...'
=======
            steps {
                echo 'Running SonarQube analysis...'
                withCredentials([file(credentialsId: 'sonarqube-token', variable: 'SONAR_TOKEN_FILE')]) {
                    withSonarQubeEnv('SonarQube-Local') {
                        sh '''
                            SONAR_TOKEN=$(cat $SONAR_TOKEN_FILE)
                            mvn sonar:sonar \
                                -Dsonar.projectKey=event-driven-ecommerce \
                                -Dsonar.projectName="Event-Driven E-Commerce" \
                                -Dsonar.login=$SONAR_TOKEN
                        '''
                    }
                }
            }
        }

        stage('Quality Gate') {
            steps {
                echo 'Checking quality gate...'
>>>>>>> 13954f99f3d3dfea4c96c85cbcbd77ddd29c736f
                timeout(time: 5, unit: 'MINUTES') {
                    waitForQualityGate abortPipeline: true
                }
            }
        }

<<<<<<< HEAD
                stage('Build Docker Images') {
            steps {
                echo '🐳 Building Docker images...'
                script {
                    sh 'docker build -t event-driven-ecommerce/order-service:${BUILD_NUMBER} order-service/'
                    sh 'docker build -t event-driven-ecommerce/inventory-service:${BUILD_NUMBER} inventory-service/'
                    sh 'docker build -t event-driven-ecommerce/eureka-server:${BUILD_NUMBER} eureka-server/'
                    sh 'docker build -t event-driven-ecommerce/config-server:${BUILD_NUMBER} config-server/'
                    sh 'docker build -t event-driven-ecommerce/api-gateway:${BUILD_NUMBER} api-gateway/'
                }
=======
        stage('Build Docker Images') {
            steps {
                echo 'Building Docker images...'
                sh "docker build -t event-driven-ecommerce/order-service:${BUILD_NUMBER} order-service/"
                sh "docker build -t event-driven-ecommerce/inventory-service:${BUILD_NUMBER} inventory-service/"
                sh "docker build -t event-driven-ecommerce/eureka-server:${BUILD_NUMBER} eureka-server/"
                sh "docker build -t event-driven-ecommerce/config-server:${BUILD_NUMBER} config-server/"
                sh "docker build -t event-driven-ecommerce/api-gateway:${BUILD_NUMBER} api-gateway/"
>>>>>>> 13954f99f3d3dfea4c96c85cbcbd77ddd29c736f
            }
        }

        stage('Deploy to Local Environment') {
            steps {
<<<<<<< HEAD
                echo '🚀 Deploying to local Docker environment...'
                sh 'docker-compose down'
                sh 'docker-compose up -d'
            }
        }

=======
                echo 'Deploying to local Docker environment...'
                sh 'docker-compose down --remove-orphans'
                sh 'docker-compose up -d'
            }
        }
>>>>>>> 13954f99f3d3dfea4c96c85cbcbd77ddd29c736f
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
