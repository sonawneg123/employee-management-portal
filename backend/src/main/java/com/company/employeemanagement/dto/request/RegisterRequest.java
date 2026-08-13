package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO carrying the credentials needed to register a new user account.
 *
 * <p>All fields are validated before reaching the service layer. Any constraint
 * violation causes Spring to return a {@code 400 Bad Request} with a
 * {@code ProblemDetail} body listing each failing field.
 *
 * <p>The optional {@code role} field may be {@code "ROLE_HR"} or
 * {@code "ROLE_EMPLOYEE"} only. If omitted or {@code null}, the service
 * defaults to {@code ROLE_EMPLOYEE}.  {@code ROLE_ADMIN} and
 * {@code ROLE_MANAGER} are never assignable via public registration.
 *
 * @param email     unique email address that will serve as the login username
 * @param password  plaintext password (minimum 8 characters); hashed before persistence
 * @param firstName user's first name
 * @param lastName  user's last name
 * @param role      optional role override — {@code "ROLE_HR"} or {@code "ROLE_EMPLOYEE"};
 *                  defaults to {@code "ROLE_EMPLOYEE"} when {@code null}
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to create a new user account")
public record RegisterRequest(

        @Schema(description = "Unique email address used as the login principal",
                example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        @Size(max = 150, message = "Email must not exceed 150 characters")
        String email,

        @Schema(description = "Plaintext password — minimum 8 characters",
                example = "SecureP@ss1")
        @NotBlank(message = "Password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String password,

        @Schema(description = "User's first name", example = "John")
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "User's last name", example = "Doe")
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Optional role for the new account — ROLE_HR or ROLE_EMPLOYEE only",
                example = "ROLE_EMPLOYEE",
                allowableValues = {"ROLE_HR", "ROLE_EMPLOYEE"})
        String role
) {
    /**
     * Backward-compatible constructor that omits {@code role}.
     * Equivalent to calling the canonical constructor with {@code role = null},
     * which causes {@link com.company.employeemanagement.service.impl.AuthServiceImpl}
     * to default to {@code ROLE_EMPLOYEE}.
     *
     * @param email     login email
     * @param password  plaintext password
     * @param firstName first name
     * @param lastName  last name
     */
    public RegisterRequest(final String email,
                           final String password,
                           final String firstName,
                           final String lastName) {
        this(email, password, firstName, lastName, null);
    }
}
