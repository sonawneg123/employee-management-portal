-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V22 — Task Category Enum Conversion
--  Employee Management Portal — Phase 6C
-- ─────────────────────────────────────────────────────────────────────────────

-- Normalize existing free-text category values to enum constants.
-- Any value that does not match a known enum name is set to 'OTHER'.

UPDATE tasks
SET category = CASE
    WHEN UPPER(category) IN ('DEVELOPMENT','DEV','FEATURE','IMPLEMENTATION') THEN 'DEVELOPMENT'
    WHEN UPPER(category) IN ('TESTING','QA','TEST') THEN 'TESTING'
    WHEN UPPER(category) IN ('DOCUMENTATION','DOCS','DOC') THEN 'DOCUMENTATION'
    WHEN UPPER(category) IN ('DEVOPS','DEPLOYMENT','INFRA','INFRASTRUCTURE') THEN 'DEVOPS'
    WHEN UPPER(category) IN ('HR','HUMAN_RESOURCES') THEN 'HR'
    WHEN UPPER(category) IN ('SUPPORT','HELPDESK','BUG FIX','BUGFIX','BUG_FIX') THEN 'SUPPORT'
    WHEN UPPER(category) IN ('RESEARCH','R&D') THEN 'RESEARCH'
    WHEN category IS NULL THEN NULL
    ELSE 'OTHER'
END
WHERE category IS NOT NULL;

-- Alter the column to VARCHAR(30) to accommodate the enum string values.
ALTER TABLE tasks MODIFY COLUMN category VARCHAR(30) NULL;
