def call(Map args = [:]) {
    def services    = args.services ?: error("services is required")
    def imageTag    = args.imageTag ?: error("imageTag is required")
    def ecrRegistry = args.ecrRegistry ?: error("ecrRegistry is required")
    def ecrPassword = args.ecrPassword ?: error("ecrPassword is required")

    if (!services || services.isEmpty()) {
        echo "No services to build & push"
        return
    }

    services.each { svc ->
        def image = "${ecrRegistry}/goorm-${svc}:${imageTag}"

        echo "Service: ${svc}"
        echo "Image: ${image}"

        withEnv(["JIB_ECR_PASSWORD=${ecrPassword}"]) {
            sh """
                ./gradlew :service:${svc}:jib \
                  --no-daemon \
                  -Dorg.gradle.vfs.watch=false \
                  -Djib.to.image=${image} \
                  -Djib.to.auth.username=AWS \
                  -Djib.to.auth.password="\$JIB_ECR_PASSWORD" \
                  -Djib.console=plain
              """
        }
    }
}