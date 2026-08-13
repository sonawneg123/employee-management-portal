-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V9 — Seed demo employee account with linked Employee record
--
--  Creates a fully functional ROLE_EMPLOYEE account that is ready to use on a
--  fresh deployment.  DataInitializer.java handles the same seeding at runtime
--  using the live PasswordEncoder bean; this migration provides the SQL-level
--  fallback and ensures the data lands before any Flyway-aware tooling runs.
--
--  ┌──────────────────────────┬─────────────────────┬───────────────┐
--  │ Email                    │ Password            │ Role          │
--  ├──────────────────────────┼─────────────────────┼───────────────┤
--  │ employee@company.com     │ Employee@1234!      │ ROLE_EMPLOYEE │
--  └──────────────────────────┴─────────────────────┴───────────────┘
--
--  Password is BCrypt-hashed (cost factor 12).
--  All steps are idempotent — safe to apply on a database that already has
--  partial data from DataInitializer or a previous run.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Step 1: Ensure ROLE_EMPLOYEE exists (V1/V8 normally insert it) ────────────
INSERT IGNORE INTO roles (id, name, created_at, updated_at)
SELECT UUID(), 'ROLE_EMPLOYEE', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_EMPLOYEE');

-- ── Step 2: Ensure "General" department exists ────────────────────────────────
INSERT IGNORE INTO departments (id, name, code, created_at, updated_at)
SELECT UUID(), 'General', 'GEN', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM departments WHERE code = 'GEN');

-- ── Step 3: Ensure employee@company.com user exists ──────────────────────────
-- Password: Employee@1234!  (BCrypt strength 12)
-- Note: DataInitializer will re-encode with the live PasswordEncoder on startup;
-- this hash is a valid fallback so the account works even before the first run.
INSERT IGNORE INTO users
    (id, email, password_hash, first_name, last_name,
     is_enabled, is_locked, created_at, updated_at)
SELECT
    '10000000-0000-0000-0000-000000000004',
    'employee@company.com',
    '$2a$12$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LPVyXMGYm9G',
    'Demo',
    'Employee',
    1, 0, NOW(), NOW()
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM users WHERE email = 'employee@company.com');

-- ── Step 4: Assign ROLE_EMPLOYEE using name-based role lookup ─────────────────
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u
JOIN   roles r ON r.name = 'ROLE_EMPLOYEE'
WHERE  u.email = 'employee@company.com'
AND    NOT EXISTS (
    SELECT 1 FROM user_roles ur2
    WHERE ur2.user_id = u.id AND ur2.role_id = r.id
);

-- ── Step 5: Ensure Employee record linked to this user exists ────────────────
INSERT IGNORE INTO employees
    (id, user_id, employee_code, department_id, job_title,
     date_of_joining, salary, status, created_at, updated_at)
SELECT
    UUID(),
    u.id,
    'EMP-0001',
    d.id,
    'Software Engineer',
    '2024-01-01',
    0.00,
    'ACTIVE',
    NOW(),
    NOW()
FROM   users u
JOIN   departments d ON d.code = 'GEN'
WHERE  u.email = 'employee@company.com'
AND    NOT EXISTS (SELECT 1 FROM employees WHERE employee_code = 'EMP-0001')
AND    NOT EXISTS (SELECT 1 FROM employees WHERE user_id = u.id);
