#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  scripts/health-check.sh
#
#  Post-deployment health verification.
#  Run on the EC2 host after 'docker compose up -d' completes.
#
#  Checks:
#    1. All Compose services are running
#    2. Frontend Nginx /healthz returns "ok"
#    3. Backend Spring Boot /api/actuator/health returns {"status":"UP"}
#    4. Backend health includes database connectivity (Flyway ran successfully)
#
#  Exit codes:
#    0 — all checks passed
#    1 — one or more checks failed
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

APP_DIR="${APP_DIR:-/opt/emp-portal}"
COMPOSE_FILE="${APP_DIR}/docker-compose.prod.yml"
ENV_FILE="${APP_DIR}/.env.production"

PASS=0
FAIL=0

check() {
  local label="$1"
  local result="$2"   # "ok" or failure message
  if [[ "${result}" == "ok" ]]; then
    echo "  PASS  ${label}"
    (( PASS++ )) || true
  else
    echo "  FAIL  ${label}: ${result}"
    (( FAIL++ )) || true
  fi
}

echo "==> Health check"

# ── 1. Container states ───────────────────────────────────────────────────────
echo ""
echo "--- Container status ---"
docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps

for SVC in backend frontend; do
  STATE=$(docker compose \
    --file "${COMPOSE_FILE}" \
    --env-file "${ENV_FILE}" \
    ps --format "{{.State}}" "${SVC}" 2>/dev/null || echo "unknown")
  if [[ "${STATE}" == "running" ]]; then
    check "Container ${SVC} running" "ok"
  else
    check "Container ${SVC} running" "state=${STATE}"
  fi
done

# ── 2. Frontend healthz ───────────────────────────────────────────────────────
echo ""
echo "--- Frontend /healthz ---"
NGINX_RESP=$(curl -sf --max-time 5 http://localhost/healthz 2>&1 || echo "CURL_FAIL")
if echo "${NGINX_RESP}" | grep -q "ok"; then
  check "Frontend /healthz" "ok"
else
  check "Frontend /healthz" "${NGINX_RESP}"
fi

# ── 3. Backend /api/actuator/health ──────────────────────────────────────────
echo ""
echo "--- Backend /api/actuator/health ---"
# Actuator is not exposed on the host port; call via docker exec to stay internal
ACTUATOR_RESP=$(docker exec emp_backend \
  wget -qO- http://localhost:8080/api/actuator/health 2>&1 || echo "EXEC_FAIL")
echo "  Response: ${ACTUATOR_RESP}"

if echo "${ACTUATOR_RESP}" | grep -q '"status":"UP"'; then
  check "Backend actuator status UP" "ok"
else
  check "Backend actuator status UP" "unexpected response"
fi

# ── 4. Database connectivity in actuator detail ───────────────────────────────
if echo "${ACTUATOR_RESP}" | python3 -c "
import sys, json
data = json.load(sys.stdin)
db = data.get('components', {}).get('db', {})
sys.exit(0 if db.get('status') == 'UP' else 1)
" 2>/dev/null; then
  check "Backend DB (RDS) connectivity" "ok"
else
  check "Backend DB (RDS) connectivity" "db component not UP or not present"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "==> Health check summary: ${PASS} passed, ${FAIL} failed"
if [[ "${FAIL}" -gt 0 ]]; then
  echo ""
  echo "==> Recent container logs:"
  docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs --tail=30
  exit 1
fi
echo "==> All checks passed"
