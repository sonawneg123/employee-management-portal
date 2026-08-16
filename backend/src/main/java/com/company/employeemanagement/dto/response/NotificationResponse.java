package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.NotificationType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a single in-app notification.
 *
 * @param id            UUID of the notification
 * @param type          notification type enum value
 * @param title         short title
 * @param message       full description text
 * @param relatedTaskId UUID of the related task, or {@code null}
 * @param read          whether the recipient has read it
 * @param createdAt     timestamp when the notification was created
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "In-app notification record")
public record NotificationResponse(

        @Schema(description = "UUID of the notification")
        UUID id,

        @Schema(description = "Notification type", example = "TASK_ASSIGNED")
        NotificationType type,

        @Schema(description = "Short notification title", example = "New Task Assigned")
        String title,

        @Schema(description = "Full notification message")
        String message,

        @Schema(description = "UUID of the related task, if any")
        UUID relatedTaskId,

        @Schema(description = "Whether the notification has been read")
        boolean read,

        @Schema(description = "When the notification was created")
        LocalDateTime createdAt
) {
}
