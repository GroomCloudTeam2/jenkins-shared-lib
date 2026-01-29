def call(String service) {
    sh """
        docker push ${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG}
        docker push ${env.ECR_REGISTRY}/goorm-${service}:latest
    """
}
