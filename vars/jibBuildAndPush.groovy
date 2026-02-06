def call(Map args = [:]) {
    def services    = args.services ?: error("services is required")
    def imageTag    = args.imageTag ?: error("imageTag is required")
    def ecrRegistry = args.ecrRegistry ?: error("ecrRegistry is required")

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    // [수정] IRSA 사용 시 Password 체크 로직이 필요 없으므로 제거

    parallel services.collectEntries { svc ->
        [(svc): {
            // ECR 레포지토리 이름 규칙 확인 (goorm인지 courm인지 확인 필요)
            def image = "${ecrRegistry}/courm-${svc}:${imageTag}"
            echo "Jib build & push using IRSA -> ${image}"

            sh """
              set -e
              ./gradlew :service:${svc}:jib --no-daemon \\
                -Djib.to.image=${image} \\
                -Djib.to.credHelper=ecr-login
            """
        }]
    }
}