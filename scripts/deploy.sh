#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  scripts/deploy.sh
#
#  Deployment script run ON the EC2 host by GitHub Actions over SSH.
#  Called with a single argument: the Git SHA to deploy.
#
#  Usage:
#    bash /opt/emp-portal/scripts/deploy.sh <git-sha>
#
#  Preconditions (set up once via bootstrap-ec2.sh + GitHub Actions secrets):
#    - /opt/emp-portal/docker-compose.prod.yml   present
#    - /opt/emp-portal/.env.production           present and readable
#    - ECR_REGISTRY env var set (from GitHub Actions deployment step)
#    - AWS credentials available via EC2 instance role (NO stored keys)
#    - Docker and Docker Compose v2 installed
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

IMAGE_TAG="${1:?Usage: deploy.sh <git-sha>}"
APP_DIR="/opt/emp-portal"
COMPOSE_FILE="${APP_DIR}/docker-compose.prod.yml"
ENV_FILE="${APP_DIR}/.env.production"
HEALTH_TIMEOUT=120   # seconds to wait for healthy status

if [[ ! -f "${COMPOSE_FILE}" ]]; then
  echo "ERROR: ${COMPOSE_FILE} not found" >&2
  exit 1
fi

if [[ ! -f "${ENV_FILE}" ]]; then
  echo "ERROR: ${ENV_FILE} not found" >&2
  exit 1
fi

echo "==> Deploying image tag: ${IMAGE_TAG}"
echo "==> Registry: ${ECR_REGISTRY}"

# ── Authenticate Docker to ECR (uses EC2 instance role — no stored keys) ─────
echo "==> Authenticating to ECR"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

# ── Export variables for docker compose ──────────────────────────────────────
export IMAGE_TAG
export ECR_REGISTRY

# ── Pull new images ───────────────────────────────────────────────────────────
echo "==> Pulling images"
docker compose \
  --file "${COMPOSE_FILE}" \
  --env-file "${ENV_FILE}" \
  pull

# ── Start / replace containers ────────────────────────────────────────────────
echo "==> Starting containers"
docker compose \
  --file "${COMPOSE_FILE}" \
  --env-file "${ENV_FILE}" \
  up -d --remove-orphans

# ── Wait for healthy status ───────────────────────────────────────────────────
echo "==> Waiting for containers to become healthy (timeout: ${HEALTH_TIMEOUT}s)"
ELAPSED=0
INTERVAL=5

while true; do
  UNHEALTHY=$(docker compose \
    --file "${COMPOSE_FILE}" \
    --env-file "${ENV_FILE}" \
    ps --format json 2>/dev/null \
    | python3 -c "
import sys, json
containers = [json.loads(l) for l in sys.stdin if l.strip()]
unhealthy = [c['Name'] for c in containers if c.get('Health','') not in ('healthy','')]
print('\n'.join(unhealthy))
" 2>/dev/null || echo "parse-error")

  if [[ -z "${UNHEALTHY}" ]]; then
    echo "==> All containers healthy"
    break
  fi

  if [[ "${ELAPSED}" -ge "${HEALTH_TIMEOUT}" ]]; then
    echo "ERROR: Containers did not become healthy within ${HEALTH_TIMEOUT}s" >&2
    docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps
    docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs --tail=50
    exit 1
  fi

  echo "    Waiting... (${ELAPSED}s elapsed, unhealthy: ${UNHEALTHY})"
  sleep "${INTERVAL}"
  ELAPSED=$(( ELAPSED + INTERVAL ))
done

# ── Final health verification ─────────────────────────────────────────────────
bash "$(dirname "$0")/health-check.sh"

echo ""
echo "Deployment complete: ${IMAGE_TAG}"
