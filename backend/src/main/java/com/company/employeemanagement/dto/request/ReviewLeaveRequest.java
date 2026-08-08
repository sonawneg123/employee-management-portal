package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for an HR/manager review decision (approve or reject) on a leave request.
 *
 * <p>This DTO is shared by both the approve and reject endpoints. For a rejection,
 * providing a {@code rejectionReason} is strongly recommended to inform the employee.
 *
 * @param rejectionReason optional reason when rejecting a leave request
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Optional payload for a leave review decision (approve or reject)")
public record ReviewLeaveRequest(

        @Schema(description = "Optional reason supplied when rejecting a leave request",
                example = "Insufficient leave balance")
        @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
        String rejectionReason
) {
}
