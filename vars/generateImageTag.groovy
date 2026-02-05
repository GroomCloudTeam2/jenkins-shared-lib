def call(Map args = [:]) {
    def buildNumber = env.BUILD_NUMBER ?: "0"
    def gitCommit   = env.GIT_COMMIT ?: "unknown"
    def shortSha    = gitCommit.take(8)

    // 기본 전략: {BUILD_NUMBER}-{SHORT_SHA}
    return "${buildNumber}-${shortSha}"
}
