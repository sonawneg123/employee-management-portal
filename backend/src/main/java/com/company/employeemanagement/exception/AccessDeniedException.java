package com.company.employeemanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when an authenticated principal attempts to perform an operation
 * they are not authorised to carry out.
 *
 * <p>Distinct from Spring Security's own {@code AccessDeniedException} —
 * this is thrown from business-logic checks (e.g., an employee attempting
 * to modify another employee's record) and is mapped to HTTP
 * {@code 403 Forbidden} by
 * {@link com.company.employeemanagement.exception.GlobalExceptionHandler}.
 *
 * @author Employee Management Portal Team
 */
@ResponseStatus(HttpStatus.FORBIDDEN)
public class AccessDeniedException extends RuntimeException {

    /**
     * Constructs a new exception with the supplied message.
     *
     * @param message human-readable description of the access violation
     */
    public AccessDeniedException(final String message) {
        super(message);
    }
}
