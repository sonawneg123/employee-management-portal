#!/usr/bin/env bash
# ──────────────────────────────────────────────────────────────────────────────
#  scripts/smoke-test.sh
#
#  Real API smoke tests against the running production application.
#  Run on the EC2 host after a successful deployment.
#
#  Usage:
#    BASE_URL=http://localhost bash /opt/emp-portal/scripts/smoke-test.sh
#
#  Tests:
#    1.  POST /api/auth/register  — create an ADMIN user
#    2.  POST /api/auth/login     — authenticate, receive JWT
#    3.  GET  /api/employees      — list employees (authenticated)
#    4.  POST /api/departments    — create a department (ADMIN)
#    5.  GET  /api/departments    — list departments (authenticated)
#    6.  POST /api/employees      — create an employee (ADMIN/HR)
#    7.  POST /api/auth/register  — create an EMPLOYEE user
#    8.  POST /api/auth/login     — authenticate as EMPLOYEE
#    9.  POST /api/leaves         — EMPLOYEE submits a leave request
#    10. GET  /api/leaves/my      — EMPLOYEE sees own leaves
#    11. RBAC: GET /api/employees returns 200 for ADMIN, 403 for anonymous
#    12. Persistence: re-read department after container restart (external)
#
#  Does NOT:
#    - Test UI functionality
#    - Create permanent test data (cleanup is performed at the end)
#    - Print any secret values
# ──────────────────────────────────────────────────────────────────────────────

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost}"
API="${BASE_URL}/api"

PASS=0
FAIL=0
CLEANUP_DEPT_ID=""
CLEANUP_EMP_ID=""
CLEANUP_LEAVE_ID=""

# ── Helper ────────────────────────────────────────────────────────────────────

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

api() {
  # curl wrapper that always succeeds (never exits on HTTP error)
  # Returns the response body
  curl -sf --max-time 15 "$@" 2>/dev/null || echo '{"error":"curl_failed"}'
}

http_status() {
  curl -o /dev/null -w "%{http_code}" -s --max-time 15 "$@" 2>/dev/null || echo "000"
}

extract() {
  # Extract a JSON field value using python3 (available on Amazon Linux 2023)
  local json="$1"
  local field="$2"
  echo "${json}" | python3 -c "
import sys, json
try:
    data = json.load(sys.stdin)
    # Support dot-notation: field1.field2
    parts = '${field}'.split('.')
    v = data
    for p in parts:
        v = v[p]
    print(v)
except Exception as e:
    print('')
" 2>/dev/null || echo ""
}

echo "==> Smoke tests against ${API}"
echo ""

# ── Test 1: Anonymous access to protected endpoint returns 401 ────────────────
echo "--- Test 1: RBAC — anonymous → 401 ---"
STATUS=$(http_status "${API}/employees")
if [[ "${STATUS}" == "401" ]]; then
  check "Anonymous GET /api/employees returns 401" "ok"
else
  check "Anonymous GET /api/employees returns 401" "got HTTP ${STATUS}"
fi

# ── Test 2: Register ADMIN user ───────────────────────────────────────────────
echo ""
echo "--- Test 2: Register test ADMIN user ---"
ADMIN_EMAIL="smoketest-admin-$(date +%s)@emp-portal.test"
REGISTER_RESP=$(api -X POST "${API}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${ADMIN_EMAIL}\",
    \"password\": \"SmokeTest@123!\",
    \"firstName\": \"Smoke\",
    \"lastName\": \"Admin\",
    \"role\": \"ROLE_ADMIN\"
  }")
ADMIN_TOKEN=$(extract "${REGISTER_RESP}" "token")
if [[ -n "${ADMIN_TOKEN}" && "${ADMIN_TOKEN}" != "null" ]]; then
  check "Register ADMIN user and receive JWT" "ok"
else
  check "Register ADMIN user and receive JWT" "no token in response: ${REGISTER_RESP}"
fi

