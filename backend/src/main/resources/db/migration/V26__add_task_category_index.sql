-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V26 — Task Category Filter Index
--  Employee Management Portal — Phase 6C
-- ─────────────────────────────────────────────────────────────────────────────

-- Index to support efficient filtering by category in the task list query.
CREATE INDEX idx_task_category ON tasks (category);
