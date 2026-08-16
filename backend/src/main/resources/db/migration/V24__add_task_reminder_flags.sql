-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V24 — Task Reminder Deduplication Flags
--  Employee Management Portal — Phase 6C
-- ─────────────────────────────────────────────────────────────────────────────

-- Add boolean flags to track which reminder notifications have already been sent,
-- preventing duplicate notifications on repeated scheduler runs.

ALTER TABLE tasks
    ADD COLUMN reminder_24h_sent          BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN reminder_2h_sent           BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN overdue_notification_sent  BOOLEAN NOT NULL DEFAULT FALSE;

-- Index to support the scheduler's query: non-completed tasks with a due date
-- where at least one reminder flag is still false.
CREATE INDEX idx_task_reminders
    ON tasks (status, due_date, reminder_24h_sent, reminder_2h_sent, overdue_notification_sent);
