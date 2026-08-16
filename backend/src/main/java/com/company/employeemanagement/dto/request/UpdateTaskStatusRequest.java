package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for an employee to update the status of their assigned task.
 *
 * <p>Employees are restricted to a limited set of status transitions
 * enforced at the service layer.
 *
 * @param status The new status to transition to
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for an employee to update their task status")
public record UpdateTaskStatusRequest(

        @Schema(description = "New task status", example = "IN_PROGRESS")
        @NotNull(message = "Status is required")
        TaskStatus status
) {
}
