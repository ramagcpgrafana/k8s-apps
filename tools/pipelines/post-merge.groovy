def run(helm_home) {
  stage("Dependencies") {
    sh('go run tools/pre-test-local-repos.go')
  }
  def buildId = "${env.BUILD_NUMBER}-${env.GERRIT_CHANGE_NUMBER}-${env.GERRIT_PATCHSET_NUMBER}"
  stage("Run tests") {
    try {
      withCredentials([file(credentialsId: 'kubeconfig', variable: 'KUBECONFIG')]) {
        withEnv([
          'HELM_HOME=' + helm_home,
          'HELM_CMD=' + pwd() + '/helm',
          'KUBECTL_CMD=' + pwd() + '/kubectl',
        ]) {
          if (env.GERRIT_CHANGE_NUMBER == '6417') {
              sh("set -o pipefail; exec 3>&1; go test -v -timeout 90m -args --images --charts --image-repo nexus-scc.ng.mirantis.net:5000/${buildId} --build-images-opts='--no-cache' --verify-version --exclude tweepub,tweetics,kibana,logstash --prefix j${buildId}- 2>&1 3>&- | tee /dev/fd/3 | ./go-junit-report > report.xml 3>&-")
          } else {
              sh("set -o pipefail; exec 3>&1; go test -v -timeout 90m -args --images --charts --image-repo nexus-scc.ng.mirantis.net:5000/${buildId} --build-images-opts='--no-cache' --exclude tweepub,tweetics,kibana,logstash --prefix j${buildId}- 2>&1 3>&- | tee /dev/fd/3 | ./go-junit-report > report.xml 3>&-")
          }
        }
      }
    } finally {
      junit('report.xml')
    }
  }

  stage("Push images") {
    sh("go test -v -timeout 60m -args --charts=false --push --image-repo nexus-scc.ng.mirantis.net:5000/${buildId}")
  }
}
return this;