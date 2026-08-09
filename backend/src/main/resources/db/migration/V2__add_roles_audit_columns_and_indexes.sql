-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V2
--  1. Add missing auditing columns to the `roles` table so that
--     Role (which extends BaseEntity) passes Hibernate schema validation.
--  2. Add performance indexes on the most frequently filtered / joined columns.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. roles — add BaseEntity audit columns ───────────────────────────────────
--
--  Role extends BaseEntity, so Hibernate expects created_at, updated_at,
--  created_by, and updated_by columns.  They were absent from V1, causing
--  spring.jpa.hibernate.ddl-auto=validate to fail at startup.
--
--  All four columns are nullable to preserve the existing seed rows inserted
--  in V1 without requiring a back-fill.

ALTER TABLE roles
    ADD COLUMN created_at  DATETIME(6)  NULL,
    ADD COLUMN updated_at  DATETIME(6)  NULL,
    ADD COLUMN created_by  VARCHAR(150) NULL,
    ADD COLUMN updated_by  VARCHAR(150) NULL;

-- ── 2. Indexes ────────────────────────────────────────────────────────────────
--
--  Only indexes that provide a measurable benefit are added:
--    - employees(user_id)       → findByUserId (ownership checks on every request)
--    - employees(department_id) → findByDepartmentId + JOIN in searchByKeyword
--    - employees(status)        → findByStatus (list-by-status endpoint)
--    - leave_requests(employee_id) → findByEmployeeId (employee self-service queries)
--    - leave_requests(status)      → findByStatus (HR approval queue)
--    - leave_requests(start_date, end_date) → date-range queries (attendance, reporting)
--    - attendance(employee_id)  → findByEmployeeId + findByEmployeeIdAndAttendanceDate
--
--  Columns already covered by an existing index are NOT re-indexed:
--    - users(email)          → covered by uq_users_email (unique key = index)
--    - employees(employee_code) → covered by uq_emp_code
--    - departments(code)     → covered by uq_dept_code

CREATE INDEX idx_employees_user_id
    ON employees (user_id);

CREATE INDEX idx_employees_department_id
    ON employees (department_id);

CREATE INDEX idx_employees_status
    ON employees (status);

CREATE INDEX idx_leave_requests_employee_id
    ON leave_requests (employee_id);

CREATE INDEX idx_leave_requests_status
    ON leave_requests (status);

CREATE INDEX idx_leave_requests_dates
    ON leave_requests (start_date, end_date);

CREATE INDEX idx_attendance_employee_id
    ON attendance (employee_id);
