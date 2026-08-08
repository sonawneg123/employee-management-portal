package com.company.employeemanagement.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Thrown when a business rule would be violated by the requested operation.
 *
 * <p>Examples:
 * <ul>
 *   <li>Registering with an email address that already exists.</li>
 *   <li>Creating an employee with a duplicate employee code.</li>
 *   <li>Approving a leave request that has already been decided.</li>
 * </ul>
 *
 * <p>Mapped to HTTP {@code 409 Conflict} by
 * {@link com.company.employeemanagement.exception.GlobalExceptionHandler}.
 *
 * @author Employee Management Portal Team
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicateResourceException extends RuntimeException {

    /**
     * Constructs a new exception describing the duplicate resource.
     *
     * @param resourceName the simple class name of the duplicated resource
     * @param fieldName    the field containing the duplicate value
     * @param fieldValue   the duplicate value
     */
    public DuplicateResourceException(final String resourceName,
                                      final String fieldName,
                                      final Object fieldValue) {
        super(String.format("%s already exists with %s: '%s'", resourceName, fieldName, fieldValue));
    }
}
