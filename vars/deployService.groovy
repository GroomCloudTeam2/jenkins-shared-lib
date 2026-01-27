def call(String serviceName) {

    def imageUri = "${env.ECR_REGISTRY}/goorm-${serviceName}:${env.IMAGE_TAG}"
    def workDir  = "deploy/${serviceName}"
    def zipName  = "${serviceName}-${env.BUILD_NUMBER}.zip"

    echo " Deploy service: ${serviceName}"
    echo " Image: ${imageUri}"

    sh """
      set -e

      # 작업 디렉토리 초기화
      rm -rf ${workDir}
      mkdir -p ${workDir}

      # =========================
      # 1️⃣ taskdef.json 생성
      # =========================
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

      # =========================
      # 2️⃣ Task Definition 등록
      # =========================
      TASK_DEF_ARN=\$(aws ecs register-task-definition \\
        --cli-input-json file://${workDir}/taskdef.json \\
        --query 'taskDefinition.taskDefinitionArn' \\
        --output text \\
        --region ${env.AWS_REGION})

      echo "✅ Registered Task Definition: \$TASK_DEF_ARN"

      # =========================
      # 3️⃣ appspec.yml 치환
      # =========================
      cp codedeploy/${serviceName}/appspec.yml ${workDir}/appspec.yml

      sed -i "s|<TASK_DEFINITION_ARN>|\$TASK_DEF_ARN|g" ${workDir}/appspec.yml

      # =========================
      # 4️⃣ ZIP 생성
      # =========================
      cd ${workDir}
      zip -r ../${zipName} .
      cd -

      # =========================
      # 5️⃣ S3 업로드
      # =========================
      aws s3 cp \\
        ${workDir}/../${zipName} \\
        s3://${env.S3_BUCKET}/${serviceName}/${zipName}

      # =========================
      # 6️⃣ CodeDeploy 배포 생성
      # =========================
     aws deploy create-deployment \\
       --application-name ${env.CODEDEPLOY_APP} \\
       --deployment-group-name ${env.CODEDEPLOY_GROUP_PREFIX}-${serviceName} \\
       --s3-location bucket=${env.S3_BUCKET},key=${serviceName}/${zipName},bundleType=zip \\
       --region ${env.AWS_REGION}
    """
}
