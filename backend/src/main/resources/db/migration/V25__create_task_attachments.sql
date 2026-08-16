-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V25 — Task Attachments
--  Employee Management Portal — Phase 6C
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS task_attachments (
    id              CHAR(36)        NOT NULL,

    task_id         CHAR(36)        NOT NULL,
    uploader_id     CHAR(36)            NULL,   -- employee who uploaded (SET NULL on delete)

    original_name   VARCHAR(255)    NOT NULL,
    stored_name     VARCHAR(255)    NOT NULL,
    mime_type       VARCHAR(120)    NOT NULL,
    size_bytes      BIGINT          NOT NULL,
    storage_key     VARCHAR(512)    NOT NULL,   -- e.g. tasks/{taskId}/{uuid}.{ext}

    -- BaseEntity audit columns
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    created_by  VARCHAR(150)    NULL,
    updated_by  VARCHAR(150)    NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_task_attachment_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_attachment_uploader
        FOREIGN KEY (uploader_id)
        REFERENCES employees(id)
        ON DELETE SET NULL

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_task_attachment_task_id ON task_attachments (task_id);
