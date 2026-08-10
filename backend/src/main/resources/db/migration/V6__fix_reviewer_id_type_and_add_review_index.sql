-- ─────────────────────────────────────────────────────────────────────────────
--  Flyway Migration V6
--  1. Revert reviewer_id from BINARY(16) back to CHAR(36) so that Hibernate's
--     UUID strategy (which stores UUIDs as CHAR(36) strings) works correctly.
--  2. Add index on performance_reviews(employee_id) for efficient per-employee
--     review lookups.
--  3. Add index on performance_reviews(review_date) for date-range queries.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── 1. Revert reviewer_id to CHAR(36) ────────────────────────────────────────
ALTER TABLE performance_reviews
    MODIFY COLUMN reviewer_id CHAR(36) NULL;

-- ── 2. Index on employee_id for fast per-employee queries ─────────────────────
CREATE INDEX idx_performance_reviews_employee_id
    ON performance_reviews (employee_id);

-- ── 3. Index on review_date for date-range queries ───────────────────────────
CREATE INDEX idx_performance_reviews_review_date
    ON performance_reviews (review_date);
