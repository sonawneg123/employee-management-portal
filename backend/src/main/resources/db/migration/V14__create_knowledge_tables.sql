-- ──────────────────────────────────────────────────────────────────────────
-- V14: Create RAG knowledge-base tables
-- ──────────────────────────────────────────────────────────────────────────
-- knowledge_documents  — stores full document text, metadata, and lifecycle status
-- knowledge_chunks     — stores chunked passages derived from a parent document
-- ──────────────────────────────────────────────────────────────────────────

CREATE TABLE knowledge_documents (
    id          CHAR(36)     NOT NULL,
    title       VARCHAR(500) NOT NULL,
    description VARCHAR(1000),
    source_type VARCHAR(30)  NOT NULL,
    source_name VARCHAR(500),
    content     LONGTEXT     NOT NULL,
    status      VARCHAR(20)  NOT NULL DEFAULT 'PROCESSING',
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    created_by  VARCHAR(150),
    updated_by  VARCHAR(150),
    PRIMARY KEY (id),
    INDEX idx_kd_title       (title(255)),
    INDEX idx_kd_source_type (source_type),
    INDEX idx_kd_status      (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE knowledge_chunks (
    id          CHAR(36)     NOT NULL,
    document_id CHAR(36)     NOT NULL,
    chunk_index INT          NOT NULL,
    content     TEXT         NOT NULL,
    token_count INT,
    metadata    TEXT,
    created_at  DATETIME(6)  NOT NULL,
    updated_at  DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_kc_document_id  (document_id),
    INDEX idx_kc_chunk_index  (document_id, chunk_index),
    CONSTRAINT fk_kc_document
        FOREIGN KEY (document_id) REFERENCES knowledge_documents (id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
