pipeline {
  agent any

  environment {
    REGISTRY = 'docker.io/rahul5553'
    APP_NAME = 'webapp'
    IMAGE = "${REGISTRY}/${APP_NAME}"
    COMMIT = "${GIT_COMMIT?.take(7)}"
    QA_DELAY_MINUTES  = 1
    UAT_DELAY_MINUTES = 2
    KUBECONFIG_CRED = 'kubeconfig-cred-id'
    DOCKER_CREDS    = 'dockerhub-cred-id'
  }

  stages {

    stage('Checkout') {
      steps {
        checkout scm
      }
    }

    stage('Build & Unit Test (JUnit)') {
      steps {
        bat 'mvn clean test package'
      }
      post {
        always {
          junit 'target/surefire-reports/*.xml'
        }
        failure {
          error "JUnit failed — blocking deploy."
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: "${DOCKER_CREDS}", usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          bat '''
          docker login -u %DOCKER_USER% -p %DOCKER_PASS%
          docker build -t %IMAGE%:%COMMIT% -t %IMAGE%:latest .
          docker push %IMAGE%:%COMMIT%
          docker push %IMAGE%:latest
          '''
        }
      }
    }

    stage('Create Namespaces (idempotent)') {
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KUBECONFIG')]) {
          bat '''
          kubectl apply -f platform_infra/namespaces/dev-namespace.yaml --kubeconfig=%KUBECONFIG%
          kubectl apply -f platform_infra/namespaces/qa-namespace.yaml --kubeconfig=%KUBECONFIG%
          kubectl apply -f platform_infra/namespaces/uat-namespace.yaml --kubeconfig=%KUBECONFIG%
          '''
        }
      }
    }

    stage('Deploy to DEV') {
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KUBECONFIG')]) {
          bat '''
          kubectl apply -k kubernetes/overlays/dev --kubeconfig=%KUBECONFIG%
          '''
        }
      }
    }

    stage('Delay → QA') {
      steps { sleep time: QA_DELAY_MINUTES, unit: 'MINUTES' }
    }

    stage('Deploy to QA') {
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KUBECONFIG')]) {
          bat '''
          kubectl apply -k kubernetes/overlays/qa --kubeconfig=%KUBECONFIG%
          '''
        }
      }
    }

    stage('Delay → UAT') {
      steps { sleep time: UAT_DELAY_MINUTES, unit: 'MINUTES' }
    }

    stage('Deploy to UAT') {
      steps {
        withCredentials([file(credentialsId: "${KUBECONFIG_CRED}", variable: 'KUBECONFIG')]) {
          bat '''
          kubectl apply -k kubernetes/overlays/uat --kubeconfig=%KUBECONFIG%
          '''
        }
      }
    }

    stage('Manual Approval for PROD') {
      steps {
        input message: "Promote build ${env.BUILD_NUMBER} to Production?"
      }
    }
  }
}
