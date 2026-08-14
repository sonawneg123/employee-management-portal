package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for initiating the password-reset OTP flow.
 *
 * @param email the email address to send the OTP to
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Email address for which a password-reset OTP should be generated")
public record ForgotPasswordRequest(

        @Schema(description = "Registered email address", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email
) {
}
