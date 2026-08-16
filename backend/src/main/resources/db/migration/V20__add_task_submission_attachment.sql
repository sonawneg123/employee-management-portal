-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V20 — Task Submission File Attachments
--  Employee Management Portal — Phase 6B.1
-- ─────────────────────────────────────────────────────────────────────────────
--
--  Adds optional attachment metadata columns to task_submissions.
--  The binary file is NOT stored in the database; only metadata is stored
--  so that the storage back-end (local filesystem today, S3 later) can be
--  changed without touching this schema.
--
--  Columns added:
--    attachment_original_name  — filename as supplied by the browser (sanitised)
--    attachment_stored_name    — server-generated UUID-based storage filename
--    attachment_mime_type      — validated MIME type
--    attachment_size_bytes     — file size in bytes
--    attachment_uploaded_at    — upload timestamp
--    attachment_storage_key    — logical storage path/key (storage-backend-agnostic)
--
--  All columns are nullable — submissions without a file attachment are still valid.
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE task_submissions
    ADD COLUMN attachment_original_name  VARCHAR(255)   NULL COMMENT 'Original filename from browser',
    ADD COLUMN attachment_stored_name    VARCHAR(255)   NULL COMMENT 'UUID-based server-side filename',
    ADD COLUMN attachment_mime_type      VARCHAR(100)   NULL COMMENT 'Validated MIME type',
    ADD COLUMN attachment_size_bytes     BIGINT         NULL COMMENT 'File size in bytes',
    ADD COLUMN attachment_uploaded_at    DATETIME(6)    NULL COMMENT 'When the file was stored',
    ADD COLUMN attachment_storage_key    VARCHAR(512)   NULL COMMENT 'Storage path/key (filesystem or S3 object key)';

-- Index to quickly locate submissions that have an attachment
CREATE INDEX idx_submission_has_attachment
    ON task_submissions (attachment_stored_name);
