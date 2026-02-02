def call(String service, String imageTag) {
    sh """
      mkdir -p trivy-reports

      trivy image \
        --severity HIGH,CRITICAL \
        --format json \
        --output trivy-reports/${service}.json \
        ${env.ECR_REGISTRY}/${service}:${imageTag}
    """
}
