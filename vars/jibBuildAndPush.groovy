def call(Map args = [:]) {
    // 필수 파라미터
    def services     = args.services ?: error("services is required")
    def imageTag     = args.imageTag ?: error("imageTag is required")
    def ecrRegistry  = args.ecrRegistry ?: error("ecrRegistry is required")
    def awsRegion    = args.awsRegion ?: error("awsRegion is required")

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    sh "aws sts get-caller-identity || true"

    parallel services.collectEntries { svc ->
        [(svc): {
            def image = "${ecrRegistry}/goorm-${svc}:${imageTag}"
            echo "Jib build & push -> ${image}"

            sh """
                set -e
                set +x
                ECR_PW=\$(aws ecr get-login-password --region ${awsRegion})

                ./gradlew :service:${svc}:jib --no-daemon \\
                  -Djib.to.image=${image} \\
                  -Djib.to.auth.username=AWS \\
                  -Djib.to.auth.password=\$ECR_PW
            """
        }]
    }
}
