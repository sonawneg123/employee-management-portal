package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.UUID;

/**
 * Response DTO returned after a successful authentication or registration.
 *
 * <p>The {@code accessToken} is a signed JWT that must be included in the
 * {@code Authorization: Bearer <token>} header of every subsequent request
 * to protected endpoints.
 *
 * @param accessToken  signed JWT access token
 * @param tokenType    always {@code "Bearer"}
 * @param expiresIn    access token validity period in seconds
 * @param userId       UUID of the authenticated user
 * @param email        email address of the authenticated user
 * @param firstName    first name of the authenticated user
 * @param lastName     last name of the authenticated user
 * @param roles        list of role names assigned to the authenticated user
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "JWT authentication response returned on successful login or registration")
public record AuthResponse(

        @Schema(description = "Signed JWT access token", example = "eyJhbGciOiJIUzI1NiJ9...")
        String accessToken,

        @Schema(description = "Token type; always Bearer", example = "Bearer")
        String tokenType,

        @Schema(description = "Access token validity period in seconds", example = "86400")
        long expiresIn,

        @Schema(description = "UUID of the authenticated user",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "Email address of the authenticated user",
                example = "john.doe@example.com")
        String email,

        @Schema(description = "First name of the authenticated user", example = "John")
        String firstName,

        @Schema(description = "Last name of the authenticated user", example = "Doe")
        String lastName,

        @Schema(description = "List of role names assigned to the user",
                example = "[\"ROLE_EMPLOYEE\"]")
        List<String> roles
) {
}
