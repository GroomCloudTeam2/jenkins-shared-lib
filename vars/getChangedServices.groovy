def call() {
    def allServices = ['user','cart','order','payment','product']

    // ✅ Git 안전 설정
    sh "git config --global --add safe.directory '*'"

    def base = ''
    if (env.CHANGE_TARGET) {
        base = "origin/${env.CHANGE_TARGET}"
    } else if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
        base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
    } else {
        echo "⚠️ No base commit found. Building all services."
        return allServices
    }

    // ✅ shallow clone 해결
    sh "git fetch --unshallow || true"

    // ✅ diff 실행
    def diff = sh(
            script: "git diff --name-only ${base}..HEAD || echo ''",
            returnStdout: true
    ).trim()

    if (!diff) {
        echo "🟢 No changed files detected."
        return []
    }

    def files = diff.readLines()

    if (files.any { it.startsWith('service/common/') }) {
        echo "🧩 common module changed → build all services"
        return allServices
    }

    def changed = []
    allServices.each { svc ->
        if (files.any { it.startsWith("service/${svc}/") }) {
            changed << svc
        }
    }

    return changed.unique()
}