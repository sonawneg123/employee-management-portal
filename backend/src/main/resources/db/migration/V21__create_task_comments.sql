-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V21 — Task Comments
--  Employee Management Portal — Phase 6C
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE IF NOT EXISTS task_comments (
    id          CHAR(36)    NOT NULL,

    task_id     CHAR(36)    NOT NULL,
    author_id   CHAR(36)    NOT NULL,   -- employee who wrote the comment

    content     TEXT        NOT NULL,
    edited      BOOLEAN     NOT NULL DEFAULT FALSE,

    -- BaseEntity audit columns
    created_at  DATETIME(6) NOT NULL,
    updated_at  DATETIME(6) NOT NULL,
    created_by  VARCHAR(150)    NULL,
    updated_by  VARCHAR(150)    NULL,

    PRIMARY KEY (id),

    CONSTRAINT fk_task_comment_task
        FOREIGN KEY (task_id)
        REFERENCES tasks(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_task_comment_author
        FOREIGN KEY (author_id)
        REFERENCES employees(id)
        ON DELETE CASCADE

) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX idx_task_comment_task_id    ON task_comments (task_id);
CREATE INDEX idx_task_comment_author_id  ON task_comments (author_id);
CREATE INDEX idx_task_comment_created_at ON task_comments (created_at);
