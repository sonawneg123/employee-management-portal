package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for changing the authenticated user's password.
 *
 * @param currentPassword the user's current (existing) password — used for verification
 * @param newPassword     the desired new password (8–100 characters)
 * @param confirmPassword must exactly match {@code newPassword}
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for changing the authenticated user's password")
public record ChangePasswordRequest(

        @Schema(description = "Current password for verification", example = "OldP@ss1")
        @NotBlank(message = "Current password is required")
        String currentPassword,

        @Schema(description = "New password (8–100 characters)", example = "NewP@ss1")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "New password must be between 8 and 100 characters")
        String newPassword,

        @Schema(description = "Confirmation of the new password", example = "NewP@ss1")
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
}
