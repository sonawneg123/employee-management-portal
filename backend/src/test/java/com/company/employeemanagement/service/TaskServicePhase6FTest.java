package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskRequest;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.mapper.TaskMapper;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import com.company.employeemanagement.repository.TaskActivityRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.TaskServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 6F unit tests for {@link TaskServiceImpl} covering same-employee
 * reassignment rejection and {@link NotificationType#TASK_UPDATED} notifications.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskServiceImpl — Phase 6F")
class TaskServicePhase6FTest {

    @Mock private TaskRepository         taskRepository;
    @Mock private EmployeeRepository     employeeRepository;
    @Mock private AttendanceRepository   attendanceRepository;
    @Mock private LeaveRequestRepository leaveRequestRepository;
    @Mock private TaskActivityRepository taskActivityRepository;
    @Mock private TaskMapper             taskMapper;
    @Mock private SecurityUtils          securityUtils;
    @Mock private NotificationService    notificationService;

    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(
                taskRepository, employeeRepository, attendanceRepository,
                leaveRequestRepository, taskActivityRepository,
                taskMapper, securityUtils, notificationService);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private Employee buildEmployee(final UUID id) {
        User user = User.builder()
                .firstName("Test")
                .lastName("User")
                .email("test@example.com")
                .passwordHash("hash")
                .build();
        user.setId(UUID.randomUUID());

        Department dept = new Department();
        dept.setName("Engineering");
        dept.setCode("ENG");

        Employee emp = Employee.builder()
                .employeeCode("EMP-001")
                .department(dept)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(75000))
                .user(user)
                .build();
        emp.setId(id);
        return emp;
    }

    private Task buildTask(final UUID taskId, final Employee assignedEmployee) {
        Task task = Task.builder()
                .title("API Authentication Implementation")
                .description("Implement auth flow")
                .priority(TaskPriority.HIGH)
                .status(TaskStatus.ASSIGNED)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedEmployee(assignedEmployee)
                .category(TaskCategory.DEVELOPMENT)
                .build();
        task.setId(taskId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private TaskResponse buildTaskResponse(final UUID taskId, final UUID empId) {
        return new TaskResponse(
                taskId, "API Authentication Implementation", "desc", null, null,
                empId, "Test User", "EMP-001",
                UUID.randomUUID(), "Manager",
                TaskPriority.HIGH, TaskStatus.ASSIGNED, false,
                LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT,
                LocalDateTime.now(), LocalDateTime.now(), "mgr@test.com", "mgr@test.com"
        );
    }

    private Attendance checkedInAttendance(final Employee emp) {
        Attendance a = Attendance.builder()
                .employee(emp)
                .attendanceDate(LocalDate.now())
                .checkInTime(java.time.LocalTime.of(9, 0))
                .status(AttendanceStatus.PRESENT)
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }

    // ── Reassignment Tests ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("reassign()")
    class Reassign {

        @Test
        @DisplayName("throws IllegalStateException when reassigning to same employee currently assigned")
        void reassign_sameEmployee_throwsIllegalState() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(employee));

            ReassignTaskRequest request = new ReassignTaskRequest(empId, "Same person");

            assertThatThrownBy(() -> taskService.reassign(taskId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("same employee");
        }

        @Test
        @DisplayName("completes successfully when reassigning to a different checked-in employee")
        void reassign_differentEmployee_succeeds() {
            UUID taskId    = UUID.randomUUID();
            UUID oldEmpId  = UUID.randomUUID();
            UUID newEmpId  = UUID.randomUUID();
            Employee oldEmployee = buildEmployee(oldEmpId);
            Employee newEmployee = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmployee);
            TaskResponse response = buildTaskResponse(taskId, newEmpId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmployee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(newEmpId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInAttendance(newEmployee)));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.reassign(taskId, new ReassignTaskRequest(newEmpId, null));

            assertThat(result).isNotNull();
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("no notification sent when same-employee reassignment is rejected")
        void reassign_sameEmployee_noNotificationSent() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> taskService.reassign(taskId, new ReassignTaskRequest(empId, null)))
                    .isInstanceOf(IllegalStateException.class);

            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }
    }

    // ── Task Updated Notification Tests ────────────────────────────────────────

    @Nested
    @DisplayName("update() — TASK_UPDATED notification")
    class UpdateNotification {

        private UpdateTaskRequest buildRequest(final String title, final TaskPriority priority,
                                               final LocalDate dueDate, final UUID assigneeId) {
            return new UpdateTaskRequest(
                    title, "desc", null, null,
                    assigneeId, priority, TaskStatus.ASSIGNED,
                    dueDate, null, TaskCategory.DEVELOPMENT
            );
        }

        @Test
        @DisplayName("sends TASK_UPDATED notification when priority changes and assignee unchanged")
        void update_meaningfulChange_sendsTaskUpdatedNotification() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee); // priority = HIGH initially
            TaskResponse response = buildTaskResponse(taskId, empId);

            // Keep same assignee but change priority (HIGH → URGENT)
            UpdateTaskRequest request = buildRequest(
                    "API Authentication Implementation", TaskPriority.URGENT,
                    LocalDate.now().plusDays(7), empId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(employee));
            // Same employee → no check-in check needed (prevAssigned.getId().equals(assignedEmployee.getId()))
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
            when(securityUtils.getCurrentUsername()).thenReturn("manager@test.com");

            taskService.update(taskId, request);

            // Verify TASK_UPDATED notification was sent to the assigned employee
            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            verify(notificationService).createNotification(
                    eq(employee), typeCaptor.capture(), any(), any(), any());
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.TASK_UPDATED);
        }

        @Test
        @DisplayName("no TASK_UPDATED notification when values are unchanged")
        void update_noChange_noNotification() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee); // priority=HIGH, title="API Authentication Implementation"
            TaskResponse response = buildTaskResponse(taskId, empId);

            // Same values: same title, same priority, same due date, same category
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "API Authentication Implementation", "Implement auth flow", null, null,
                    empId, TaskPriority.HIGH, TaskStatus.ASSIGNED,
                    LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT
            );

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(employee));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());

            taskService.update(taskId, request);

            // No TASK_UPDATED notification (nothing changed)
            verify(notificationService, never()).createNotification(
                    any(), eq(NotificationType.TASK_UPDATED), any(), any(), any());
        }

        @Test
        @DisplayName("no TASK_UPDATED sent when assignee changes (TASK_ASSIGNED covers it)")
        void update_assigneeChanged_noTaskUpdatedNotification() {
            UUID taskId    = UUID.randomUUID();
            UUID oldEmpId  = UUID.randomUUID();
            UUID newEmpId  = UUID.randomUUID();
            Employee oldEmployee = buildEmployee(oldEmpId);
            Employee newEmployee = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmployee);
            TaskResponse response = buildTaskResponse(taskId, newEmpId);

            Attendance attendance = checkedInAttendance(newEmployee);

            // Change assignee — this should only trigger TASK_ASSIGNED, not TASK_UPDATED
            UpdateTaskRequest request = buildRequest(
                    "API Authentication Implementation", TaskPriority.URGENT,
                    LocalDate.now().plusDays(7), newEmpId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmployee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(newEmpId, LocalDate.now()))
                    .thenReturn(Optional.of(attendance));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());

            taskService.update(taskId, request);

            // TASK_ASSIGNED must be sent; TASK_UPDATED must NOT
            verify(notificationService).createNotification(
                    eq(newEmployee), eq(NotificationType.TASK_ASSIGNED), any(), any(), any());
            verify(notificationService, never()).createNotification(
                    any(), eq(NotificationType.TASK_UPDATED), any(), any(), any());
        }

        @Test
        @DisplayName("no TASK_UPDATED notification when task has no assignee after update")
        void update_noAssignee_noNotification() {
            UUID taskId = UUID.randomUUID();
            Task task = buildTask(taskId, null); // starts with no assignee
            task.setAssignedEmployee(null);
            TaskResponse response = new TaskResponse(
                    taskId, "API Authentication Implementation", "desc", null, null,
                    null, null, null, null, null,
                    TaskPriority.HIGH, TaskStatus.DRAFT, false,
                    LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT,
                    LocalDateTime.now(), LocalDateTime.now(), null, null
            );

            // Update with no assignee but change priority
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "API Authentication Implementation", "desc", null, null,
                    null, TaskPriority.URGENT, TaskStatus.DRAFT,
                    LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT
            );

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());

            taskService.update(taskId, request);

            // No notification sent — no one to notify
            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }
    }
}
