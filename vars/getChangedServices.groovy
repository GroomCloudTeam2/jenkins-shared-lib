def call() {
    def allServices = ['user','cart','order','payment','product']

    sh "git config --global --add safe.directory '*'"

    // ✅ 디버깅: 현재 상태 확인
    echo "================================================"
    echo "🔍 Git Status Debug"
    echo "================================================"
    echo "WORKSPACE: ${env.WORKSPACE}"
    echo "BRANCH_NAME: ${env.BRANCH_NAME}"
    echo "GIT_COMMIT: ${env.GIT_COMMIT}"
    echo "GIT_PREVIOUS_COMMIT: ${env.GIT_PREVIOUS_COMMIT}"
    echo "GIT_PREVIOUS_SUCCESSFUL_COMMIT: ${env.GIT_PREVIOUS_SUCCESSFUL_COMMIT}"
    echo "CHANGE_TARGET: ${env.CHANGE_TARGET}"

    sh """
        echo "Current commit:"
        git log -1 --oneline
        echo ""
        echo "Recent commits:"
        git log -5 --oneline
        echo ""
        echo "Branches:"
        git branch -a
    """

    def base = ''
    if (env.CHANGE_TARGET) {
        base = "origin/${env.CHANGE_TARGET}"
        echo "📌 Using CHANGE_TARGET: ${base}"
    } else if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
        base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
        echo "📌 Using GIT_PREVIOUS_SUCCESSFUL_COMMIT: ${base}"
    } else if (env.GIT_PREVIOUS_COMMIT) {
        base = env.GIT_PREVIOUS_COMMIT
        echo "📌 Using GIT_PREVIOUS_COMMIT: ${base}"
    } else {
        // ✅ HEAD의 바로 이전 커밋과 비교
        base = "HEAD~1"
        echo "📌 Using HEAD~1 (previous commit)"
    }

    sh "git fetch --unshallow || git fetch --all || true"

    // ✅ diff 명령 전에 어떤 비교를 하는지 출력
    echo "🔍 Comparing: ${base}..HEAD"

    def diff = sh(
            script: """
            echo "Running: git diff --name-only ${base}..HEAD"
            git diff --name-only ${base}..HEAD || echo ''
        """,
            returnStdout: true
    ).trim()

    echo "================================================"
    echo "📄 Changed Files:"
    echo "${diff ?: '(none)'}"
    echo "================================================"

    if (!diff) {
        echo "⚠️ No changed files detected. Building all services as fallback."
        return allServices
    }

    def files = diff.readLines()
    echo "📝 Files list: ${files}"

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

    echo "🎯 Final changed services: ${changed}"
    return changed.unique()
}