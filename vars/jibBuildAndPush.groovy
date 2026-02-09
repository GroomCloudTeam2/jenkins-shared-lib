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
            def image = "${ecrRegistry}/courm-${svc}:${imageTag}"
            echo "Jib build & push using IRSA -> ${image}"

            // ✅ 환경변수 확인 (디버깅)
            sh '''
                echo "AWS_ROLE_ARN: $AWS_ROLE_ARN"
                echo "AWS_WEB_IDENTITY_TOKEN_FILE: $AWS_WEB_IDENTITY_TOKEN_FILE"
            '''

            sh """
              set -e
              # ✅ Jib가 AWS SDK를 사용하도록 설정
              export AWS_SDK_LOAD_CONFIG=true
              
              ./gradlew :service:${svc}:jib --no-daemon \\
                -Djib.to.image=${image} \\
                -Djib.to.credHelper= \\
                -Djib.allowInsecureRegistries=false \\
                -Djib.console=plain \\
                --info
            """
        }]
    }
}