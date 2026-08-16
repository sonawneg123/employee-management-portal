package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for reassigning a task to a different employee.
 *
 * @param newEmployeeId UUID of the employee to reassign the task to
 * @param reason        Optional reason for the reassignment
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for reassigning a task to a different employee")
public record ReassignTaskRequest(

        @Schema(description = "UUID of the new assignee",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "New employee ID is required")
        UUID newEmployeeId,

        @Schema(description = "Optional reason for the reassignment",
                example = "Employee is on leave")
        String reason
) {
}
