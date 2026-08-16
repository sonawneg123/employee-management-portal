package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Lightweight response containing only the unread notification count.
 *
 * @param unreadCount number of unread notifications for the authenticated user
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Unread notification count response")
public record UnreadCountResponse(

        @Schema(description = "Number of unread notifications", example = "3")
        long unreadCount
) {
}
