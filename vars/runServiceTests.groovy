def call(Map args = [:]) {
    def services     = args.services ?: error("services is required")
    def excludeTags  = args.excludeTags ?: ""
    def gradleOpts   = args.gradleOpts ?: "--no-daemon"

    if (!services || services.isEmpty()) {
        echo "No services to test"
        return
    }

    def testTasks = services.collect { svc ->
        ":service:${svc}:test"
        // ":service:${svc}:jacocoTestReport"
    }.join(' ')

    def excludeOpt = excludeTags ? "-PexcludeTags=${excludeTags}" : ""

    echo "🚀 Running tests: ${testTasks}"

    sh """
        set -e
        ./gradlew ${testTasks} ${excludeOpt} ${gradleOpts}
    """
}