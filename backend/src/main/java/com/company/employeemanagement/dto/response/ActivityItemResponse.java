package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO representing a single entry in the recent-activity feed
 * returned by {@code GET /dashboard/activity}.
 *
 * @param id          Unique identifier for this activity entry (UUID string).
 * @param type        Activity type key corresponding to {@code ACTIVITY_TYPE_META}
 *                    on the frontend (e.g. {@code "EMPLOYEE_JOINED"},
 *                    {@code "LEAVE_APPROVED"}).
 * @param description Human-readable summary of the event.
 * @param timestamp   ISO-8601 date-time when the event occurred.
 * @param actorName   Full name of the user who triggered the event, or
 *                    {@code null} for system events.
 * @param targetName  Name of the entity that was affected, or {@code null}.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "A single entry in the recent activity feed")
public record ActivityItemResponse(

        @Schema(description = "Unique activity entry identifier",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        String id,

        @Schema(description = "Activity type key matching ACTIVITY_TYPE_META on the frontend",
                example = "EMPLOYEE_JOINED")
        String type,

        @Schema(description = "Human-readable description of the event",
                example = "Alice Smith joined the Engineering department")
        String description,

        @Schema(description = "ISO-8601 timestamp of the event",
                example = "2024-01-15T10:00:00")
        String timestamp,

        @Schema(description = "Name of the user who triggered the event",
                example = "Admin User",
                nullable = true)
        String actorName,

        @Schema(description = "Name of the entity that was affected",
                example = "Alice Smith",
                nullable = true)
        String targetName
) {
}
