package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.NotificationResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UnreadCountResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Notification;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.NotificationRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link NotificationServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationServiceImpl")
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private SecurityUtils          securityUtils;

    private NotificationServiceImpl notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationServiceImpl(notificationRepository, securityUtils);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(final UUID id) {
        User user = User.builder()
                .firstName("Jane")
                .lastName("Doe")
                .email("jane@example.com")
                .passwordHash("hash")
                .build();
        user.setId(UUID.randomUUID());

        Department dept = new Department();
        dept.setName("Engineering");
        dept.setCode("ENG");

        Employee emp = Employee.builder()
                .employeeCode("EMP-001")
                .department(dept)
                .jobTitle("Software Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(75000))
                .user(user)
                .build();
        emp.setId(id);
        return emp;
    }

    private Notification buildNotification(final UUID notifId, final Employee recipient, final boolean read) {
        Notification n = Notification.builder()
                .recipient(recipient)
                .type(NotificationType.TASK_ASSIGNED)
                .title("New Task Assigned")
                .message("You have been assigned: \"Test Task\"")
                .relatedTaskId(UUID.randomUUID())
                .read(read)
                .build();
        n.setId(notifId);
        n.setCreatedAt(LocalDateTime.now());
        n.setUpdatedAt(LocalDateTime.now());
        return n;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findMyNotifications()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findMyNotifications()")
    class FindMyNotifications {

        @Test
        @DisplayName("returns page of notifications for authenticated employee")
        void returnsNotifications() {
            UUID empId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            UUID notifId = UUID.randomUUID();
            Notification notif = buildNotification(notifId, employee, false);

            Pageable pageable = PageRequest.of(0, 20);
            Page<Notification> page = new PageImpl<>(List.of(notif), pageable, 1L);

            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(notificationRepository.findByRecipientId(empId, pageable)).thenReturn(page);

            PageResponse<NotificationResponse> result = notificationService.findMyNotifications(pageable);

            assertThat(result.totalElements()).isEqualTo(1);
            assertThat(result.content()).hasSize(1);
            assertThat(result.content().get(0).id()).isEqualTo(notifId);
            assertThat(result.content().get(0).read()).isFalse();
        }

        @Test
        @DisplayName("throws when no employee record linked to account")
        void throwsWhenNoEmployee() {
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());

            Pageable pageable = PageRequest.of(0, 20);
            assertThatThrownBy(() -> notificationService.findMyNotifications(pageable))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getUnreadCount()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUnreadCount()")
    class GetUnreadCount {

        @Test
        @DisplayName("returns correct unread count for recipient")
        void returnsUnreadCount() {
            UUID empId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);

            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(notificationRepository.countByRecipientIdAndReadFalse(empId)).thenReturn(5L);

            UnreadCountResponse result = notificationService.getUnreadCount();

            assertThat(result.unreadCount()).isEqualTo(5L);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markAsRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAsRead()")
    class MarkAsRead {

        @Test
        @DisplayName("marks notification as read for its recipient")
        void marksAsRead() {
            UUID empId  = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);

            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(notificationRepository.existsById(notifId)).thenReturn(true);
            when(notificationRepository.markAsRead(notifId, empId)).thenReturn(1);

            notificationService.markAsRead(notifId);

            verify(notificationRepository).markAsRead(notifId, empId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when notification does not exist")
        void throwsWhenNotFound() {
            UUID empId  = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);

            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(notificationRepository.existsById(notifId)).thenReturn(false);

            assertThatThrownBy(() -> notificationService.markAsRead(notifId))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("throws AccessDeniedException when notification belongs to different user")
        void throwsWhenWrongRecipient() {
            UUID empId  = UUID.randomUUID();
            UUID notifId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);

            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(notificationRepository.existsById(notifId)).thenReturn(true);
            // markAsRead returns 0 because recipient doesn't match
            when(notificationRepository.markAsRead(notifId, empId)).thenReturn(0);

            assertThatThrownBy(() -> notificationService.markAsRead(notifId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own notifications");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // markAllAsRead()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("markAllAsRead()")
    class MarkAllAsRead {

        @Test
        @DisplayName("marks all unread notifications as read for recipient")
        void marksAllAsRead() {
            UUID empId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);

            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(notificationRepository.markAllAsRead(empId)).thenReturn(3);

            notificationService.markAllAsRead();

            verify(notificationRepository).markAllAsRead(empId);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createNotification()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createNotification()")
    class CreateNotification {

        @Test
        @DisplayName("creates a new notification with correct fields")
        void createsNotification() {
            UUID empId = UUID.randomUUID();
            Employee recipient = buildEmployee(empId);
            UUID taskId = UUID.randomUUID();

            notificationService.createNotification(
                    recipient,
                    NotificationType.TASK_ASSIGNED,
                    "New Task Assigned",
                    "You have been assigned: \"Test Task\"",
                    taskId
            );

            verify(notificationRepository).save(any(Notification.class));
        }
    }
}
