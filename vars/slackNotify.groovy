def call(Map args = [:]) {
    String status   = (args.status ?: (currentBuild?.currentResult ?: 'UNKNOWN')).toString()
    String channel  = (args.channel ?: (env.SLACK_CHANNEL ?: '#ci')).toString()
    def servicesIn  = args.services
    String branch   = (args.branch ?: (env.BRANCH_NAME ?: 'unknown')).toString()

    String services = '없음'
    if (servicesIn instanceof Collection && servicesIn.size() > 0) services = servicesIn.join(', ')
    else if (servicesIn instanceof String && servicesIn?.trim())   services = servicesIn

    String duration = (currentBuild?.durationString ?: "unknown").replace(' and counting', '')

    // git 정보 (실패해도 알림은 보내게)
    String commit  = sh(script: "git rev-parse --short HEAD || true", returnStdout: true).trim()
    String author  = sh(script: "git log -1 --pretty=%an || true",   returnStdout: true).trim()
    String subject = sh(script: "git log -1 --pretty=%s  || true",   returnStdout: true).trim()

    String prInfo = env.CHANGE_ID ? "PR: #${env.CHANGE_ID} → ${env.CHANGE_TARGET}\n" : ""

    String cause = getBuildCauseSafe()

    boolean ok = (status == 'SUCCESS')
    String emoji = ok ? "✅" : (status == 'FAILURE' ? "❌" : "⚠️")
    String color = ok ? "good" : (status == 'FAILURE' ? "danger" : "warning")

    String msg = """${emoji} ${status}
브랜치: ${branch}
${prInfo}서비스: ${services}
빌드: ${env.JOB_NAME} #${env.BUILD_NUMBER}
시간: ${duration}
커밋: ${commit} (${author}) - ${subject}
트리거: ${cause}
링크: <${env.BUILD_URL}|Jenkins 열기>"""

    slackSend(channel: channel, color: color, message: msg)
}

@NonCPS
private String getBuildCauseSafe() {
    try {
        def causes = currentBuild?.rawBuild?.getCauses()
        return causes ? causes[0]?.getShortDescription() : 'Unknown'
    } catch (ignored) {
        return 'Unknown'
    }
}
