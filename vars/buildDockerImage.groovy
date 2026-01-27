def call(String service) {

    def image = "${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG}"

    echo "🐳 Docker build: ${service}"

    sh """
        ls -al service/${service}/build/libs
        docker build -t ${image} service/${service}
    """
}
