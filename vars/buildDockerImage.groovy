//def call(String service) {
//    def contextPath = "service/${service}"
//
//    sh """
//        export DOCKER_BUILDKIT=1
//        docker pull ${env.ECR_REGISTRY}/goorm-${service}:latest || true
//        docker build \
//            --cache-from ${env.ECR_REGISTRY}/goorm-${service}:latest \
//            -t ${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG} \
//            -t ${env.ECR_REGISTRY}/goorm-${service}:latest \
//            ${contextPath}
//    """
//}
def call(String service, String imageTag) {
    def contextPath = "service/${service}"
    def imageName = "${env.ECR_REGISTRY}/goorm-${service}"

    sh """
        set -e

        # buildx 사용 보장
        docker buildx inspect multiarch-builder >/dev/null 2>&1 || \
        docker buildx create --name multiarch-builder --use

        docker buildx use multiarch-builder

        # 멀티 아키텍처 빌드 & ECR push
        docker buildx build \
            --platform linux/amd64,linux/arm64 \
            -t ${imageName}:${imageTag} \
            -t ${imageName}:latest \
            --push \
            ${contextPath}
    """
}
