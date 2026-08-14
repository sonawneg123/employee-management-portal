package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for resetting the user's password after OTP verification.
 *
 * @param email           the email address (identifies which verified token to use)
 * @param newPassword     the new plaintext password
 * @param confirmPassword confirmation of the new password
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "New password payload for the password-reset flow")
public record ResetPasswordRequest(

        @Schema(description = "Email address the OTP was verified for", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "New plaintext password — minimum 8 characters", example = "NewP@ss1!")
        @NotBlank(message = "New password is required")
        @Size(min = 8, max = 100, message = "Password must be between 8 and 100 characters")
        String newPassword,

        @Schema(description = "Confirmation of the new password", example = "NewP@ss1!")
        @NotBlank(message = "Password confirmation is required")
        String confirmPassword
) {
}
