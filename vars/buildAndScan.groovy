def call(String service) {

    def image = "${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG}"

    echo "🚀 Build & Dockerize service: ${service}"

    sh """
        ./gradlew :service:${service}:clean :service:${service}:bootJar

        echo "📦 JAR 확인"
        ls -al service/${service}/build/libs

        docker build -t ${image} service/${service}

        echo "🐳 Docker image built: ${image}"
    """

    // 보안 스캔 (필요 시 활성화)
    // sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${image}"
}
