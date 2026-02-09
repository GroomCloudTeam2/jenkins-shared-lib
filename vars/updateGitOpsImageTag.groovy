def call(Map args = [:]) {
    // 필수 파라미터
    def repoUrl        = args.repoUrl ?: error("repoUrl is required")
    def branch         = args.branch ?: error("branch is required")
    def services       = args.services ?: error("services is required")
    def imageTag       = args.imageTag ?: error("imageTag is required")
    def valuesBaseDir  = args.valuesBaseDir ?: error("valuesBaseDir is required")
    def workDir        = args.workDir ?: "gitops-repo"

    if (!services || services.isEmpty()) {
        echo "No services to update in GitOps repo"
        return
    }

    withCredentials([
            string(credentialsId: 'git-token', variable: 'GIT_TOKEN')
    ]) {
        sh """
            set -e
            rm -rf ${workDir}
            git clone https://x-access-token:${GIT_TOKEN}@${repoUrl.replace('https://', '')} ${workDir}
            cd ${workDir}
            git checkout ${branch}
        """

        services.each { svc ->
            sh """
                set -e
                cd ${workDir}
                sed -i 's|tag:.*|tag: "${imageTag}"|' ${valuesBaseDir}/${svc}-service/values-dev.yaml
            """
        }

        sh """
            set -e
            cd ${workDir}
            git config user.email "ci-bot@jenkins"
            git config user.name "jenkins"
            git add .
            git commit -m "Update services [${services.join(', ')}] to ${imageTag}" || echo "No changes"
            git push origin ${branch}
        """
    }
}
