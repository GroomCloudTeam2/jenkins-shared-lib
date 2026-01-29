def call(String service) {
    def contextPath = "service/${service}"

    sh """
        export DOCKER_BUILDKIT=1
        docker pull ${env.ECR_REGISTRY}/goorm-${service}:latest || true
        docker build \
            --cache-from ${env.ECR_REGISTRY}/goorm-${service}:latest \
            -t ${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG} \
            -t ${env.ECR_REGISTRY}/goorm-${service}:latest \
            ${contextPath}
    """
}
