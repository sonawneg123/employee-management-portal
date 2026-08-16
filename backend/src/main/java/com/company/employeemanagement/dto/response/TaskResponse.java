package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a task returned to API consumers.
 *
 * @param id                       UUID primary key of the task
 * @param title                    Short task title
 * @param description              Detailed work description
 * @param guidelines               Step-by-step instructions
 * @param acceptanceCriteria       Completion criteria
 * @param assignedEmployeeId       UUID of the assigned employee, or {@code null}
 * @param assignedEmployeeName     Display name of the assignee
 * @param assignedEmployeeCode     Employee code of the assignee
 * @param createdByEmployeeId      UUID of the creating employee
 * @param createdByEmployeeName    Display name of the creator
 * @param priority                 Task priority level
 * @param status                   Current task status
 * @param overdue                  Derived flag: true when status != COMPLETED and dueDate < today
 * @param dueDate                  Target completion date
 * @param estimatedHours           Estimated effort in hours
 * @param category                 Structured task category enum
 * @param createdAt                Record creation timestamp
 * @param updatedAt                Record last-modified timestamp
 * @param createdBy                Email of the principal who created the record
 * @param updatedBy                Email of the principal who last modified the record
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Task record as returned by the API")
public record TaskResponse(

        @Schema(description = "UUID of the task",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Short task title", example = "Implement login page")
        String title,

        @Schema(description = "Detailed description of the work")
        String description,

        @Schema(description = "Step-by-step guidelines for the assignee")
        String guidelines,

        @Schema(description = "Criteria that must be met for task completion")
        String acceptanceCriteria,

        @Schema(description = "UUID of the assigned employee")
        UUID assignedEmployeeId,

        @Schema(description = "Full name of the assigned employee", example = "Jane Doe")
        String assignedEmployeeName,

        @Schema(description = "Employee code of the assignee", example = "EMP-0042")
        String assignedEmployeeCode,

        @Schema(description = "UUID of the employee who created this task")
        UUID createdByEmployeeId,

        @Schema(description = "Full name of the task creator", example = "John Manager")
        String createdByEmployeeName,

        @Schema(description = "Task priority", example = "MEDIUM")
        TaskPriority priority,

        @Schema(description = "Current task status", example = "ASSIGNED")
        TaskStatus status,

        @Schema(description = "True when status != COMPLETED and dueDate is in the past",
                example = "false")
        boolean overdue,

        @Schema(description = "Target completion date", example = "2025-09-30")
        LocalDate dueDate,

        @Schema(description = "Estimated effort in hours", example = "8.0")
        BigDecimal estimatedHours,

        @Schema(description = "Structured task category", example = "DEVELOPMENT")
        TaskCategory category,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Email of the principal who created the record",
                example = "manager@example.com")
        String createdBy,

        @Schema(description = "Email of the principal who last modified the record",
                example = "manager@example.com")
        String updatedBy
) {
}
