def call() {
    def allServices = ['user','cart','order','payment','product']
    def base = ''

    if (env.CHANGE_TARGET) {
        // PR 빌드
        base = "origin/${env.CHANGE_TARGET}"
    } else if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
        // 이전 성공 빌드 기준
        base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
    } else {
        echo "⚠️ No base commit found. Building all services."
        return allServices
    }

    def diff = ''

    dir(env.WORKSPACE) {

        // shallow clone 방어 (있어도 문제 없음)
        sh "git fetch --all --quiet || true"

        diff = sh(
                script: "git diff --name-only ${base}..HEAD || true",
                returnStdout: true
        ).trim()
    }

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
