#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  aws/ecr/bootstrap-ecr.sh
#
#  One-time script to create both ECR repositories and attach lifecycle
#  policies.  Run this ONCE before the first GitHub Actions push.
#
#  Usage:
#    export AWS_REGION=us-east-1
#    export AWS_ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
#    bash aws/ecr/bootstrap-ecr.sh
#
#  Requirements:
#    - AWS CLI installed and configured with sufficient permissions
#    - IAM permissions: ecr:CreateRepository, ecr:PutLifecyclePolicy
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

REGION="${AWS_REGION:?Set AWS_REGION first}"
ACCOUNT_ID="${AWS_ACCOUNT_ID:?Set AWS_ACCOUNT_ID first}"

REPOS=(
  "employee-management-backend"
  "employee-management-frontend"
)

LIFECYCLE_POLICY='{
  "rules": [
    {
      "rulePriority": 1,
      "description": "Keep last 20 tagged images",
      "selection": {
        "tagStatus": "tagged",
        "tagPrefixList": ["main-"],
        "countType": "imageCountMoreThan",
        "countNumber": 20
      },
      "action": { "type": "expire" }
    },
    {
      "rulePriority": 2,
      "description": "Expire untagged images after 1 day",
      "selection": {
        "tagStatus": "untagged",
        "countType": "sinceImagePushed",
        "countUnit": "days",
        "countNumber": 1
      },
      "action": { "type": "expire" }
    }
  ]
}'

for REPO in "${REPOS[@]}"; do
  echo "──────────────────────────────────────"
  echo "Repository: ${REPO}"

  # Create repository (idempotent — error if already exists is suppressed)
  if aws ecr describe-repositories \
       --repository-names "${REPO}" \
       --region "${REGION}" \
       --query 'repositories[0].repositoryName' \
       --output text 2>/dev/null | grep -q "${REPO}"; then
    echo "  Already exists — skipping creation"
  else
    aws ecr create-repository \
      --repository-name "${REPO}" \
      --region "${REGION}" \
      --image-tag-mutability IMMUTABLE \
      --image-scanning-configuration scanOnPush=true \
      --encryption-configuration encryptionType=AES256
    echo "  Created"
  fi

  # Apply lifecycle policy
  aws ecr put-lifecycle-policy \
    --repository-name "${REPO}" \
    --region "${REGION}" \
    --lifecycle-policy-text "${LIFECYCLE_POLICY}"
  echo "  Lifecycle policy applied"

  REPO_URI="${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com/${REPO}"
  echo "  URI: ${REPO_URI}"
done

echo ""
echo "ECR bootstrap complete."
echo ""
echo "Set these GitHub Actions variables:"
echo "  AWS_REGION   = ${REGION}"
echo "  ECR_REGISTRY = ${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"
