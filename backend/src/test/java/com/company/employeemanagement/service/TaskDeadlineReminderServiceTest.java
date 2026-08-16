package com.company.employeemanagement.service;

import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskDeadlineReminderService}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskDeadlineReminderService")
class TaskDeadlineReminderServiceTest {

    @Mock private TaskRepository      taskRepository;
    @Mock private NotificationService notificationService;

    private TaskDeadlineReminderService reminderService;

    @BeforeEach
    void setUp() {
        reminderService = new TaskDeadlineReminderService(taskRepository, notificationService);
    }

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

    private Task buildTask(final LocalDate dueDate,
                            final boolean reminder24hSent,
                            final boolean reminder2hSent,
                            final boolean overdueNotificationSent) {
        UUID empId = UUID.randomUUID();
        Employee emp = buildEmployee(empId);

        Task task = Task.builder()
                .title("Test Task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.IN_PROGRESS)
                .dueDate(dueDate)
                .assignedEmployee(emp)
                .reminder24hSent(reminder24hSent)
                .reminder2hSent(reminder2hSent)
                .overdueNotificationSent(overdueNotificationSent)
                .build();
        task.setId(UUID.randomUUID());
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    @Nested
    @DisplayName("sendDeadlineReminders()")
    class SendDeadlineReminders {

        @Test
        @DisplayName("sends 24h reminder when due date is tomorrow and flag not set")
        void sends24hReminder() {
            Task task = buildTask(LocalDate.now().plusDays(1), false, false, false);
            when(taskRepository.findNonCompletedTasksWithDueDate(any())).thenReturn(List.of(task));

            reminderService.sendDeadlineReminders();

            verify(notificationService).createNotification(
                    eq(task.getAssignedEmployee()),
                    eq(NotificationType.TASK_DUE_SOON),
                    argThat(t -> t.contains("Tomorrow")),
                    any(),
                    eq(task.getId())
            );
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("does not send 24h reminder if flag already set")
        void skips24hReminderIfAlreadySent() {
            Task task = buildTask(LocalDate.now().plusDays(1), true, false, false);
            when(taskRepository.findNonCompletedTasksWithDueDate(any())).thenReturn(List.of(task));

            reminderService.sendDeadlineReminders();

            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("sends overdue notification when due date has passed and flag not set")
        void sendsOverdueNotification() {
            Task task = buildTask(LocalDate.now().minusDays(1), false, false, false);
            when(taskRepository.findNonCompletedTasksWithDueDate(any())).thenReturn(List.of(task));

            reminderService.sendDeadlineReminders();

            verify(notificationService).createNotification(
                    eq(task.getAssignedEmployee()),
                    eq(NotificationType.TASK_OVERDUE),
                    argThat(t -> t.contains("Overdue")),
                    any(),
                    eq(task.getId())
            );
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("does not send overdue notification if flag already set (deduplication)")
        void deduplicatesOverdueNotification() {
            Task task = buildTask(LocalDate.now().minusDays(2), false, false, true);
            when(taskRepository.findNonCompletedTasksWithDueDate(any())).thenReturn(List.of(task));

            reminderService.sendDeadlineReminders();

            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("does not send any notification when no tasks match")
        void noTasksNoNotifications() {
            when(taskRepository.findNonCompletedTasksWithDueDate(any())).thenReturn(List.of());

            reminderService.sendDeadlineReminders();

            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }
    }
}
