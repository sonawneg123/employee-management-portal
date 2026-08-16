-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V17 — Notifications & Task Activities
--  Employee Management Portal — Phase 6A.1
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Task Activities ───────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS task_activities (
    id           CHAR(36)    NOT NULL,

    task_id      CHAR(36)    NOT NULL,
    actor_id     CHAR(36)        NULL,   -- employee who performed the action

    event_type   VARCHAR(50) NOT NULL,  -- e.g. TASK_ASSIGNED, TASK_STARTED, TASK_STATUS_CHANGED
    description  TEXT        NOT NULL,
    from_status  VARCHAR(30)     NULL,
    to_status    VARCHAR(30)     NULL,

    -- BaseEntity audit columns
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    created_by   VARCHAR(150)    NULL,
    updated_by   VARCHAR(150)    NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_task_activity_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_activity_actor
        FOREIGN KEY (actor_id)
        REFERENCES employees(id)
        ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_task_activity_task_id   ON task_activities (task_id);
CREATE INDEX idx_task_activity_actor_id  ON task_activities (actor_id);
CREATE INDEX idx_task_activity_created   ON task_activities (created_at);

-- ── Notifications ─────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notifications (
    id               CHAR(36)    NOT NULL,

    recipient_id     CHAR(36)    NOT NULL,  -- employee who receives the notification
    type             VARCHAR(40) NOT NULL,  -- NotificationType enum value
    title            VARCHAR(255) NOT NULL,
    message          TEXT        NOT NULL,
    related_task_id  CHAR(36)        NULL,  -- optional reference to a task
    is_read          BOOLEAN     NOT NULL DEFAULT FALSE,

    -- BaseEntity audit columns
    created_at   DATETIME(6) NOT NULL,
    updated_at   DATETIME(6) NOT NULL,
    created_by   VARCHAR(150)    NULL,
    updated_by   VARCHAR(150)    NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_notification_recipient
        FOREIGN KEY (recipient_id)
        REFERENCES employees(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_notification_task
        FOREIGN KEY (related_task_id)
        REFERENCES tasks(id)
        ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index on recipient + unread for the bell badge query (hot path)
CREATE INDEX idx_notification_recipient_unread
    ON notifications (recipient_id, is_read);

-- Index for sorting by newest first
CREATE INDEX idx_notification_created_at
    ON notifications (created_at DESC);

-- Index for looking up notifications by task
CREATE INDEX idx_notification_task_id
    ON notifications (related_task_id);
