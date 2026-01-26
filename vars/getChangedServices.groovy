def call() {
    def allServices = ['user','cart','order','payment','product']
    def base = ''

    if (env.CHANGE_TARGET) {
        base = "origin/${env.CHANGE_TARGET}"
    } else if (env.GIT_PREVIOUS_SUCCESSFUL_COMMIT) {
        base = env.GIT_PREVIOUS_SUCCESSFUL_COMMIT
    } else {
        return allServices
    }

    def diff = sh(
            script: "git diff --name-only ${base}..HEAD",
            returnStdout: true
    ).trim()

    if (!diff) return []

    if (diff.contains('service/common/')) {
        return allServices
    }

    def changed = []
    allServices.each { svc ->
        if (diff.contains("service/${svc}/")) {
            changed << svc
        }
    }

    return changed.unique()
}
