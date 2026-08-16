package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating an existing task (manager use).
 *
 * @param title              Updated title (required)
 * @param description        Updated description
 * @param guidelines         Updated guidelines
 * @param acceptanceCriteria Updated acceptance criteria
 * @param assignedEmployeeId New assignee UUID (or null to unassign)
 * @param priority           Updated priority
 * @param status             Updated status
 * @param dueDate            Updated due date
 * @param estimatedHours     Updated estimate
 * @param category           Updated category enum value
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for updating a task")
public record UpdateTaskRequest(

        @Schema(description = "Short task title", example = "Implement login page")
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Schema(description = "Detailed description")
        String description,

        @Schema(description = "Step-by-step guidelines")
        String guidelines,

        @Schema(description = "Acceptance criteria")
        String acceptanceCriteria,

        @Schema(description = "UUID of the employee to assign")
        UUID assignedEmployeeId,

        @Schema(description = "Task priority", example = "HIGH")
        TaskPriority priority,

        @Schema(description = "Task status", example = "IN_PROGRESS")
        TaskStatus status,

        @Schema(description = "Target completion date", example = "2025-09-30")
        @NotNull(message = "Due date is required")
        LocalDate dueDate,

        @Schema(description = "Estimated hours", example = "8.0")
        BigDecimal estimatedHours,

        @Schema(description = "Task category", example = "DEVELOPMENT")
        TaskCategory category
) {
}
