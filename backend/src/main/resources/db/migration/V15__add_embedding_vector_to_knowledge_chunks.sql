-- ──────────────────────────────────────────────────────────────────────────
-- V15: Add embedding_vector column to knowledge_chunks (Phase 3 — Vector RAG)
-- ──────────────────────────────────────────────────────────────────────────
-- Adds a LONGBLOB column to store serialised IEEE 754 float vectors.
-- Nullable: existing chunks without embeddings remain accessible and can be
-- re-indexed without data loss.
-- ──────────────────────────────────────────────────────────────────────────

ALTER TABLE knowledge_chunks
    ADD COLUMN embedding_vector LONGBLOB NULL
        COMMENT 'Serialised dense embedding vector (big-endian IEEE 754 floats). NULL = not yet embedded.';
