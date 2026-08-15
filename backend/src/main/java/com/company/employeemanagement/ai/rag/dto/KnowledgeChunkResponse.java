package com.company.employeemanagement.ai.rag.dto;

import java.util.UUID;

/**
 * Read-only projection of a {@link com.company.employeemanagement.ai.rag.entity.KnowledgeChunk}.
 *
 * @param id          chunk UUID
 * @param documentId  UUID of the parent document
 * @param chunkIndex  zero-based index within the parent document
 * @param content     chunk text
 * @param tokenCount  estimated token count
 * @param metadata    optional JSON metadata string
 *
 * @author Employee Management Portal Team
 */
public record KnowledgeChunkResponse(
        UUID id,
        UUID documentId,
        int chunkIndex,
        String content,
        Integer tokenCount,
        String metadata
) {}
