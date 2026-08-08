package com.company.employeemanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.UUID;

/**
 * Thrown when a requested resource cannot be found in the database.
 *
 * <p>Mapped to HTTP {@code 404 Not Found} by
 * {@link com.company.employeemanagement.exception.GlobalExceptionHandler}.
 *
 * @author Employee Management Portal Team
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class ResourceNotFoundException extends RuntimeException {

    /**
     * Constructs a new exception with a descriptive message.
     *
     * @param resourceName the simple class name of the missing resource
     * @param fieldName    the field used to look up the resource (e.g., {@code "id"})
     * @param fieldValue   the value that was searched for
     */
    public ResourceNotFoundException(final String resourceName,
                                     final String fieldName,
                                     final Object fieldValue) {
        super(String.format("%s not found with %s: '%s'", resourceName, fieldName, fieldValue));
    }

    /**
     * Convenience overload for UUID-keyed lookups.
     *
     * @param resourceName the simple class name of the missing resource
     * @param id           the UUID that was searched for
     */
    public ResourceNotFoundException(final String resourceName, final UUID id) {
        this(resourceName, "id", id);
    }
}
