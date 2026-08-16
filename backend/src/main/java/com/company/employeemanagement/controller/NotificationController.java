package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.response.MessageResponse;
import com.company.employeemanagement.dto.response.NotificationResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UnreadCountResponse;
import com.company.employeemanagement.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing notification endpoints.
 *
 * <p>Base path: {@code /api/notifications}
 *
 * <p>All endpoints are scoped to the authenticated user — no user can access
 * another user's notifications.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "In-app notification management")
@SecurityRequirement(name = "BearerAuth")
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param notificationService the notification service
     */
    public NotificationController(final NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    /**
     * Returns a paginated list of notifications for the authenticated user.
     *
     * @param page      zero-based page number (default: 0)
     * @param size      page size (default: 20)
     * @return page of notifications
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List my notifications",
               description = "Returns notifications for the authenticated user, newest first.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notifications returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<NotificationResponse>> findMyNotifications(
            @Parameter(description = "Zero-based page number") @RequestParam(defaultValue = "0") final int page,
            @Parameter(description = "Page size") @RequestParam(defaultValue = "20") final int size
    ) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100),
                Sort.by("createdAt").descending());
        return ResponseEntity.ok(notificationService.findMyNotifications(pageable));
    }

    /**
     * Returns the unread notification count for the authenticated user.
     *
     * @return unread count
     */
    @GetMapping(value = "/unread-count", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Unread notification count",
               description = "Returns the number of unread notifications for the authenticated user.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Count returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UnreadCountResponse> getUnreadCount() {
        return ResponseEntity.ok(notificationService.getUnreadCount());
    }

    /**
     * Marks a single notification as read.
     *
     * @param id the UUID of the notification to mark as read
     * @return 200 OK with a message
     */
    @PatchMapping(value = "/{id}/read", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Mark notification as read",
               description = "Marks a specific notification as read. The authenticated user must be the recipient.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Marked as read"),
            @ApiResponse(responseCode = "403", description = "Not the recipient",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Notification not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MessageResponse> markAsRead(
            @Parameter(description = "UUID of the notification")
            @PathVariable final UUID id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(new MessageResponse("Notification marked as read."));
    }

    /**
     * Marks all notifications for the authenticated user as read.
     *
     * @return 200 OK with a message
     */
    @PatchMapping(value = "/read-all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Mark all notifications as read",
               description = "Marks all notifications for the authenticated user as read.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "All marked as read"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MessageResponse> markAllAsRead() {
        notificationService.markAllAsRead();
        return ResponseEntity.ok(new MessageResponse("All notifications marked as read."));
    }
}
