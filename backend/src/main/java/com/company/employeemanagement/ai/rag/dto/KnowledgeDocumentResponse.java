package com.company.employeemanagement.ai.rag.dto;

import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Read-only projection of a {@link com.company.employeemanagement.ai.rag.entity.KnowledgeDocument}.
 * Exposes no JPA entity internals.
 *
 * @param id          document UUID
 * @param title       document title
 * @param description optional description
 * @param sourceType  classification of the document's origin
 * @param sourceName  optional source filename or system name
 * @param status      current lifecycle status
 * @param createdAt   creation timestamp
 * @param updatedAt   last-updated timestamp
 * @param createdBy   auditor who created this document
 *
 * @author Employee Management Portal Team
 */
public record KnowledgeDocumentResponse(
        UUID id,
        String title,
        String description,
        KnowledgeSourceType sourceType,
        String sourceName,
        KnowledgeDocumentStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        String createdBy
) {}
