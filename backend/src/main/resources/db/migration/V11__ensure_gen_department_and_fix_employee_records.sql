-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V11 — Ensure GEN department exists and fix any remaining
--                         ROLE_EMPLOYEE users without an Employee record
--
--  Covers the gap between V10 and any new ROLE_EMPLOYEE registrations that
--  may have been created without an Employee record (e.g., if V10 was already
--  applied before the user registered, and registration failed to auto-create
--  the Employee record for any reason).
--
--  Safe to run multiple times (idempotent guards on every INSERT).
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Step 1: Guarantee the "General" department (code='GEN') exists ────────────
INSERT INTO departments (id, name, code, created_at, updated_at)
SELECT UUID(), 'General', 'GEN', NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE code = 'GEN');

-- ── Step 2: Back-fill Employee records for ROLE_EMPLOYEE users missing one ────
--
--  For any user that has the ROLE_EMPLOYEE role but no linked Employee record,
--  create one using the 'GEN' department as default.
--  A REG- prefixed code is generated to distinguish auto-created records.
--
INSERT INTO employees
    (id, user_id, employee_code, department_id, job_title,
     date_of_joining, salary, status, created_at, updated_at)
SELECT
    UUID(),
    u.id,
    CONCAT('REG-', LPAD(
        MOD(CONV(SUBSTRING(HEX(u.id), 1, 8), 16, 10), 10000000000),
        10, '0'
    )),
    d.id,
    'Employee',
    CURDATE(),
    0.00,
    'ACTIVE',
    NOW(),
    NOW()
FROM users u
JOIN user_roles ur ON ur.user_id = u.id
JOIN roles r        ON r.id = ur.role_id AND r.name = 'ROLE_EMPLOYEE'
JOIN departments d  ON d.code = 'GEN'
WHERE NOT EXISTS (
    SELECT 1 FROM employees e WHERE e.user_id = u.id
);

-- ── Step 3: Verify ─────────────────────────────────────────────────────────────
--  (informational — no changes)
--  After this migration every ROLE_EMPLOYEE user should have exactly one
--  Employee record linked via employees.user_id = users.id.
