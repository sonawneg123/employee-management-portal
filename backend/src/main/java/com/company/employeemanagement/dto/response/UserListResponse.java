package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a user account in the admin user-management list.
 *
 * @param id          UUID primary key of the user
 * @param email       unique email address / login credential
 * @param firstName   first name
 * @param lastName    last name
 * @param roles       list of role names assigned to this user (e.g. "ROLE_ADMIN")
 * @param isEnabled   whether the account can log in
 * @param isLocked    whether the account is temporarily locked
 * @param createdAt   account creation timestamp
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "User account record as returned by the admin user-management API")
public record UserListResponse(

        @Schema(description = "UUID of the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Email address (login credential)", example = "admin@company.com")
        String email,

        @Schema(description = "First name", example = "Admin")
        String firstName,

        @Schema(description = "Last name", example = "User")
        String lastName,

        @Schema(description = "Assigned role names", example = "[\"ROLE_ADMIN\"]")
        List<String> roles,

        @Schema(description = "Whether the account is enabled", example = "true")
        boolean isEnabled,

        @Schema(description = "Whether the account is locked", example = "false")
        boolean isLocked,

        @Schema(description = "Account creation timestamp")
        LocalDateTime createdAt
) {
}
