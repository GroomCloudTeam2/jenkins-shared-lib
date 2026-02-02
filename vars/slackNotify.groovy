def call(Map args = [:]) {
    String status  = (args.status ?: (currentBuild?.currentResult ?: 'UNKNOWN')).toString()
    String channel = (args.channel ?: (env.SLACK_CHANNEL ?: '#ci')).toString()
    String branch  = (args.branch ?: (env.BRANCH_NAME ?: 'unknown')).toString()

    boolean ok = (status == 'SUCCESS')
    String emoji = ok ? "✅" : "❌"
    String color = ok ? "good" : "danger"

    // 빌드 시간
    String duration = (currentBuild?.durationString ?: "unknown").replace(' and counting', '')

    // 커밋(없으면 N/A)
    String commit  = (env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : '').trim()
    if (!commit) commit = shSafe("git rev-parse --short HEAD 2>/dev/null || true")
    String subject = shSafe("git log -1 --pretty=%s 2>/dev/null || true")

    if (!commit)  commit = "N/A"
    if (!subject) subject = "N/A"

    String msg
    if (ok) {
        msg = """${emoji} 성공
브랜치: ${branch}
시간: ${duration}
커밋: ${commit} - ${subject}"""
    } else {
        msg = """${emoji} 실패
브랜치: ${branch}
시간: ${duration}
커밋: ${commit} - ${subject}"""
    }

    slackSend(channel: channel, color: color, message: msg)
}

private String shSafe(String cmd) {
    try {
        return sh(script: cmd, returnStdout: true).trim()
    } catch (ignored) {
        return ""
    }
}
