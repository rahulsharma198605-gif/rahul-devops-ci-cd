pipeline {
  agent any

  tools {
    maven 'M3'          // from Global Tool Configuration
    // jdk 'JDK21'      // uncomment if you configured a JDK tool
  }

  environment {
    REGISTRY = 'docker.io/rahul5553'
    APP_NAME = 'webapp'
    IMAGE    = "${REGISTRY}/${APP_NAME}"
    COMMIT   = "${GIT_COMMIT?.take(7)}"
    QA_DELAY_MINUTES  = 1
    UAT_DELAY_MINUTES = 2
    KUBECONFIG_CRED = 'kubeconfig-cred-id'
    DOCKER_CREDS    = 'dockerhub-cred-id'
  }

  stages {
    stage('Checkout') {
      steps { checkout scm }
    }

    stage('Build & Unit Test (JUnit)') {
      steps {
        bat 'mvn -v'
        bat 'mvn -B clean test package'
      }
      post {
        always {
          // allowEmptyResults avoids failure if there are no tests yet
          junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: true
        }
      }
    }

    stage('Docker Build & Push') {
      steps {
        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDS,
                                          usernameVariable: 'DOCKER_USER',
                                          passwordVariable: 'DOCKER_PASS')]) {
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
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG')]) {
          bat '''
            kubectl apply -f platform_infra\\namespaces\\dev-namespace.yaml --kubeconfig=%KUBECONFIG%
            kubectl apply -f platform_infra\\namespaces\\qa-namespace.yaml  --kubeconfig=%KUBECONFIG%
            kubectl apply -f platform_infra\\namespaces\\uat-namespace.yaml  --kubeconfig=%KUBECONFIG%
          '''
        }
      }
    }

    stage('Deploy to DEV') {
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG')]) {
          // Use kubectl’s built-in kustomize (-k). Paths use Windows backslashes.
          bat 'kubectl apply -k kubernetes\\overlays\\dev --kubeconfig=%KUBECONFIG%'
        }
      }
    }

    stage('Delay → QA') { steps { sleep time: env.QA_DELAY_MINUTES as Integer, unit: 'MINUTES' } }

    stage('Deploy to QA') {
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG')]) {
          bat 'kubectl apply -k kubernetes\\overlays\\qa --kubeconfig=%KUBECONFIG%'
        }
      }
    }

    stage('Delay → UAT') {
      steps {
        script {
          def extra = (env.UAT_DELAY_MINUTES as Integer) - (env.QA_DELAY_MINUTES as Integer)
          if (extra > 0) sleep time: extra, unit: 'MINUTES'
        }
      }
    }

    stage('Deploy to UAT') {
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG')]) {
          bat 'kubectl apply -k kubernetes\\overlays\\uat --kubeconfig=%KUBECONFIG%'
        }
      }
    }

    stage('Manual Approval for PROD') {
      steps { input message: "Promote ${env.COMMIT} to PROD?" }
    }
  }
}
