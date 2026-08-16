package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.response.NotificationResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UnreadCountResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Notification;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.NotificationRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link NotificationService}.
 *
 * <p>Notifications are always scoped to the authenticated user.
 * Internal creation methods are called by the task service — not exposed via the API.
 *
 * @author Employee Management Portal Team
 */
@Service
public class NotificationServiceImpl implements NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationServiceImpl.class);

    private final NotificationRepository notificationRepository;
    private final SecurityUtils securityUtils;

    /**
     * Constructs the service with required dependencies.
     *
     * @param notificationRepository repository for notification persistence
     * @param securityUtils          helper for resolving the authenticated employee
     */
    public NotificationServiceImpl(final NotificationRepository notificationRepository,
                                    final SecurityUtils securityUtils) {
        this.notificationRepository = notificationRepository;
        this.securityUtils = securityUtils;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> findMyNotifications(final Pageable pageable) {
        Employee recipient = requireCurrentEmployee();
        Page<Notification> page = notificationRepository.findByRecipientId(recipient.getId(), pageable);
        List<NotificationResponse> content = page.getContent().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        return PageResponse.from(new PageImpl<>(content, pageable, page.getTotalElements()));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public UnreadCountResponse getUnreadCount() {
        Employee recipient = requireCurrentEmployee();
        long count = notificationRepository.countByRecipientIdAndReadFalse(recipient.getId());
        return new UnreadCountResponse(count);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void markAsRead(final UUID notificationId) {
        Employee recipient = requireCurrentEmployee();
        // Verify the notification exists at all
        if (!notificationRepository.existsById(notificationId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        int updated = notificationRepository.markAsRead(notificationId, recipient.getId());
        if (updated == 0) {
            // Notification exists but belongs to a different employee
            throw new AccessDeniedException("You may only modify your own notifications.");
        }
        log.debug("Notification.markAsRead: id={} recipient={}", notificationId, recipient.getId());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void markAllAsRead() {
        Employee recipient = requireCurrentEmployee();
        int updated = notificationRepository.markAllAsRead(recipient.getId());
        log.debug("Notification.markAllAsRead: {} notifications for recipient={}", updated, recipient.getId());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void createNotification(final Employee recipient,
                                    final NotificationType type,
                                    final String title,
                                    final String message,
                                    final UUID relatedTaskId) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .type(type)
                .title(title)
                .message(message)
                .relatedTaskId(relatedTaskId)
                .read(false)
                .build();
        notificationRepository.save(notification);
        log.info("Notification.create: type={} recipient={} task={}", type, recipient.getId(), relatedTaskId);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Resolves the authenticated user's employee record, throwing if none found.
     */
    private Employee requireCurrentEmployee() {
        return securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));
    }

    /**
     * Converts a {@link Notification} entity to a {@link NotificationResponse} DTO.
     */
    private NotificationResponse toResponse(final Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getTitle(),
                n.getMessage(),
                n.getRelatedTaskId(),
                n.isRead(),
                n.getCreatedAt()
        );
    }
}
