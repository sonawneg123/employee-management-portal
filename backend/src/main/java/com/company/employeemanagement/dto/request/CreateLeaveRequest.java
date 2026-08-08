package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for submitting a new leave request.
 *
 * @param employeeId UUID of the employee submitting the leave request
 * @param leaveType  category of leave
 * @param startDate  inclusive first day of the leave period
 * @param endDate    inclusive last day of the leave period; must not be before {@code startDate}
 * @param reason     optional justification provided by the employee
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to submit a new leave request")
public record CreateLeaveRequest(

        @Schema(description = "UUID of the employee submitting the request",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @Schema(description = "Category of leave", example = "ANNUAL")
        @NotNull(message = "Leave type is required")
        LeaveType leaveType,

        @Schema(description = "Inclusive first day of the leave period", example = "2025-08-01")
        @NotNull(message = "Start date is required")
        LocalDate startDate,

        @Schema(description = "Inclusive last day of the leave period", example = "2025-08-05")
        @NotNull(message = "End date is required")
        LocalDate endDate,

        @Schema(description = "Optional reason or justification", example = "Family vacation")
        @Size(max = 500, message = "Reason must not exceed 500 characters")
        String reason
) {
}
