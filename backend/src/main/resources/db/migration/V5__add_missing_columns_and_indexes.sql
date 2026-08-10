-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V5
--  1. Add rejection_reason column to leave_requests
--  2. Add attachment_url column to leave_requests
--  3. Extend leave_type ENUM with EMERGENCY and STUDY values
--  4. Add composite indexes for common multi-column filter patterns
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. leave_requests — add rejection_reason ─────────────────────────────────
ALTER TABLE leave_requests
    ADD COLUMN rejection_reason VARCHAR(500) NULL AFTER reason;

-- ── 2. leave_requests — add attachment_url ───────────────────────────────────
ALTER TABLE leave_requests
    ADD COLUMN attachment_url VARCHAR(500) NULL AFTER rejection_reason;

-- ── 3. Extend leave_type ENUM to include EMERGENCY and STUDY ─────────────────
--
--  MySQL requires re-specifying the full ENUM definition when adding values.
--  Existing rows retain their current values; no data migration is needed.
ALTER TABLE leave_requests
    MODIFY COLUMN leave_type
        ENUM('ANNUAL','SICK','MATERNITY','PATERNITY','UNPAID','OTHER','EMERGENCY','STUDY')
        NOT NULL;

-- ── 4. Composite indexes ──────────────────────────────────────────────────────
--
--  NOTE: The following indexes already exist from V2 and are NOT re-created:
--    - idx_employees_status           ON employees(status)
--    - idx_leave_requests_status      ON leave_requests(status)
--    - idx_leave_requests_employee_id ON leave_requests(employee_id)
--    - idx_attendance_employee_id     ON attendance(employee_id)
--
--  New composite indexes that V2 did not include:

CREATE INDEX idx_leave_requests_emp_status
    ON leave_requests (employee_id, status);

CREATE INDEX idx_attendance_date_status
    ON attendance (attendance_date, status);

CREATE INDEX idx_employees_dept_status
    ON employees (department_id, status);
