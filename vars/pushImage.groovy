def call(service) {
    sh "docker push ${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG}"
}
