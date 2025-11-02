pipeline {
  agent any
  options { disableConcurrentBuilds(); timestamps(); buildDiscarder(logRotator(numToKeepStr: '30')) }
  environment {
    REGISTRY = 'docker.io/rahul5553'
    APP_NAME = 'webapp'
    IMAGE = "${REGISTRY}/${APP_NAME}"
    COMMIT = "${GIT_COMMIT?.take(7)}"
    QA_DELAY_MINUTES  = 10   // total time from DEV start to QA deploy
    UAT_DELAY_MINUTES = 20   // total time from DEV start to UAT deploy
    KUBECONFIG_CRED = 'kubeconfig-cred-id'
    DOCKER_CREDS    = 'dockerhub-cred-id'
  }
  stages {
    stage('Checkout'){ steps { checkout scm } }
    stage('Build & Unit Test (JUnit)'){
      steps { sh 'mvn -B clean test package' }
      post {
        always { junit 'target/surefire-reports/*.xml' }
        unsuccessful { error 'JUnit failed — blocking deploy.' }
      }
    }
    stage('Docker Build & Push'){
      steps {
        withCredentials([usernamePassword(credentialsId: env.DOCKER_CREDS, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
          sh '''
            echo "$DOCKER_PASS" | docker login -u "$DOCKER_USER" --password-stdin
            docker build -t $IMAGE:$COMMIT -t $IMAGE:latest .
            docker push $IMAGE:$COMMIT
            docker push $IMAGE:latest
          '''
        }
      }
    }
    stage('Create Namespaces (idempotent)'){
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG_FILE')]) {
          sh '''
            export KUBECONFIG=$KUBECONFIG_FILE
            kubectl apply -f platform_infra/namespaces/dev-namespace.yaml || true
            kubectl apply -f platform_infra/namespaces/qa-namespace.yaml || true
            kubectl apply -f platform_infra/namespaces/uat-namespace.yaml || true
          '''
        }
      }
    }
    stage('Deploy to DEV'){
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG_FILE')]) {
          sh '''
            export KUBECONFIG=$KUBECONFIG_FILE
            cd kubernetes/overlays/dev
            kustomize edit set image $IMAGE=$IMAGE:$COMMIT
            kustomize build . | kubectl apply -f -
          '''
        }
      }
    }
    stage('Delay → QA'){ steps { sleep(time: env.QA_DELAY_MINUTES as Integer, unit: 'MINUTES') } }
    stage('Deploy to QA'){
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG_FILE')]) {
          sh '''
            export KUBECONFIG=$KUBECONFIG_FILE
            cd kubernetes/overlays/qa
            kustomize edit set image $IMAGE=$IMAGE:$COMMIT
            kustomize build . | kubectl apply -f -
          '''
        }
      }
    }
    stage('Automation Tests (Selenium placeholder)'){
      steps { echo 'Run Selenium tests against QA here (separate job or dockerized runner).' }
      post { unsuccessful { error 'Automation failed — blocking UAT.' } }
    }
    stage('Delay → UAT'){
      steps {
        script {
          def extra = (env.UAT_DELAY_MINUTES as Integer) - (env.QA_DELAY_MINUTES as Integer)
          if (extra > 0) sleep(time: extra, unit: 'MINUTES')
        }
      }
    }
    stage('Deploy to UAT'){
      steps {
        withCredentials([file(credentialsId: env.KUBECONFIG_CRED, variable: 'KUBECONFIG_FILE')]) {
          sh '''
            export KUBECONFIG=$KUBECONFIG_FILE
            cd kubernetes/overlays/uat
            kustomize edit set image $IMAGE=$IMAGE:$COMMIT
            kustomize build . | kubectl apply -f -
          '''
        }
      }
    }
    stage('Manual Approval for PROD'){
      steps { input message: "Promote ${env.COMMIT} to PROD?" }
    }
  }
}
