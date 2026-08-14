package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for verifying the OTP entered by the user.
 *
 * @param email the email address the OTP was sent to
 * @param otp   the 6-digit OTP entered by the user
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Email and OTP for verifying the password-reset code")
public record VerifyOtpRequest(

        @Schema(description = "Email address the OTP was sent to", example = "john.doe@example.com")
        @NotBlank(message = "Email is required")
        @Email(message = "Email must be a valid email address")
        String email,

        @Schema(description = "6-digit OTP received via email", example = "483921")
        @NotBlank(message = "OTP is required")
        @Size(min = 6, max = 6, message = "OTP must be exactly 6 digits")
        String otp
) {
}
