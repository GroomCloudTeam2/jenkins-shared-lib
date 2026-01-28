def call(String serviceName) {

    def imageUri = "${env.ECR_REGISTRY}/goorm-${serviceName}:${env.IMAGE_TAG}"
    def workDir  = "deploy/${serviceName}"

    sh """
      set -e

      rm -rf ${workDir}
      mkdir -p ${workDir}

      cat > ${workDir}/taskdef.json <<EOF
{
  "family": "msa-${serviceName}",
  "networkMode": "awsvpc",
  "requiresCompatibilities": ["FARGATE"],
  "cpu": "512",
  "memory": "1024",
  "executionRoleArn": "arn:aws:iam::${env.AWS_ACCOUNT_ID}:role/ecsTaskExecutionRole",
  "containerDefinitions": [
    {
      "name": "${serviceName}",
      "image": "${imageUri}",
      "essential": true,
      "portMappings": [
        {
          "containerPort": 8080,
          "protocol": "tcp"
        }
      ]
    }
  ]
}
EOF

      TASK_DEF_ARN=\$(aws ecs register-task-definition \\
        --cli-input-json file://${workDir}/taskdef.json \\
        --query 'taskDefinition.taskDefinitionArn' \\
        --output text \\
        --region ${env.AWS_REGION})

      aws ecs update-service \\
        --cluster ${env.ECS_CLUSTER} \\
        --service msa-${serviceName} \\
        --task-definition \$TASK_DEF_ARN \\
        --region ${env.AWS_REGION}
    """
}
