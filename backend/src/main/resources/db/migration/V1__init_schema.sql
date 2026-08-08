-- ─────────────────────────────────────────────────────────────
--  Flyway Migration V1 — Initial Schema (UUID primary keys)
--  Employee Management Portal
-- ─────────────────────────────────────────────────────────────

-- ── Roles ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS roles (
    id    CHAR(36)      NOT NULL,
    name  VARCHAR(50)   NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_roles_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Users ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id              CHAR(36)        NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   VARCHAR(255)    NOT NULL,
    first_name      VARCHAR(100)    NOT NULL,
    last_name       VARCHAR(100)    NOT NULL,
    is_enabled      TINYINT(1)      NOT NULL DEFAULT 1,
    is_locked       TINYINT(1)      NOT NULL DEFAULT 0,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)        NULL,
    updated_by      VARCHAR(150)        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_users_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── User–Role join ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_roles (
    user_id CHAR(36) NOT NULL,
    role_id CHAR(36) NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_ur_user FOREIGN KEY (user_id) REFERENCES users(id)  ON DELETE CASCADE,
    CONSTRAINT fk_ur_role FOREIGN KEY (role_id) REFERENCES roles(id)  ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Departments ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS departments (
    id          CHAR(36)        NOT NULL,
    name        VARCHAR(100)    NOT NULL,
    code        VARCHAR(20)     NOT NULL,
    created_at  DATETIME(6)     NOT NULL,
    updated_at  DATETIME(6)     NOT NULL,
    created_by  VARCHAR(150)        NULL,
    updated_by  VARCHAR(150)        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_dept_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Employees ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS employees (
    id              CHAR(36)        NOT NULL,
    user_id         CHAR(36)            NULL,
    employee_code   VARCHAR(20)     NOT NULL,
    department_id   CHAR(36)        NOT NULL,
    job_title       VARCHAR(150)    NOT NULL,
    phone           VARCHAR(20)         NULL,
    address         VARCHAR(255)        NULL,
    date_of_joining DATE            NOT NULL,
    salary          DECIMAL(15,2)   NOT NULL DEFAULT 0.00,
    status          ENUM('ACTIVE','INACTIVE','ON_LEAVE','TERMINATED') NOT NULL DEFAULT 'ACTIVE',
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)        NULL,
    updated_by      VARCHAR(150)        NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_emp_code (employee_code),
    UNIQUE KEY uq_emp_user (user_id),
    CONSTRAINT fk_emp_user FOREIGN KEY (user_id)       REFERENCES users(id)       ON DELETE SET NULL,
    CONSTRAINT fk_emp_dept FOREIGN KEY (department_id) REFERENCES departments(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Leave Requests ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS leave_requests (
    id           CHAR(36)        NOT NULL,
    employee_id  CHAR(36)        NOT NULL,
    leave_type   ENUM('ANNUAL','SICK','MATERNITY','PATERNITY','UNPAID','OTHER') NOT NULL,
    start_date   DATE            NOT NULL,
    end_date     DATE            NOT NULL,
    reason       VARCHAR(500)        NULL,
    status       ENUM('PENDING','APPROVED','REJECTED','CANCELLED') NOT NULL DEFAULT 'PENDING',
    reviewed_by  CHAR(36)            NULL,
    reviewed_at  DATETIME(6)         NULL,
    created_at   DATETIME(6)     NOT NULL,
    updated_at   DATETIME(6)     NOT NULL,
    created_by   VARCHAR(150)        NULL,
    updated_by   VARCHAR(150)        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_lr_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Attendance ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS attendance (
    id              CHAR(36)    NOT NULL,
    employee_id     CHAR(36)    NOT NULL,
    attendance_date DATE        NOT NULL,
    check_in_time   TIME            NULL,
    check_out_time  TIME            NULL,
    status          ENUM('PRESENT','ABSENT','HALF_DAY','WORK_FROM_HOME','ON_LEAVE') NOT NULL DEFAULT 'PRESENT',
    notes           VARCHAR(255)    NULL,
    created_at      DATETIME(6) NOT NULL,
    updated_at      DATETIME(6) NOT NULL,
    created_by      VARCHAR(150)    NULL,
    updated_by      VARCHAR(150)    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_attendance_emp_date (employee_id, attendance_date),
    CONSTRAINT fk_att_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Performance Reviews ───────────────────────────────────────
CREATE TABLE IF NOT EXISTS performance_reviews (
    id              CHAR(36)        NOT NULL,
    employee_id     CHAR(36)        NOT NULL,
    reviewer_id     CHAR(36)            NULL,
    review_period   VARCHAR(50)     NOT NULL,
    rating          TINYINT         NOT NULL,
    comments        TEXT                NULL,
    goals           TEXT                NULL,
    review_date     DATE            NOT NULL,
    created_at      DATETIME(6)     NOT NULL,
    updated_at      DATETIME(6)     NOT NULL,
    created_by      VARCHAR(150)        NULL,
    updated_by      VARCHAR(150)        NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_pr_employee FOREIGN KEY (employee_id) REFERENCES employees(id) ON DELETE CASCADE,
    CONSTRAINT chk_rating     CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Refresh Tokens ────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS refresh_tokens (
    id          CHAR(36)        NOT NULL,
    user_id     CHAR(36)        NOT NULL,
    token       VARCHAR(512)    NOT NULL,
    expiry_date DATETIME(6)     NOT NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uq_rt_token (token),
    CONSTRAINT fk_rt_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Seed: default roles ───────────────────────────────────────
INSERT IGNORE INTO roles (id, name) VALUES
    (UUID(), 'ROLE_ADMIN'),
    (UUID(), 'ROLE_HR'),
    (UUID(), 'ROLE_MANAGER'),
    (UUID(), 'ROLE_EMPLOYEE');
