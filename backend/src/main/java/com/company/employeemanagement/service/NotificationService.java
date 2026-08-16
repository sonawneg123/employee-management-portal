package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.NotificationResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UnreadCountResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.enums.NotificationType;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for in-app notification management.
 *
 * <p>All methods that return notifications are scoped to the authenticated user.
 *
 * @author Employee Management Portal Team
 */
public interface NotificationService {

    /**
     * Returns a paginated list of notifications for the authenticated user.
     *
     * @param pageable pagination and sorting parameters
     * @return a {@link PageResponse} of {@link NotificationResponse} records
     */
    PageResponse<NotificationResponse> findMyNotifications(Pageable pageable);

    /**
     * Returns the number of unread notifications for the authenticated user.
     *
     * @return unread count response
     */
    UnreadCountResponse getUnreadCount();

    /**
     * Marks a specific notification as read.
     * The authenticated user must be the recipient.
     *
     * @param notificationId UUID of the notification to mark as read
     */
    void markAsRead(UUID notificationId);

    /**
     * Marks all notifications for the authenticated user as read.
     */
    void markAllAsRead();

    /**
     * Creates a notification for the given recipient.
     * Called internally by the task service — not exposed via the API.
     *
     * @param recipient     the employee to notify
     * @param type          the notification type
     * @param title         the short title
     * @param message       the full message
     * @param relatedTaskId the UUID of the related task, or {@code null}
     */
    void createNotification(Employee recipient, NotificationType type,
                             String title, String message, UUID relatedTaskId);
}
