package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single task activity timeline entry.
 *
 * @param id          UUID of the activity record
 * @param taskId      UUID of the task this activity belongs to
 * @param actorId     UUID of the employee who performed the action (may be null for system events)
 * @param actorName   Display name of the actor
 * @param eventType   Short event code, e.g. TASK_ASSIGNED, TASK_STARTED
 * @param description Human-readable description of the event
 * @param fromStatus  Previous task status (if a status change occurred)
 * @param toStatus    New task status (if a status change occurred)
 * @param createdAt   Timestamp of the event
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "A single entry in a task's activity timeline")
public record TaskActivityResponse(

        @Schema(description = "UUID of the activity record")
        UUID id,

        @Schema(description = "UUID of the parent task")
        UUID taskId,

        @Schema(description = "UUID of the actor employee, or null for system events")
        UUID actorId,

        @Schema(description = "Display name of the actor", example = "Jane Doe")
        String actorName,

        @Schema(description = "Short event code", example = "TASK_ASSIGNED")
        String eventType,

        @Schema(description = "Human-readable description of the event")
        String description,

        @Schema(description = "Previous task status", example = "ASSIGNED")
        String fromStatus,

        @Schema(description = "New task status", example = "IN_PROGRESS")
        String toStatus,

        @Schema(description = "Timestamp of the event")
        LocalDateTime createdAt
) {
}
