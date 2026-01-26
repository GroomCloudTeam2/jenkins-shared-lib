def call(service) {
    def image = "${env.ECR_REGISTRY}/goorm-${service}:${env.IMAGE_TAG}"
    sh "docker build -t ${image} service/${service}"
    //sh "trivy image --severity HIGH,CRITICAL --exit-code 1 ${image}"
}
