#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  scripts/health-check.sh
#
#  Post-deployment health verification.
#  Run on the EC2 host after 'docker compose up -d' completes.
#
#  Checks:
#    1. backend container state is 'running'
#    2. frontend container state is 'running'
#    3. Frontend Nginx /healthz returns "ok"
#    4. Backend Spring Boot /api/actuator/health returns {"status":"UP"}
#    5. Backend DB component (RDS) reports status UP in actuator detail
#
#  Exit codes:
#    0 — all checks passed
#    1 — one or more checks failed
#
#  Notes:
#    - Uses 'docker exec' for the actuator call so port 8080 does not need
#      to be published to the host.
#    - (( N++ )) is intentionally avoided because arithmetic expansion of 0
#      returns exit-code 1 in bash strict mode.  Use N=$((N+1)) instead.
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
    PASS=$((PASS + 1))
  else
    echo "  FAIL  ${label}: ${result}"
    FAIL=$((FAIL + 1))
  fi
}

echo "==> Health check"

# ── 1 & 2. Container states ───────────────────────────────────────────────────
echo ""
echo "--- Container status ---"
docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" ps 2>/dev/null || true

for SVC in backend frontend; do
  # 'docker inspect' is more reliable than compose ps --format for state
  CONTAINER_NAME="emp_${SVC}"
  STATE=$(docker inspect --format '{{.State.Status}}' "${CONTAINER_NAME}" 2>/dev/null || echo "not_found")
  if [[ "${STATE}" == "running" ]]; then
    check "Container ${SVC} running" "ok"
  else
    check "Container ${SVC} running" "state=${STATE}"
  fi
done

# ── 3. Frontend /healthz ──────────────────────────────────────────────────────
echo ""
echo "--- Frontend /healthz ---"
NGINX_RESP=$(curl -sf --max-time 5 http://localhost/healthz 2>&1 || echo "CURL_FAIL")
if echo "${NGINX_RESP}" | grep -q "ok"; then
  check "Frontend /healthz" "ok"
else
  check "Frontend /healthz" "${NGINX_RESP}"
fi

# ── 4. Backend actuator health ────────────────────────────────────────────────
echo ""
echo "--- Backend /api/actuator/health ---"
# Port 8080 is not published to the host (only 'expose:' in prod compose).
# Use docker exec to call wget inside the container.
ACTUATOR_RESP=$(docker exec emp_backend \
  wget -qO- --timeout=10 http://localhost:8080/api/actuator/health 2>&1 \
  || echo "EXEC_FAIL")
echo "  Response: ${ACTUATOR_RESP}"

if echo "${ACTUATOR_RESP}" | grep -q '"status":"UP"'; then
  check "Backend actuator status UP" "ok"
else
  check "Backend actuator status UP" "${ACTUATOR_RESP}"
fi

# ── 5. DB (RDS) component status ─────────────────────────────────────────────
# Requires management.endpoint.health.show-details=always in application-prod.properties
# The container healthcheck already confirms connectivity, but this gives an
# explicit confirmation that Flyway ran and the connection pool is active.
if echo "${ACTUATOR_RESP}" | grep -q '"db"'; then
  if echo "${ACTUATOR_RESP}" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    db = data.get('components', {}).get('db', {})
    sys.exit(0 if db.get('status') == 'UP' else 1)
except Exception:
    sys.exit(1)
" 2>/dev/null; then
    check "Backend DB (RDS) connectivity" "ok"
  else
    check "Backend DB (RDS) connectivity" "db component status not UP"
  fi
else
  # 'db' not present: show-details may not be returning components.
  # Fall back to checking that status=UP (basic connectivity already confirmed
  # by the container healthcheck which also calls /api/actuator/health).
  if echo "${ACTUATOR_RESP}" | grep -q '"status":"UP"'; then
    check "Backend DB (RDS) connectivity" "ok (inferred from status:UP)"
  else
    check "Backend DB (RDS) connectivity" "db component absent and status not UP"
  fi
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "==> Health check summary: ${PASS} passed, ${FAIL} failed"
if [[ "${FAIL}" -gt 0 ]]; then
  echo ""
  echo "==> Recent container logs:"
  docker compose --file "${COMPOSE_FILE}" --env-file "${ENV_FILE}" logs --tail=30 2>/dev/null || true
  exit 1
fi
echo "==> All checks passed"
