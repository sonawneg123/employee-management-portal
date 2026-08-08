package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for updating a pending leave request (full replacement / PUT semantics).
 *
 * <p>Only {@code PENDING} leave requests may be updated. Once reviewed the record
 * is immutable from the employee's perspective.
 *
 * @param leaveType updated category of leave
 * @param startDate updated inclusive first day of the leave period
 * @param endDate   updated inclusive last day of the leave period
 * @param reason    updated optional justification
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to update a pending leave request")
public record UpdateLeaveRequest(

        @Schema(description = "Updated category of leave", example = "SICK")
        @NotNull(message = "Leave type is required")
        LeaveType leaveType,

        @Schema(description = "Updated inclusive first day of the leave period", example = "2025-08-03")
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @Schema(description = "Updated inclusive last day of the leave period", example = "2025-08-04")
        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Schema(description = "Updated reason or justification", example = "Medical appointment")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
