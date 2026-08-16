package com.company.employeemanagement.ai.rag.embedding;

/**
 * Thrown when an embedding provider call fails.
 *
 * <p>Wraps provider-specific errors (HTTP failures, auth errors, malformed
 * responses) so that callers can handle embedding failures uniformly without
 * depending on HTTP or provider-specific exception types.
 *
 * @author Employee Management Portal Team
 */
public class EmbeddingException extends RuntimeException {

    /**
     * Constructs an {@code EmbeddingException} with the given detail message.
     *
     * @param message human-readable description of the error
     */
    public EmbeddingException(final String message) {
        super(message);
    }

    /**
     * Constructs an {@code EmbeddingException} with a message and a root cause.
     *
     * @param message human-readable description of the error
     * @param cause   the underlying exception
     */
    public EmbeddingException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
