def call(String service) {
    sh """
        # BuildKit 활성화
        export DOCKER_BUILDKIT=1
        
        # 이전 이미지를 캐시로 pull (없어도 에러 안 남)
        docker pull ${env.ECR_REGISTRY}/goorm-${service}:latest || true
        
        # 캐시 사용하여 빌드
        docker build \\
            --cache-from ${env.ECR_REGISTRY}/goorm-${service}:latest \\
            -t ${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG} \\
            -t ${env.ECR_REGISTRY}/goorm-${service}:latest \\
            .
    """
}
