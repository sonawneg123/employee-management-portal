-- V31: Fix ai_agent_audit_logs UUID column definitions
--
-- Root cause (Phase 7E startup failure):
--   AiAgentAuditLog.id and AiAgentAuditLog.userId were declared as Java UUID
--   without @JdbcTypeCode(SqlTypes.CHAR). In Hibernate 6 / Spring Boot 3.x with
--   MySQLDialect, a bare UUID field without @JdbcTypeCode defaults to BINARY(16),
--   causing a schema-validation mismatch against the CHAR(36) columns created by V30.
--
-- Fix applied to entity (Phase 7E corrective):
--   @JdbcTypeCode(SqlTypes.CHAR) added to both `id` and `userId` fields in
--   AiAgentAuditLog, consistent with all other entities in this project
--   (BaseEntity, KnowledgeChunk, KnowledgeDocument, etc.).
--
-- This migration ensures the database columns explicitly match the CHAR(36)
-- convention used throughout this project. If V30 already created them as
-- CHAR(36), these statements are idempotent and safe (no data is changed).
-- If for any reason the columns were created differently, they are corrected here.
--
-- Existing data safety:
--   The id column stores UUID strings (e.g. '550e8400-e29b-41d4-a716-446655440000')
--   as CHAR(36). Converting CHAR(36) -> CHAR(36) is a no-op on existing data.
--   No binary conversion is needed because the project convention is CHAR(36).

ALTER TABLE ai_agent_audit_logs
    MODIFY COLUMN id      CHAR(36) NOT NULL COMMENT 'Primary key — UUID stored as CHAR(36)',
    MODIFY COLUMN user_id CHAR(36) NOT NULL COMMENT 'Authenticated user UUID — stored as CHAR(36)';
