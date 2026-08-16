package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new task.
 *
 * @param title              Short descriptive title (required)
 * @param description        Detailed description of the work
 * @param guidelines         Step-by-step instructions
 * @param acceptanceCriteria Criteria for task completion
 * @param assignedEmployeeId UUID of the employee to assign the task to
 * @param priority           Task priority (defaults to MEDIUM when null)
 * @param dueDate            Target completion date
 * @param estimatedHours     Estimated effort in hours
 * @param category           Task category enum value
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to create a new task")
public record CreateTaskRequest(

        @Schema(description = "Short task title", example = "Implement login page")
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Schema(description = "Detailed description of the work to be done")
        String description,

        @Schema(description = "Step-by-step guidelines for the assignee")
        String guidelines,

        @Schema(description = "Acceptance criteria that must be met")
        String acceptanceCriteria,

        @Schema(description = "UUID of the employee to assign this task to",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID assignedEmployeeId,

        @Schema(description = "Task priority", example = "MEDIUM")
        TaskPriority priority,

        @Schema(description = "Target completion date", example = "2025-09-30")
        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        @Schema(description = "Estimated effort in hours", example = "8.0")
        BigDecimal estimatedHours,

        @Schema(description = "Task category", example = "DEVELOPMENT")
        TaskCategory category
) {
}
