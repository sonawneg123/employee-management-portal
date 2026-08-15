package com.company.employeemanagement.ai.rag.dto;

import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request payload for ingesting a new document into the RAG knowledge base.
 *
 * @param title       human-readable title (required, max 500 chars)
 * @param description optional short description (max 1000 chars)
 * @param sourceType  classification of the document's origin (required)
 * @param sourceName  optional source filename or system name
 * @param content     full text content of the document (required)
 *
 * @author Employee Management Portal Team
 */
public record IngestDocumentRequest(

        @NotBlank(message = "Title must not be blank")
        @Size(max = 500, message = "Title must not exceed 500 characters")
        String title,

        @Size(max = 1000, message = "Description must not exceed 1000 characters")
        String description,

        @NotNull(message = "Source type must not be null")
        KnowledgeSourceType sourceType,

        @Size(max = 500, message = "Source name must not exceed 500 characters")
        String sourceName,

        @NotBlank(message = "Content must not be blank")
        String content
) {}
