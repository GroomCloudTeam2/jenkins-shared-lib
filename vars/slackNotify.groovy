def call(Map args = [:]) {
    String status  = (args.status ?: (currentBuild?.currentResult ?: 'UNKNOWN')).toString()
    String channel = (args.channel ?: (env.SLACK_CHANNEL ?: '#ci')).toString()
    def servicesIn = args.services
    String branch  = (args.branch ?: (env.BRANCH_NAME ?: 'unknown')).toString()

    // services
    String services = '없음'
    if (servicesIn instanceof Collection && servicesIn.size() > 0) services = servicesIn.join(', ')
    else if (servicesIn instanceof String && servicesIn?.trim())   services = servicesIn

    // duration
    String duration = (currentBuild?.durationString ?: "unknown").replace(' and counting', '')

    // PR info (multibranch)
    String prInfo = env.CHANGE_ID ? "PR: #${env.CHANGE_ID} → ${env.CHANGE_TARGET}\n" : ""

    // trigger cause
    String cause = getBuildCauseSafe()

    // git info: env 우선 + 실패 안전
    String commit = (env.GIT_COMMIT ? env.GIT_COMMIT.take(7) : '').trim()
    if (!commit) commit = sh(script: "git rev-parse --short HEAD 2>/dev/null || true", returnStdout: true).trim()

    String author = sh(script: "git log -1 --pretty=%an 2>/dev/null || true", returnStdout: true).trim()
    String subject = sh(script: "git log -1 --pretty=%s  2>/dev/null || true", returnStdout: true).trim()

    if (!commit) commit = 'N/A'
    if (!author) author = 'N/A'
    if (!subject) subject = 'N/A'

    boolean ok = (status == 'SUCCESS')
    String emoji = ok ? "✅" : (status == 'FAILURE' ? "❌" : "⚠️")
    String color = ok ? "good" : (status == 'FAILURE' ? "danger" : "warning")

    // links
    String jenkinsLink  = env.BUILD_URL ? "<${env.BUILD_URL}|Jenkins>" : "Jenkins(N/A)"
    String consoleLink  = env.BUILD_URL ? "<${env.BUILD_URL}console|Console>" : "Console(N/A)"

    // 실패 때 한 줄 요약(최근 로그에서 에러처럼 보이는 마지막 라인 추출)
    String errorHint = ""
    if (!ok) {
        errorHint = getLastErrorHintSafe()
        if (errorHint) {
            // 너무 길면 잘라서 슬랙 메시지 폭주 방지
            if (errorHint.length() > 200) errorHint = errorHint.take(200) + "…"
            errorHint = "에러요약: ${errorHint}\n"
        }
    }

    String msg = """${emoji} ${status}
브랜치: ${branch}
${prInfo}서비스: ${services}
빌드: ${env.JOB_NAME} #${env.BUILD_NUMBER}
시간: ${duration}
커밋: ${commit} (${author}) - ${subject}
트리거: ${cause}
${errorHint}링크: ${jenkinsLink} | ${consoleLink}"""

    slackSend(channel: channel, color: color, message: msg)
}

@NonCPS
private String getBuildCauseSafe() {
    try {
        def causes = currentBuild?.rawBuild?.getCauses()
        if (!causes || causes.isEmpty()) return 'Unknown'
        // 여러 원인일 수 있어 짧게 합침
        def texts = causes.collect { it?.getShortDescription() }.findAll { it }
        return texts ? texts.join(' / ') : 'Unknown'
    } catch (ignored) {
        return 'Unknown'
    }
}

/**
 * 실패 시 콘솔 로그에서 마지막 에러성 라인 하나를 뽑아 요약.
 * - Jenkins Script Security / sandbox 이슈로 막히면 그냥 빈 문자열 반환
 */
private String getLastErrorHintSafe() {
    try {
        def logLines = currentBuild?.rawBuild?.getLog(200) // 마지막 200줄
        if (!logLines) return ""

        // 흔한 에러 패턴들(필요하면 더 추가 가능)
        def patterns = [
                ~/^.*ERROR.*$/i,
                        ~/^.*Exception.*$/i,
                ~/^.*FAILED.*$/i,
                        ~/^.*fatal:.*$/i,
                ~/^.*denied.*$/i,
                        ~/^.*permission.*$/i
        ]

        // 뒤에서부터 찾기
        for (int i = logLines.size() - 1; i >= 0; i--) {
            def line = logLines[i]?.toString()?.trim()
            if (!line) continue
            if (patterns.any { p -> line ==~ p }) return line
        }
        return ""
    } catch (ignored) {
        return ""
    }
}
