package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO carrying the credentials required to authenticate a user
 * and obtain a JWT access token.
 *
 * @param email    registered email address of the user
 * @param password plaintext password submitted for verification
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Credentials required to authenticate and obtain a JWT token")
public record LoginRequest(

        @Schema(description = "Registered email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "Account password", example = "SecureP@ss1")
        @NotBlank(message = "Password is required")
        String password
) {
}
