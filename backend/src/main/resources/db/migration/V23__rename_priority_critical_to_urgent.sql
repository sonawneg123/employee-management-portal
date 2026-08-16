-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V23 — Rename Task Priority CRITICAL → URGENT
--  Employee Management Portal — Phase 6C
--
--  MySQL ENUM constraint: you cannot UPDATE a row to a value that is not yet
--  in the ENUM definition.  The correct order is therefore:
--
--    Step 1  Add URGENT to the ENUM (alongside the existing CRITICAL).
--    Step 2  Migrate existing CRITICAL rows to URGENT.
--    Step 3  Remove CRITICAL from the ENUM now that no rows reference it.
--
--  This three-step approach is safe for any MySQL 8.x / InnoDB setup and
--  preserves all existing data.
-- ─────────────────────────────────────────────────────────────────────────────

-- Step 1: Expand the ENUM to include URGENT alongside the legacy CRITICAL value.
ALTER TABLE tasks MODIFY COLUMN priority
    ENUM('LOW','MEDIUM','HIGH','CRITICAL','URGENT') NOT NULL DEFAULT 'MEDIUM';

-- Step 2: Migrate all existing CRITICAL rows to URGENT.
UPDATE tasks SET priority = 'URGENT' WHERE priority = 'CRITICAL';

-- Step 3: Remove CRITICAL from the ENUM now that no rows reference it.
ALTER TABLE tasks MODIFY COLUMN priority
    ENUM('LOW','MEDIUM','HIGH','URGENT') NOT NULL DEFAULT 'MEDIUM';
