def call(Map args = [:]) {
    def services    = args.services ?: error("services is required")
    def imageTag    = args.imageTag ?: error("imageTag is required")
    def ecrRegistry = args.ecrRegistry ?: error("ecrRegistry is required")
    def awsRegion   = args.awsRegion ?: 'ap-northeast-2'

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    // 각 서비스별로 순차 처리
    services.each { svc ->
        def image = "${ecrRegistry}/goorm-${svc}:${imageTag}"

        echo "════════════════════════════════════════"
        echo "🚀 Service: ${svc}"
        echo "📦 Image: ${image}"
        echo "🔐 Auth: IRSA + ECR Token"
        echo "════════════════════════════════════════"

        // ✅ 1단계: AWS CLI 컨테이너에서 ECR 로그인 토큰 가져오기
        def ecrPassword = ''
        container('aws-cli') {
            echo "🔑 Retrieving ECR login token..."

            // IRSA 환경변수 확인
            sh '''
                echo "Checking IRSA configuration:"
                echo "  AWS_ROLE_ARN=${AWS_ROLE_ARN:-NOT_SET}"
                echo "  AWS_WEB_IDENTITY_TOKEN_FILE=${AWS_WEB_IDENTITY_TOKEN_FILE:-NOT_SET}"
            '''

            // AWS CLI 버전 확인
            sh 'aws --version'

            // ECR 토큰 가져오기
            ecrPassword = sh(
                    script: "aws ecr get-login-password --region ${awsRegion}",
                    returnStdout: true
            ).trim()

            echo "✅ ECR token retrieved successfully (${ecrPassword.length()} characters)"
        }

        // ✅ 2단계: Gradle 컨테이너에서 Jib 빌드 & 푸시
        container('gradle') {
            echo "🔨 Building and pushing Docker image with Jib..."

            sh """
              ./gradlew :service:${svc}:jib \\
                --no-daemon \\
                -Djib.to.image=${image} \\
                -Djib.to.auth.username=AWS \\
                -Djib.to.auth.password='${ecrPassword}' \\
                -Djib.from.auth.username='' \\
                -Djib.from.auth.password='' \\
                -Djib.console=plain \\
                --info
            """

            echo "✅ Successfully pushed: ${image}"
            echo "════════════════════════════════════════"
        }
    }
}