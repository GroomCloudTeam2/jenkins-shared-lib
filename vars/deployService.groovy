sh """
  set -e

  rm -rf ${workDir}
  mkdir -p ${workDir}

  cat > ${workDir}/taskdef.json <<EOF
{
  "family": "msa-${serviceName}",
  ...
}
EOF

  TASK_DEF_ARN=\$(aws ecs register-task-definition \
    --cli-input-json file://${workDir}/taskdef.json \
    --query 'taskDefinition.taskDefinitionArn' \
    --output text \
    --region ${env.AWS_REGION})

  cp codedeploy/${serviceName}/appspec.yml ${workDir}/appspec.yml
  sed -i "s|<TASK_DEFINITION_ARN>|\$TASK_DEF_ARN|g" ${workDir}/appspec.yml

  cd ${workDir}
  zip -r ../${zipName} .
  cd -

  aws s3 cp \
    ${workDir}/../${zipName} \
    s3://${env.S3_BUCKET}/${serviceName}/${zipName}

  # =========================
  # 6️⃣ CodeDeploy 배포 생성 (임시 비활성화)
  # =========================
  # aws deploy create-deployment \
  #   --application-name ${env.CODEDEPLOY_APP} \
  #   --deployment-group-name ${env.CODEDEPLOY_GROUP_PREFIX}-${serviceName} \
  #   --s3-location bucket=${env.S3_BUCKET},key=${serviceName}/${zipName},bundleType=zip \
  #   --region ${env.AWS_REGION}
"""
