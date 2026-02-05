def call(Map args = [:]) {
    def services     = args.services ?: error("services is required")
    def excludeTags  = args.excludeTags ?: ""
    def gradleOpts   = args.gradleOpts ?: "--no-daemon --parallel"

    if (!services || services.isEmpty()) {
        echo "No services to test"
        return
    }

    def testTasks = services.collect { svc ->
        ":service:${svc}:test :service:${svc}:jacocoTestReport"
    }.join(' ')

    def excludeOpt = excludeTags ? "-DexcludeTags=${excludeTags}" : ""

    sh """
        set -e
        ./gradlew ${testTasks} ${excludeOpt} ${gradleOpts}
    """
}
