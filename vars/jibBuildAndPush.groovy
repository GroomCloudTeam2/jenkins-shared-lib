def call(Map args = [:]) {
    def services    = args.services ?: error("services is required")
    def imageTag    = args.imageTag ?: error("imageTag is required")
    def ecrRegistry = args.ecrRegistry ?: error("ecrRegistry is required")

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    parallel services.collectEntries { svc ->
        [(svc): {
            // ECR 레포지토리 이름 규칙
            def image = "${ecrRegistry}/courm-${svc}:${imageTag}"
            echo "Jib build & push using IRSA (native AWS auth) -> ${image}"

            sh """
              set -e
              ./gradlew :service:${svc}:jib --no-daemon \\
                -Djib.to.image=${image}
            """
        }]
    }
}
