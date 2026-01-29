def call(Map config = [:]) {
    // 필수
    def serviceName = config.serviceName ?: error("serviceName 필수")

    // 옵션 (기본값 제공)
    def cpu = config.cpu ?: "512"
    def memory = config.memory ?: "1024"

    def imageUri = "${env.ECR_REGISTRY}/goorm-${serviceName}:${env.IMAGE_TAG}"
    def workDir = "deploy/${serviceName}"

    // 템플릿 로드 및 치환
    def template = libraryResource('templates/taskdef.json')
    def taskDef = template
            .replace('{{SERVICE_NAME}}', serviceName)
            .replace('{{IMAGE_URI}}', imageUri)
            .replace('{{CPU}}', cpu)
            .replace('{{MEMORY}}', memory)
            .replace('{{AWS_ACCOUNT_ID}}', env.AWS_ACCOUNT_ID)

    // 파일 생성
    sh "rm -rf ${workDir} && mkdir -p ${workDir}"
    writeFile file: "${workDir}/taskdef.json", text: taskDef

    // 배포
    sh """
        set -e
        TASK_DEF_ARN=\$(aws ecs register-task-definition \\
            --cli-input-json file://${workDir}/taskdef.json \\
            --query 'taskDefinition.taskDefinitionArn' \\
            --output text \\
            --region ${env.AWS_REGION})
        
        aws ecs update-service \\
            --cluster ${env.ECS_CLUSTER} \\
            --service msa-${serviceName}-service \\
            --task-definition \$TASK_DEF_ARN \\
            --region ${env.AWS_REGION}
    """
}
