def call(Map args = [:]) {
    def services    = args.services ?: error("services is required")
    def imageTag    = args.imageTag ?: error("imageTag is required")
    def ecrRegistry = args.ecrRegistry ?: error("ecrRegistry is required")
    def awsRegion   = args.awsRegion ?: 'ap-northeast-2'

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    parallel services.collectEntries { svc ->
        [(svc): {
            def image = "${ecrRegistry}/courm-${svc}:${imageTag}"

            echo "════════════════════════════════════════"
            echo "🚀 Building: ${svc}"
            echo "📦 Target: ${image}"
            echo "🔐 Auth: IRSA (AWS SDK)"
            echo "════════════════════════════════════════"

            // ✅ IRSA 환경변수 확인
            sh '''
                echo "Checking IRSA configuration..."
                echo "AWS_ROLE_ARN=${AWS_ROLE_ARN:-NOT_SET}"
                echo "AWS_WEB_IDENTITY_TOKEN_FILE=${AWS_WEB_IDENTITY_TOKEN_FILE:-NOT_SET}"
                echo "AWS_REGION=${AWS_REGION:-NOT_SET}"
                
                if [ -z "$AWS_ROLE_ARN" ]; then
                    echo "❌ ERROR: AWS_ROLE_ARN is not set!"
                    echo "Please check ServiceAccount IRSA annotation"
                    exit 1
                fi
            '''

            // ✅ AWS CLI 없이 Jib가 AWS SDK로 직접 인증
            sh """
              # AWS SDK 설정
              export AWS_SDK_LOAD_CONFIG=true
              export AWS_REGION=${awsRegion}
              
              # Jib 빌드 (AWS SDK가 자동으로 IRSA 사용)
              ./gradlew :service:${svc}:jib \\
                --no-daemon \\
                -Djib.to.image=${image} \\
                -Djib.console=plain \\
                --info
            """

            echo "✅ Successfully pushed: ${image}"
        }]
    }
}