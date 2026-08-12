-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V7 — Seed default admin / HR / manager accounts
--
--  Provides working login credentials so the application is functional on a
--  fresh deployment without manual DB inserts.
--
--  ┌──────────────────────┬─────────────────────┬─────────────┐
--  │ Email                │ Password            │ Role        │
--  ├──────────────────────┼─────────────────────┼─────────────┤
--  │ admin@company.com    │ Admin@1234!         │ ROLE_ADMIN  │
--  │ hr@company.com       │ HR@1234!            │ ROLE_HR     │
--  │ manager@company.com  │ Manager@1234!       │ ROLE_MANAGER│
--  └──────────────────────┴─────────────────────┴─────────────┘
--
--  Passwords are BCrypt-hashed (cost factor 12).
--  Change these credentials immediately in any production environment.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Seed roles (idempotent — skip if already present from V1) ─────────────────
INSERT IGNORE INTO roles (id, name)
VALUES
    ('00000000-0000-0000-0000-000000000001', 'ROLE_ADMIN'),
    ('00000000-0000-0000-0000-000000000002', 'ROLE_HR'),
    ('00000000-0000-0000-0000-000000000003', 'ROLE_MANAGER'),
    ('00000000-0000-0000-0000-000000000004', 'ROLE_EMPLOYEE');

-- ── Seed default admin user ───────────────────────────────────────────────────
-- Password: Admin@1234!  (BCrypt strength 12)
INSERT IGNORE INTO users (id, email, password_hash, first_name, last_name,
                          is_enabled, is_locked, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000001',
    'admin@company.com',
    '$2a$12$zS5/HqMKxeMeaGTqjdZoQupaMqI.HiZGZyy3Q5bS8nnGlswtabC5G',
    'System',
    'Admin',
    1, 0, NOW(), NOW()
);

-- ── Seed default HR user ──────────────────────────────────────────────────────
-- Password: HR@1234!  (BCrypt strength 12)
INSERT IGNORE INTO users (id, email, password_hash, first_name, last_name,
                          is_enabled, is_locked, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000002',
    'hr@company.com',
    '$2a$12$LSljb6P2q2cvn72mgyGUludK4ttN4BZXcFlGukt.XIjFDMxlBP74C',
    'HR',
    'Manager',
    1, 0, NOW(), NOW()
);

-- ── Seed default manager user ─────────────────────────────────────────────────
-- Password: Manager@1234!  (BCrypt strength 12)
INSERT IGNORE INTO users (id, email, password_hash, first_name, last_name,
                          is_enabled, is_locked, created_at, updated_at)
VALUES (
    '10000000-0000-0000-0000-000000000003',
    'manager@company.com',
    '$2a$12$L1BuA4pWjGiX6jhp/fwBM.6cSvQaDohyDPQ/JMgvoBtjfKSR39NZC',
    'Team',
    'Manager',
    1, 0, NOW(), NOW()
);

-- ── Assign roles to seed users ────────────────────────────────────────────────
INSERT IGNORE INTO user_roles (user_id, role_id)
VALUES
    -- admin@company.com → ROLE_ADMIN
    ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001'),
    -- hr@company.com → ROLE_HR
    ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002'),
    -- manager@company.com → ROLE_MANAGER
    ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003');
