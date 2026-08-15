package com.company.employeemanagement.ai.rag.dto;

import java.util.UUID;

/**
 * A single result returned from a RAG knowledge-base search.
 *
 * @param documentId    UUID of the matched document
 * @param documentTitle title of the matched document
 * @param chunkId       UUID of the specific chunk that matched
 * @param chunkIndex    zero-based position of the chunk in its document
 * @param chunkContent  text content of the matching chunk
 * @param matchType     how the match was determined (e.g., "TITLE", "CONTENT")
 *
 * @author Employee Management Portal Team
 */
public record KnowledgeSearchResult(
        UUID documentId,
        String documentTitle,
        UUID chunkId,
        int chunkIndex,
        String chunkContent,
        String matchType
) {}
