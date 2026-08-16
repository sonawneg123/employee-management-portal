-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V18 — Fix notifications.related_task_id column type
--  Employee Management Portal — Phase 6A.1 hotfix
-- ─────────────────────────────────────────────────────────────────────────────
--
--  Root cause
--  ----------
--  V17 created notifications.related_task_id as CHAR(36), which is the correct
--  UUID storage type used by every other table in this project.
--
--  However, the Notification.relatedTaskId Java field was missing the
--  @JdbcTypeCode(SqlTypes.CHAR) annotation that BaseEntity applies to its `id`
--  field.  Without that annotation, Hibernate 6 defaults to BINARY(16) for bare
--  UUID fields, causing the schema-validation error:
--
--    found [char (Types#CHAR)], but expecting [binary(16) (Types#BINARY)]
--
--  The annotation has now been added to the field.  Hibernate therefore expects
--  CHAR(36) for this column, which is exactly what V17 created.
--
--  This migration explicitly re-declares the column as CHAR(36) to guarantee
--  the correct type in any environment, including those where V17 may have been
--  applied by a Hibernate ddl-auto=update run that created it as BINARY(16).
--
--  Steps
--  -----
--  1. Drop the FK constraint that references notifications.related_task_id
--     (fk_notification_task — pointing to tasks.id) because MySQL requires the
--     FK to be dropped before the referenced column type can be changed.
--  2. Drop the index on related_task_id (idx_notification_task_id) for the same
--     reason.
--  3. MODIFY the column to CHAR(36) NULL with utf8mb4_unicode_ci collation,
--     consistent with every other UUID column in the schema.
--  4. Re-create the index.
--  5. Re-create the FK constraint.
--
--  Safety
--  ------
--  * If the column is already CHAR(36) the MODIFY is a structural no-op and
--    MySQL returns success without modifying any row data.
--  * Existing notification records are fully preserved.
--  * The FK and index are restored with identical semantics.
-- ─────────────────────────────────────────────────────────────────────────────

-- Step 1: Drop the foreign-key constraint so MySQL allows the column type change.
ALTER TABLE notifications
    DROP FOREIGN KEY fk_notification_task;

-- Step 2: Drop the index on related_task_id (required before column MODIFY on
--         indexed columns in some MySQL versions).
DROP INDEX idx_notification_task_id ON notifications;

-- Step 3: Re-declare the column as CHAR(36) with the project-standard collation.
--         This is a no-op if it is already CHAR(36); it converts BINARY(16) to
--         CHAR(36) safely (UUID string values are preserved as text; NULL values
--         remain NULL).
ALTER TABLE notifications
    MODIFY COLUMN related_task_id CHAR(36)
        CHARACTER SET utf8mb4
        COLLATE utf8mb4_unicode_ci
        NULL;

-- Step 4: Re-create the index.
CREATE INDEX idx_notification_task_id
    ON notifications (related_task_id);

-- Step 5: Re-create the foreign-key constraint with identical ON DELETE behaviour.
ALTER TABLE notifications
    ADD CONSTRAINT fk_notification_task
        FOREIGN KEY (related_task_id)
        REFERENCES tasks (id)
        ON DELETE SET NULL;
