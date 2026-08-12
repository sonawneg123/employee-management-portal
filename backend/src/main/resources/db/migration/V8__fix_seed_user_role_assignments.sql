-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V8 — Correct seed-user role assignments
--
--  ROOT CAUSE (V7 failure):
--
--  V1 seeded the four application roles using UUID() — so each role received
--  a random UUID as its primary key.  V7 then attempted to INSERT those same
--  roles again with *different*, hard-coded UUIDs
--  (e.g. '00000000-0000-0000-0000-000000000001').
--
--  MySQL's INSERT IGNORE on the roles table observed the UNIQUE constraint on
--  the `name` column, suppressed the duplicate-name error, and left the rows
--  untouched — meaning the hard-coded role UUIDs never landed in the table.
--
--  V7 then inserted into user_roles referencing those non-existent hard-coded
--  UUIDs.  InnoDB fired a foreign-key violation.  INSERT IGNORE swallowed the
--  FK error silently, so every user_roles row was skipped.  Flyway recorded V7
--  as successful (it inspects exit codes / checksums, not row counts), leaving
--  admin@company.com, hr@company.com and manager@company.com with zero role
--  assignments and therefore no authorities.
--
--  FIX STRATEGY (V8):
--
--  1. Look up each role's real UUID by its NAME (never use a hard-coded UUID).
--  2. Insert into user_roles only if the mapping does not already exist.
--  3. Ensure the three seed users exist (idempotent — INSERT IGNORE on email).
--  4. All operations are idempotent — safe to re-run or apply on a database
--     where V7 partially succeeded.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Step 1: Ensure all four application roles exist ───────────────────────────
--
--  V1 already inserts these with UUID(); this guard catches any environment
--  where V1 was not applied (e.g. a fresh schema created outside Flyway).
--  INSERT IGNORE is safe: if the name already exists the row is skipped.

INSERT IGNORE INTO roles (id, name, created_at, updated_at)
SELECT UUID(), 'ROLE_ADMIN',    NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_ADMIN');

INSERT IGNORE INTO roles (id, name, created_at, updated_at)
SELECT UUID(), 'ROLE_HR',       NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_HR');

INSERT IGNORE INTO roles (id, name, created_at, updated_at)
SELECT UUID(), 'ROLE_MANAGER',  NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_MANAGER');

INSERT IGNORE INTO roles (id, name, created_at, updated_at)
SELECT UUID(), 'ROLE_EMPLOYEE', NOW(), NOW() FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM roles WHERE name = 'ROLE_EMPLOYEE');

-- ── Step 2: Ensure the three seed users exist ─────────────────────────────────
--
--  V7 already inserted these with INSERT IGNORE, so on a normal database they
--  already exist.  This block is the safety net for any environment where V7
--  was rolled back or never applied.
--
--  Passwords: Admin@1234! / HR@1234! / Manager@1234!  (BCrypt strength 12)

INSERT IGNORE INTO users
    (id, email, password_hash, first_name, last_name,
     is_enabled, is_locked, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'admin@company.com',
    '$2a$12$zS5/HqMKxeMeaGTqjdZoQupaMqI.HiZGZyy3Q5bS8nnGlswtabC5G',
    'System', 'Admin',
    1, 0, NOW(), NOW()
);

INSERT IGNORE INTO users
    (id, email, password_hash, first_name, last_name,
     is_enabled, is_locked, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000002',
    'hr@company.com',
    '$2a$12$LSljb6P2q2cvn72mgyGUludK4ttN4BZXcFlGukt.XIjFDMxlBP74C',
    'HR', 'Manager',
    1, 0, NOW(), NOW()
);

INSERT IGNORE INTO users
    (id, email, password_hash, first_name, last_name,
     is_enabled, is_locked, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000003',
    'manager@company.com',
    '$2a$12$L1BuA4pWjGiX6jhp/fwBM.6cSvQaDohyDPQ/JMgvoBtjfKSR39NZC',
    'Team', 'Manager',
    1, 0, NOW(), NOW()
);

-- ── Step 3: Assign roles using name-based lookup (never hard-coded UUIDs) ─────
--
--  Each INSERT ... SELECT resolves the real UUID of the role from the roles
--  table by name, then inserts only if that (user_id, role_id) pair is absent.
--  This is the correct pattern for any environment regardless of which UUID
--  V1 originally assigned to each role.

-- admin@company.com → ROLE_ADMIN
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u
JOIN   roles r ON r.name = 'ROLE_ADMIN'
WHERE  u.email = 'admin@company.com'
AND    NOT EXISTS (
    SELECT 1 FROM user_roles ur2
    WHERE ur2.user_id = u.id AND ur2.role_id = r.id
);

-- hr@company.com → ROLE_HR
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u
JOIN   roles r ON r.name = 'ROLE_HR'
WHERE  u.email = 'hr@company.com'
AND    NOT EXISTS (
    SELECT 1 FROM user_roles ur2
    WHERE ur2.user_id = u.id AND ur2.role_id = r.id
);

-- manager@company.com → ROLE_MANAGER
INSERT IGNORE INTO user_roles (user_id, role_id)
SELECT u.id, r.id
FROM   users u
JOIN   roles r ON r.name = 'ROLE_MANAGER'
WHERE  u.email = 'manager@company.com'
AND    NOT EXISTS (
    SELECT 1 FROM user_roles ur2
    WHERE ur2.user_id = u.id AND ur2.role_id = r.id
);
