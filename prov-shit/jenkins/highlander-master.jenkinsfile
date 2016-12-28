node {
  try {
    notifyBuild()

    stage("Checkout") {
      git "git@github.com:FoxComm/highlander.git"
    }

    stage("Prepare") {
      sh "export GOPATH=/var/lib/buildkite-agent/go"
      sh "export GITHUB_API_TOKEN=20d1fc8b78dca5bf192176f6e69513b769da2c69"
      sh "export STRIPE_API_KEY=sk_test_uvaf3GCFsjCsvvKO7FsQhNRm"
      sh "export ON_SERVER=true"
    }

    stage("Projects") {
      sh "./projects.sh -verbose"
    }

    stage("Build") {
      sh "make build"
    }

    stage("Test") {
      sh "make test"
    }

    stage("Build Docker Images") {
      sh "make docker"
    }

    stage("Push Docker Images") {
      sh "make docker-push"
    }

    stage("Trigger Duncan Builds") {
      build job: "duncan-mccloud"
      build job: "duncan-loves-socks"
      build job: "duncan-is-hungry"
    }
  } catch (e) {
    currentBuild.result = "FAILED"
    throw e
  } finally {
    notifyBuild(currentBuild.result)
  }
}

def notifyBuild(String buildStatus = 'STARTED') {
  // build status of null means successful
  buildStatus =  buildStatus ?: 'SUCCESSFUL'

  def colorCode = '#FF0000'
  def subject = "${buildStatus}: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]'"
  def summary = "${subject} (${env.BUILD_URL})"
  def details = """<p>STARTED: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]':</p>
    <p>Check console output at &QUOT;<a href='${env.BUILD_URL}'>${env.JOB_NAME} [${env.BUILD_NUMBER}]</a>&QUOT;</p>"""

  if (buildStatus == 'STARTED') {
    colorCode = '#FFFF00'
  } else if (buildStatus == 'SUCCESSFUL') {
    colorCode = '#00FF00'
  } else {
    colorCode = '#FF0000'
  }

  slackSend (color: colorCode, message: summary)
}
