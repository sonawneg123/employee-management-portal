#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  aws/ec2/verify-ec2.sh
#
#  On-host EC2 verification script.
#  Run as the 'deploy' user (or ec2-user) after bootstrap.
#
#  Verifies:
#    1. Docker is installed and running
#    2. Docker Compose v2 is installed
#    3. AWS CLI is installed
#    4. IAM instance role is attached (via sts:GetCallerIdentity)
#    5. EC2 can authenticate to ECR (requires IAM role to be attached)
#    6. 'deploy' user exists and is in the docker group
#    7. /opt/emp-portal/ directory exists with correct permissions
#    8. Required deployment files are present
#
#  Usage:
#    bash aws/ec2/verify-ec2.sh
#
#  This script does NOT modify any state. It is read-only.
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

REGION="${AWS_REGION:-us-east-1}"
APP_DIR="/opt/emp-portal"

PASS=0
FAIL=0

check() {
  local label="$1"
  local result="$2"
  if [[ "${result}" == "ok" ]]; then
    echo "  PASS  ${label}"
    PASS=$((PASS + 1))
  else
    echo "  FAIL  ${label}: ${result}"
    FAIL=$((FAIL + 1))
  fi
}

echo "==> EC2 host verification (region: ${REGION})"
echo ""

# ── 1. Docker ─────────────────────────────────────────────────────────────────
echo "--- Docker ---"
if command -v docker &>/dev/null; then
  DOCKER_VER=$(docker --version 2>/dev/null || echo "unknown")
  check "Docker installed (${DOCKER_VER})" "ok"
else
  check "Docker installed" "not found"
fi

if systemctl is-active --quiet docker 2>/dev/null; then
  check "Docker daemon running" "ok"
else
  check "Docker daemon running" "not running"
fi

# ── 2. Docker Compose v2 ──────────────────────────────────────────────────────
echo ""
echo "--- Docker Compose ---"
if docker compose version &>/dev/null; then
  COMPOSE_VER=$(docker compose version 2>/dev/null || echo "unknown")
  check "Docker Compose v2 installed (${COMPOSE_VER})" "ok"
else
  check "Docker Compose v2 installed" "not found — install plugin"
fi

# ── 3. AWS CLI ────────────────────────────────────────────────────────────────
echo ""
echo "--- AWS CLI ---"
if command -v aws &>/dev/null; then
  AWS_VER=$(aws --version 2>/dev/null || echo "unknown")
  check "AWS CLI installed (${AWS_VER})" "ok"
else
  check "AWS CLI installed" "not found"
fi

# ── 4. IAM instance role via sts:GetCallerIdentity ───────────────────────────
echo ""
echo "--- IAM instance role ---"
IDENTITY=$(aws sts get-caller-identity --output json 2>/dev/null || echo '{"error":"failed"}')
echo "  Identity: ${IDENTITY}"

ACCOUNT=$(echo "${IDENTITY}" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('Account',''))" 2>/dev/null || echo "")
ARN=$(echo "${IDENTITY}" | python3 -c "import sys,json;d=json.load(sys.stdin);print(d.get('Arn',''))" 2>/dev/null || echo "")

if [[ -n "${ACCOUNT}" && "${ACCOUNT}" != "" ]]; then
  check "STS GetCallerIdentity succeeded (account: ${ACCOUNT})" "ok"
else
  check "STS GetCallerIdentity succeeded" "failed — is IAM instance profile attached?"
fi

if echo "${ARN}" | grep -q "assumed-role/EC2-EmpPortal-InstanceRole"; then
  check "Caller identity is EC2-EmpPortal-InstanceRole" "ok"
elif [[ -n "${ARN}" ]]; then
  check "Caller identity is EC2-EmpPortal-InstanceRole" "ARN is ${ARN} — verify correct role"
else
  check "Caller identity is EC2-EmpPortal-InstanceRole" "ARN not available"
fi

# ── 5. ECR authentication ─────────────────────────────────────────────────────
echo ""
echo "--- ECR authentication ---"
if aws ecr get-login-password --region "${REGION}" &>/dev/null; then
  check "ECR GetAuthorizationToken (region: ${REGION})" "ok"
else
  check "ECR GetAuthorizationToken (region: ${REGION})" "failed — check IAM policy"
fi

# ── 6. 'deploy' user and docker group ────────────────────────────────────────
echo ""
echo "--- Deploy user ---"
if id deploy &>/dev/null; then
  check "'deploy' user exists" "ok"
  DEPLOY_GROUPS=$(id -Gn deploy 2>/dev/null || echo "")
  if echo "${DEPLOY_GROUPS}" | grep -q "docker"; then
    check "'deploy' user in docker group" "ok"
  else
    check "'deploy' user in docker group" "groups: ${DEPLOY_GROUPS}"
  fi
else
  check "'deploy' user exists" "not found — run bootstrap-ec2.sh"
  check "'deploy' user in docker group" "SKIP — user missing"
fi

# ── 7. Application directory ──────────────────────────────────────────────────
echo ""
echo "--- Application directory ---"
if [[ -d "${APP_DIR}" ]]; then
  check "${APP_DIR} directory exists" "ok"
  OWNER=$(stat -c '%U' "${APP_DIR}" 2>/dev/null || echo "unknown")
  if [[ "${OWNER}" == "deploy" ]]; then
    check "${APP_DIR} owned by deploy" "ok"
  else
    check "${APP_DIR} owned by deploy" "owner=${OWNER}"
  fi
else
  check "${APP_DIR} directory exists" "not found"
  check "${APP_DIR} owned by deploy" "SKIP — directory missing"
fi

# ── 8. Deployment files ───────────────────────────────────────────────────────
echo ""
echo "--- Deployment files ---"
REQUIRED_FILES=(
  "${APP_DIR}/docker-compose.prod.yml"
  "${APP_DIR}/.env.production"
  "${APP_DIR}/scripts/deploy.sh"
  "${APP_DIR}/scripts/health-check.sh"
  "${APP_DIR}/scripts/rollback.sh"
)
for F in "${REQUIRED_FILES[@]}"; do
  if [[ -f "${F}" ]]; then
    check "${F} exists" "ok"
  else
    check "${F} exists" "MISSING — copy from repo before deploying"
  fi
done

# Check .env.production permissions (should be 600)
ENV_FILE="${APP_DIR}/.env.production"
if [[ -f "${ENV_FILE}" ]]; then
  ENV_PERMS=$(stat -c '%a' "${ENV_FILE}" 2>/dev/null || echo "unknown")
  if [[ "${ENV_PERMS}" == "600" ]]; then
    check ".env.production permissions are 600" "ok"
  else
    check ".env.production permissions are 600" "permissions=${ENV_PERMS} — run: chmod 600 ${ENV_FILE}"
  fi
fi

# Check scripts are executable
for S in "${APP_DIR}/scripts/"*.sh; do
  if [[ -x "${S}" ]]; then
    check "$(basename "${S}") is executable" "ok"
  else
    check "$(basename "${S}") is executable" "run: chmod +x ${APP_DIR}/scripts/*.sh"
  fi
done

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "==> EC2 verification: ${PASS} passed, ${FAIL} failed"
if [[ "${FAIL}" -gt 0 ]]; then
  echo ""
  echo "==> Fix the FAIL items above before running the first deployment."
  exit 1
fi
echo "==> EC2 host is ready for deployment."
