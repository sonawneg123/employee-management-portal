package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic message response returned from password-reset endpoints.
 *
 * @param message a human-readable status message
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Generic message response")
public record MessageResponse(

        @Schema(description = "Status message", example = "If an account exists for this email, an OTP has been sent.")
        String message
) {
}
