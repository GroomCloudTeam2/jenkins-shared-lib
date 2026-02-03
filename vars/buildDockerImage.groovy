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
    def imageName   = "${env.ECR_REGISTRY}/goorm-${service}"
    def builderName = "builder-${service}"

    sh """
        set -e
        export DOCKER_BUILDKIT=1

        # 서비스별 buildx builder 보장 (parallel 안전)
        docker buildx inspect ${builderName} >/dev/null 2>&1 || \
        docker buildx create --name ${builderName} --use

        docker buildx use ${builderName}

        # 멀티 아키텍처 빌드 + ECR push + cache
        docker buildx build \
            --platform linux/amd64,linux/arm64 \
            --cache-from type=registry,ref=${imageName}:cache \
            --cache-to type=registry,ref=${imageName}:cache,mode=max \
            -t ${imageName}:${imageTag} \
            -t ${imageName}:latest \
            --push \
            ${contextPath}
    """
}
