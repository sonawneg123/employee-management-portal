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
#  Required environment variables (injected by GitHub Actions ssh-action):
#    IMAGE_TAG    — Git SHA to deploy (also passed as $1)
#    ECR_REGISTRY — e.g. 123456789.dkr.ecr.us-east-1.amazonaws.com
#    AWS_REGION   — e.g. us-east-1
#
#  Preconditions (set up once via bootstrap-ec2.sh + EC2 setup):
#    - /opt/emp-portal/docker-compose.prod.yml   present
#    - /opt/emp-portal/.env.production           present and chmod 600
#    - AWS credentials available via EC2 instance role (NO stored keys)
#    - Docker and Docker Compose v2 installed
#    - 'deploy' user is in the 'docker' group
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

IMAGE_TAG="${1:?Usage: deploy.sh <git-sha>}"
APP_DIR="/opt/emp-portal"
COMPOSE_FILE="${APP_DIR}/docker-compose.prod.yml"
ENV_FILE="${APP_DIR}/.env.production"
HEALTH_TIMEOUT=180   # seconds — RDS cold start can be slow on first deploy
INTERVAL=10

# ── Validate required env vars ────────────────────────────────────────────────
: "${ECR_REGISTRY:?ECR_REGISTRY must be set}"
: "${AWS_REGION:?AWS_REGION must be set}"

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
echo "==> Region:   ${AWS_REGION}"

# ── Authenticate Docker to ECR (uses EC2 instance role — no stored keys) ─────
echo "==> Authenticating to ECR"
aws ecr get-login-password --region "${AWS_REGION}" \
  | docker login --username AWS --password-stdin "${ECR_REGISTRY}"

# ── Export variables needed by docker-compose.prod.yml ───────────────────────
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

# ── Wait for all containers to reach 'healthy' status ────────────────────────
# Strategy: poll 'docker inspect' on each container name directly.
# Compose v2 'ps --format json' output varies by version; docker inspect is stable.
echo "==> Waiting for containers to become healthy (timeout: ${HEALTH_TIMEOUT}s)"
ELAPSED=0
CONTAINERS=("emp_backend" "emp_frontend")

while true; do
  ALL_HEALTHY=true
  NOT_READY=()

  for CONTAINER in "${CONTAINERS[@]}"; do
    HEALTH=$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
             "${CONTAINER}" 2>/dev/null || echo "missing")

    # 'none' means the container has no healthcheck defined — treat as passing
    # if the container is at least running.
    if [[ "${HEALTH}" == "healthy" || "${HEALTH}" == "none" ]]; then
      RUNNING=$(docker inspect --format '{{.State.Status}}' "${CONTAINER}" 2>/dev/null || echo "missing")
      if [[ "${RUNNING}" != "running" ]]; then
        ALL_HEALTHY=false
        NOT_READY+=("${CONTAINER}:state=${RUNNING}")
      fi
    elif [[ "${HEALTH}" == "starting" ]]; then
      ALL_HEALTHY=false
      NOT_READY+=("${CONTAINER}:starting")
    else
      # unhealthy or missing
      ALL_HEALTHY=false
      NOT_READY+=("${CONTAINER}:${HEALTH}")
    fi
  done

  if [[ "${ALL_HEALTHY}" == "true" ]]; then
    echo "==> All containers healthy"
    break
  fi

  if [[ "${ELAPSED}" -ge "${HEALTH_TIMEOUT}" ]]; then
    echo "ERROR: Containers did not become healthy within ${HEALTH_TIMEOUT}s" >&2
    echo "  Not ready: ${NOT_READY[*]}"
    echo ""
    echo "==> Container status:"
    docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps 2>/dev/null || true
    echo ""
    echo "==> Recent logs:"
    docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs --tail=50 2>/dev/null || true
    exit 1
  fi

  echo "    Waiting... (${ELAPSED}s elapsed, not ready: ${NOT_READY[*]})"
  sleep "${INTERVAL}"
  ELAPSED=$((ELAPSED + INTERVAL))
done

# ── Final health verification ─────────────────────────────────────────────────
export APP_DIR
bash "$(dirname "$0")/health-check.sh"

echo ""
echo "Deployment complete: ${IMAGE_TAG}"
