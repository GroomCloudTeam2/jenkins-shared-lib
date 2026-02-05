def call(Map args = [:]) {
    def service     = args.service ?: error("service is required")
    def tag         = args.tag ?: error("tag is required")
    def registry    = args.registry ?: env.ECR_REGISTRY ?: error("ECR_REGISTRY not set")

    return "${registry}/goorm-${service}:${tag}"
}