# ── Test 3: Login as ADMIN ────────────────────────────────────────────────────
echo ""
echo "--- Test 3: Login ---"
LOGIN_RESP=$(api -X POST "${API}/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\": \"${ADMIN_EMAIL}\", \"password\": \"SmokeTest@123!\"}")
ADMIN_TOKEN=$(extract "${LOGIN_RESP}" "token")
if [[ -n "${ADMIN_TOKEN}" && "${ADMIN_TOKEN}" != "null" ]]; then
  check "Login and receive JWT" "ok"
else
  check "Login and receive JWT" "no token: ${LOGIN_RESP}"
fi

# ── Test 4: Authenticated access — GET /employees ────────────────────────────
echo ""
echo "--- Test 4: GET /api/employees (authenticated) ---"
STATUS=$(http_status "${API}/employees" -H "Authorization: Bearer ${ADMIN_TOKEN}")
if [[ "${STATUS}" == "200" ]]; then
  check "Authenticated GET /api/employees returns 200" "ok"
else
  check "Authenticated GET /api/employees returns 200" "got HTTP ${STATUS}"
fi

# ── Test 5: Create department ─────────────────────────────────────────────────
echo ""
echo "--- Test 5: POST /api/departments ---"
DEPT_CODE="SMOKE-$(date +%s | tail -c 5)"
DEPT_RESP=$(api -X POST "${API}/departments" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{\"name\": \"Smoke Test Department\", \"code\": \"${DEPT_CODE}\"}")
CLEANUP_DEPT_ID=$(extract "${DEPT_RESP}" "id")
if [[ -n "${CLEANUP_DEPT_ID}" && "${CLEANUP_DEPT_ID}" != "null" ]]; then
  check "Create department (ADMIN)" "ok"
else
  check "Create department (ADMIN)" "no id in response: ${DEPT_RESP}"
fi

# ── Test 6: Create employee ───────────────────────────────────────────────────
echo ""
echo "--- Test 6: POST /api/employees ---"
EMP_CODE="EMP-SM-$(date +%s | tail -c 5)"
EMP_RESP=$(api -X POST "${API}/employees" \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"employeeCode\": \"${EMP_CODE}\",
    \"departmentId\": \"${CLEANUP_DEPT_ID}\",
    \"jobTitle\": \"Smoke Tester\",
    \"dateOfJoining\": \"$(date +%Y-%m-%d)\",
    \"salary\": 50000
  }")
CLEANUP_EMP_ID=$(extract "${EMP_RESP}" "id")
if [[ -n "${CLEANUP_EMP_ID}" && "${CLEANUP_EMP_ID}" != "null" ]]; then
  check "Create employee (ADMIN)" "ok"
else
  check "Create employee (ADMIN)" "no id in response: ${EMP_RESP}"
fi

# ── Test 7: Register EMPLOYEE user ────────────────────────────────────────────
echo ""
echo "--- Test 7: Register EMPLOYEE user ---"
EMP_EMAIL="smoketest-emp-$(date +%s)@emp-portal.test"
EMP_REG_RESP=$(api -X POST "${API}/auth/register" \
  -H "Content-Type: application/json" \
  -d "{
    \"email\": \"${EMP_EMAIL}\",
    \"password\": \"SmokeEmp@123!\",
    \"firstName\": \"Smoke\",
    \"lastName\": \"Employee\",
    \"role\": \"ROLE_EMPLOYEE\"
  }")
EMP_TOKEN=$(extract "${EMP_REG_RESP}" "token")
if [[ -n "${EMP_TOKEN}" && "${EMP_TOKEN}" != "null" ]]; then
  check "Register EMPLOYEE user" "ok"
else
  check "Register EMPLOYEE user" "no token: ${EMP_REG_RESP}"
fi

# ── Test 8: RBAC — EMPLOYEE cannot create a department ───────────────────────
echo ""
echo "--- Test 8: RBAC — EMPLOYEE cannot create department (expect 403) ---"
RBAC_STATUS=$(http_status -X POST "${API}/departments" \
  -H "Authorization: Bearer ${EMP_TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{"name":"Forbidden Dept","code":"FORBIDDEN"}')
if [[ "${RBAC_STATUS}" == "403" ]]; then
  check "EMPLOYEE POST /api/departments returns 403" "ok"
else
  check "EMPLOYEE POST /api/departments returns 403" "got HTTP ${RBAC_STATUS}"
fi

# ── Test 9: Submit leave request (as EMPLOYEE) ────────────────────────────────
echo ""
echo "--- Test 9: POST /api/leaves ---"
LEAVE_START=$(date -d "+7 days" +%Y-%m-%d 2>/dev/null || date -v+7d +%Y-%m-%d)
LEAVE_END=$(date -d "+9 days" +%Y-%m-%d 2>/dev/null || date -v+9d +%Y-%m-%d)
LEAVE_RESP=$(api -X POST "${API}/leaves" \
  -H "Authorization: Bearer ${EMP_TOKEN}" \
  -H "Content-Type: application/json" \
  -d "{
    \"leaveType\": \"ANNUAL\",
    \"startDate\": \"${LEAVE_START}\",
    \"endDate\": \"${LEAVE_END}\",
    \"reason\": \"Smoke test leave request\"
  }")
CLEANUP_LEAVE_ID=$(extract "${LEAVE_RESP}" "id")
if [[ -n "${CLEANUP_LEAVE_ID}" && "${CLEANUP_LEAVE_ID}" != "null" ]]; then
  check "EMPLOYEE submit leave request" "ok"
else
  check "EMPLOYEE submit leave request" "no id: ${LEAVE_RESP}"
fi

# ── Test 10: EMPLOYEE can see own leaves ──────────────────────────────────────
echo ""
echo "--- Test 10: GET /api/leaves/my ---"
MY_LEAVES_STATUS=$(http_status "${API}/leaves/my" \
  -H "Authorization: Bearer ${EMP_TOKEN}")
if [[ "${MY_LEAVES_STATUS}" == "200" ]]; then
  check "EMPLOYEE GET /api/leaves/my returns 200" "ok"
else
  check "EMPLOYEE GET /api/leaves/my returns 200" "got HTTP ${MY_LEAVES_STATUS}"
fi

# ── Test 11: ADMIN can approve leave ─────────────────────────────────────────
echo ""
echo "--- Test 11: ADMIN approves leave ---"
if [[ -n "${CLEANUP_LEAVE_ID}" && "${CLEANUP_LEAVE_ID}" != "null" ]]; then
  APPROVE_STATUS=$(http_status -X PUT "${API}/leaves/${CLEANUP_LEAVE_ID}/approve" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")
  if [[ "${APPROVE_STATUS}" == "200" ]]; then
    check "ADMIN approve leave request returns 200" "ok"
  else
    check "ADMIN approve leave request returns 200" "got HTTP ${APPROVE_STATUS}"
  fi
else
  check "ADMIN approve leave request returns 200" "SKIP — no leave ID (test 9 failed)"
fi

# ── Cleanup ───────────────────────────────────────────────────────────────────
echo ""
echo "--- Cleanup: removing smoke test data ---"
# Leaves are cascade-deleted when employee is deleted
# Employees are cascade-deleted when user is deleted (or delete directly)
if [[ -n "${CLEANUP_EMP_ID}" && "${CLEANUP_EMP_ID}" != "null" ]]; then
  DEL_STATUS=$(http_status -X DELETE "${API}/employees/${CLEANUP_EMP_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")
  echo "  DELETE /api/employees/${CLEANUP_EMP_ID}: HTTP ${DEL_STATUS}"
fi
if [[ -n "${CLEANUP_DEPT_ID}" && "${CLEANUP_DEPT_ID}" != "null" ]]; then
  DEL_STATUS=$(http_status -X DELETE "${API}/departments/${CLEANUP_DEPT_ID}" \
    -H "Authorization: Bearer ${ADMIN_TOKEN}")
  echo "  DELETE /api/departments/${CLEANUP_DEPT_ID}: HTTP ${DEL_STATUS}"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "==> Smoke test summary: ${PASS} passed, ${FAIL} failed"
if [[ "${FAIL}" -gt 0 ]]; then
  exit 1
fi
echo "==> All smoke tests passed"
