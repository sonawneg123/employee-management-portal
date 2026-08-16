-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V19 — Task Submissions
--  Employee Management Portal — Phase 6B
-- ─────────────────────────────────────────────────────────────────────────────
--
--  Creates the task_submissions table, which stores employee work submissions
--  for manager review.
--
--  A task may accumulate multiple submissions over its lifecycle (one per
--  review round): each time a manager requests changes and the employee
--  resubmits, a new revision is stored against the same task.
--
--  The latest submission for a task is determined by MAX(submitted_at)
--  or by querying with ORDER BY submitted_at DESC LIMIT 1.
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS task_submissions (
    id                  CHAR(36)        NOT NULL,

    -- The task being submitted
    task_id             CHAR(36)        NOT NULL,

    -- Employee who submitted the work
    submitted_by_id     CHAR(36)        NOT NULL,

    -- Submission content
    submission_notes    TEXT                NULL,
    work_completed      TEXT                NULL,
    additional_comments TEXT                NULL,

    -- Submission timestamp (updated on resubmit)
    submitted_at        DATETIME(6)     NOT NULL,

    -- Review lifecycle
    review_status       ENUM('PENDING_REVIEW','APPROVED','CHANGES_REQUESTED')
                                        NOT NULL DEFAULT 'PENDING_REVIEW',
    review_comment      TEXT                NULL,
    reviewed_by_id      CHAR(36)            NULL,
    reviewed_at         DATETIME(6)         NULL,

    -- BaseEntity audit columns
    created_at   DATETIME(6)     NOT NULL,
    updated_at   DATETIME(6)     NOT NULL,
    created_by   VARCHAR(150)        NULL,
    updated_by   VARCHAR(150)        NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_submission_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_submission_submitted_by
        FOREIGN KEY (submitted_by_id)
        REFERENCES employees(id)
        ON DELETE RESTRICT,

    CONSTRAINT fk_submission_reviewed_by
        FOREIGN KEY (reviewed_by_id)
        REFERENCES employees(id)
        ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ── Indexes ───────────────────────────────────────────────────────────────────

-- Task submissions by task (fetch all submissions for a task)
CREATE INDEX idx_submission_task_id
    ON task_submissions (task_id);

-- Submissions by submitting employee
CREATE INDEX idx_submission_submitted_by
    ON task_submissions (submitted_by_id);

-- Filter / sort by submission time (latest submission per task)
CREATE INDEX idx_submission_submitted_at
    ON task_submissions (submitted_at DESC);

-- Filter by review status (e.g., all PENDING_REVIEW for manager dashboard)
CREATE INDEX idx_submission_review_status
    ON task_submissions (review_status);

-- Combined: task submissions ordered by time (most common query pattern)
CREATE INDEX idx_submission_task_submitted_at
    ON task_submissions (task_id, submitted_at DESC);
