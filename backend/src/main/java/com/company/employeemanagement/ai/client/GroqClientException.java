package com.company.employeemanagement.ai.client;

/**
 * Unchecked exception thrown by {@link GroqClient} when any communication
 * or API-level error occurs while calling the Groq API.
 *
 * <p>Carries an {@link ErrorType} discriminant so that the service layer can
 * map specific error categories to appropriate HTTP responses without
 * depending on the HTTP status code of the upstream call.
 *
 * @author Employee Management Portal Team
 */
public class GroqClientException extends RuntimeException {

    /**
     * Categorises the type of failure that occurred.
     */
    public enum ErrorType {
        /** The API key was rejected by Groq (HTTP 401). */
        AUTH_FAILURE,
        /** The request payload was rejected by Groq (HTTP 4xx). */
        INVALID_REQUEST,
        /** The Groq API returned a server-side error (HTTP 5xx). */
        API_FAILURE,
        /** The request timed out or the network connection was refused. */
        TIMEOUT
    }

    private final ErrorType errorType;

    /**
     * Constructs the exception with a user-safe message and an error type.
     *
     * @param message   a message safe to surface to the caller (no internal details)
     * @param errorType the category of the error
     */
    public GroqClientException(final String message, final ErrorType errorType) {
        super(message);
        this.errorType = errorType;
    }

    /**
     * Returns the error category.
     *
     * @return the {@link ErrorType}
     */
    public ErrorType getErrorType() {
        return errorType;
    }
}
