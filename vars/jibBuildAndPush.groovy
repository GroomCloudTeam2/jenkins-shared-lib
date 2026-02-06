def call(Map args = [:]) {
    def services    = args.services ?: error("services is required")
    def imageTag    = args.imageTag ?: error("imageTag is required")
    def ecrRegistry = args.ecrRegistry ?: error("ecrRegistry is required")

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    if (!env.ECR_PASSWORD) {
        error("ECR_PASSWORD is not set. Did you run ECR Login stage?")
    }

    parallel services.collectEntries { svc ->
        [(svc): {
            def image = "${ecrRegistry}/goorm-${svc}:${imageTag}"
            echo "Jib build & push -> ${image}"

            sh """
              set -e
              ./gradlew :service:${svc}:jib --no-daemon \\
                -Djib.to.image=${image} \\
                -Djib.to.auth.username=AWS \\
                -Djib.to.auth.password=${env.ECR_PASSWORD}
            """
        }]
    }
}
