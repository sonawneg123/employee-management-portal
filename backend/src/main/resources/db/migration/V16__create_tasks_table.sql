-- ─────────────────────────────────────────────────────────────
--  Flyway Migration V16 — Tasks Table
--  Employee Management Portal — Phase 6A
-- ─────────────────────────────────────────────────────────────

-- ── Tasks ─────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tasks (
    id                      CHAR(36)        NOT NULL,

    title                   VARCHAR(255)    NOT NULL,
    description             TEXT                NULL,
    guidelines              TEXT                NULL,
    acceptance_criteria     TEXT                NULL,

    -- Employee assigned to complete the task (nullable: DRAFT tasks may be unassigned)
    assigned_employee_id    CHAR(36)            NULL,

    -- Manager / privileged user who created the task (nullable: system-created)
    created_by_employee_id  CHAR(36)            NULL,

    priority    ENUM('LOW','MEDIUM','HIGH','CRITICAL')                              NOT NULL DEFAULT 'MEDIUM',
    status      ENUM('DRAFT','ASSIGNED','IN_PROGRESS','SUBMITTED','COMPLETED','CHANGES_REQUESTED','REJECTED')
                                                                                   NOT NULL DEFAULT 'DRAFT',

    due_date         DATE            NULL,
    estimated_hours  DECIMAL(6,2)    NULL,
    category         VARCHAR(100)    NULL,

    -- BaseEntity audit columns
    created_at   DATETIME(6)     NOT NULL,
    updated_at   DATETIME(6)     NOT NULL,
    created_by   VARCHAR(150)        NULL,
    updated_by   VARCHAR(150)        NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_task_assigned_employee
        FOREIGN KEY (assigned_employee_id)
        REFERENCES employees(id)
        ON DELETE SET NULL,

    CONSTRAINT fk_task_created_by_employee
        FOREIGN KEY (created_by_employee_id)
        REFERENCES employees(id)
        ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Indexes for common query patterns ─────────────────────────

-- Tasks assigned to a specific employee (employee task list)
CREATE INDEX idx_task_assigned_employee
    ON tasks (assigned_employee_id);

-- Tasks created by a specific manager
CREATE INDEX idx_task_created_by_employee
    ON tasks (created_by_employee_id);

-- Filter by status (e.g. IN_PROGRESS, SUBMITTED)
CREATE INDEX idx_task_status
    ON tasks (status);

-- Filter by priority
CREATE INDEX idx_task_priority
    ON tasks (priority);

-- Filter / sort by due date (overdue queries)
CREATE INDEX idx_task_due_date
    ON tasks (due_date);

-- Combined index for the most common manager dashboard query:
-- tasks by assignee narrowed by status
CREATE INDEX idx_task_assigned_status
    ON tasks (assigned_employee_id, status);
