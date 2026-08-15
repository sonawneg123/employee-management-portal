package com.company.employeemanagement.ai.rag.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * General-purpose exception for RAG knowledge-base operations.
 *
 * <p>Thrown by RAG services when a business-level error occurs (e.g., a document is
 * not found, ingestion validation fails, or the feature is disabled). Maps to
 * HTTP 400 Bad Request by default; callers that need a different status should
 * catch and re-throw or use a specific sub-class.
 *
 * @author Employee Management Portal Team
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class RagException extends RuntimeException {

    /**
     * Constructs a {@code RagException} with the given detail message.
     *
     * @param message human-readable description of the error
     */
    public RagException(final String message) {
        super(message);
    }

    /**
     * Constructs a {@code RagException} with a message and a root cause.
     *
     * @param message human-readable description of the error
     * @param cause   the underlying exception
     */
    public RagException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
