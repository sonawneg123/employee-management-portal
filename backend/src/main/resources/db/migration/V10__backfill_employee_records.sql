-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V10 — Back-fill Employee records for ROLE_EMPLOYEE users
--
--  For any user assigned ROLE_EMPLOYEE that does not yet have a linked Employee
--  record, this migration creates one automatically using the General department
--  (code = 'GEN') as the default department.
--
--  This ensures that users registered before the auto-create fix in
--  AuthServiceImpl can still submit leave requests and access employee features.
--
--  Safe to run multiple times (WHERE NOT EXISTS guards prevent duplicates).
-- ─────────────────────────────────────────────────────────────────────────────

-- Back-fill Employee records for ROLE_EMPLOYEE users that have no employee record yet.
-- Uses the 'GEN' department as default; uses a REG-prefixed code to distinguish
-- auto-generated records from HR-managed records.
INSERT INTO employees
    (id, user_id, employee_code, department_id, job_title,
     date_of_joining, salary, status, created_at, updated_at)
SELECT
    UUID(),
    u.id,
    CONCAT('REG-', LPAD(CONV(SUBSTRING(HEX(u.id), 1, 8), 16, 10), 10, '0')),
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
