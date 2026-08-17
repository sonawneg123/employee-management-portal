-- V29: Create task_ai_reviews table for Phase 7A AI Task Review feature.
--
-- Stores the result of an AI-powered analysis of a task submission.
-- Multiple analyses are allowed per submission (different requests/retries).
-- A unique constraint prevents simultaneous PENDING/PROCESSING requests for
-- the same submission, preventing duplicate in-flight requests.
--
-- Design notes:
--   - Does NOT modify task_submissions — employee work is never altered.
--   - structured_analysis_json stores the full structured JSON from the AI.
--   - error_message is populated only when status = 'FAILED'.
--   - prompt_version identifies which prompt template was used (for auditability).

CREATE TABLE task_ai_reviews (
    id                       CHAR(36)        NOT NULL,
    task_id                  CHAR(36)        NOT NULL,
    submission_id            CHAR(36)        NOT NULL,
    requested_by_id          CHAR(36)        NOT NULL,
    status                   ENUM('PENDING','PROCESSING','COMPLETED','FAILED')
                                             NOT NULL DEFAULT 'PENDING',
    ai_provider              VARCHAR(50)     NOT NULL DEFAULT 'groq',
    ai_model                 VARCHAR(100)    NULL,
    prompt_version           VARCHAR(50)     NOT NULL DEFAULT 'v1',
    completion_score         INT             NULL COMMENT '0-100',
    quality_score            INT             NULL COMMENT '0-100',
    confidence               INT             NULL COMMENT '0-100',
    recommended_action       ENUM('APPROVE','REQUEST_CHANGES','MANUAL_REVIEW') NULL,
    structured_analysis_json LONGTEXT        NULL COMMENT 'Full structured JSON from AI',
    manager_summary          TEXT            NULL,
    error_message            TEXT            NULL,
    created_at               DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at               DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    created_by               VARCHAR(150)    NULL,
    updated_by               VARCHAR(150)    NULL,
    completed_at             DATETIME(6)     NULL,

    PRIMARY KEY (id),
    CONSTRAINT fk_ai_review_task
        FOREIGN KEY (task_id)       REFERENCES tasks(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_review_submission
        FOREIGN KEY (submission_id) REFERENCES task_submissions(id) ON DELETE CASCADE,
    CONSTRAINT fk_ai_review_requester
        FOREIGN KEY (requested_by_id) REFERENCES employees(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Index to quickly find all reviews for a submission
CREATE INDEX idx_ai_review_submission_id
    ON task_ai_reviews (submission_id);

-- Index to quickly find all reviews for a task
CREATE INDEX idx_ai_review_task_id
    ON task_ai_reviews (task_id);

-- Partial unique index: at most one PENDING or PROCESSING review per submission
-- Prevents duplicate simultaneous requests for the same submission.
-- MySQL does not support partial indexes natively; we use a dedicated check via
-- application logic + a composite index to detect conflicts efficiently.
CREATE INDEX idx_ai_review_submission_status
    ON task_ai_reviews (submission_id, status);
