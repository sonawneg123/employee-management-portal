-- V27: Add profile photo columns to the employees table.
-- All columns are nullable so existing rows are not affected.

ALTER TABLE employees
    ADD COLUMN profile_photo_original_name  VARCHAR(255)    NULL,
    ADD COLUMN profile_photo_stored_name    VARCHAR(255)    NULL,
    ADD COLUMN profile_photo_mime_type      VARCHAR(100)    NULL,
    ADD COLUMN profile_photo_size_bytes     BIGINT          NULL,
    ADD COLUMN profile_photo_storage_key    VARCHAR(500)    NULL,
    ADD COLUMN profile_photo_uploaded_at    DATETIME(6)     NULL;
