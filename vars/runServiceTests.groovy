def call(Map args = [:]) {
    def services     = args.services ?: error("services is required")
    def excludeTags  = args.excludeTags ?: ""
    def gradleOpts   = args.gradleOpts ?: "--no-daemon"

    if (!services || services.isEmpty()) {
        echo "No services to test"
        return
    }

    // [수정] Jacoco 리포트 생성은 주석 처리하여 속도를 높입니다.
    def testTasks = services.collect { svc ->
        ":service:${svc}:test"
        // ":service:${svc}:jacocoTestReport" // 일단 제거
    }.join(' ')

    def excludeOpt = excludeTags ? "-PexcludeTags=${excludeTags}" : ""

    echo "🚀 Running only tests (Jacoco skipped): ${testTasks}"

    sh """
        set -e
        ./gradlew ${testTasks} ${excludeOpt} ${gradleOpts}
    """
}