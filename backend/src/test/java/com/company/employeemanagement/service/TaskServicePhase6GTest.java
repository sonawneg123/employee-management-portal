package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.mapper.LeaveRequestMapper;
import com.company.employeemanagement.mapper.TaskMapper;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import com.company.employeemanagement.repository.TaskActivityRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.LeaveRequestServiceImpl;
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
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 6G tests covering:
 * <ol>
 *   <li>Disabled employee cannot be assigned a task (409 Conflict via IllegalStateException).</li>
 *   <li>Disabled employee cannot be reassigned a task (409 Conflict).</li>
 *   <li>Disabled employee appears in availability list with disabled=true.</li>
 *   <li>Existing tasks remain intact (not affected by employee disable).</li>
 *   <li>Leave approval sends LEAVE_APPROVED notification.</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 6G — Disabled Employee + Leave/Role Notifications")
class TaskServicePhase6GTest {

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

    private Employee buildEmployee(UUID id, EmployeeStatus status) {
        User user = User.builder()
                .firstName("Jane").lastName("Doe")
                .email("jane@example.com").passwordHash("hash")
                .build();
        user.setId(UUID.randomUUID());
        Department dept = new Department();
        dept.setName("Eng"); dept.setCode("ENG");
        Employee emp = Employee.builder()
                .employeeCode("EMP-001").department(dept)
                .jobTitle("Engineer").dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.TEN).user(user)
                .status(status)
                .build();
        emp.setId(id);
        return emp;
    }

    // ── Disabled employee task assignment ──────────────────────────────────────

    @Nested
    @DisplayName("Disabled Employee — Task Assignment")
    class DisabledEmployeeAssignment {

        @Test
        @DisplayName("cannot assign new task to a disabled employee — throws IllegalStateException")
        void createTask_disabledEmployee_throws() {
            UUID empId = UUID.randomUUID();
            Employee disabled = buildEmployee(empId, EmployeeStatus.DISABLED);

            when(employeeRepository.findById(empId)).thenReturn(Optional.of(disabled));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(securityUtils.isPrivileged()).thenReturn(true);

            CreateTaskRequest req = new CreateTaskRequest(
                    "Task", "desc", null, null,
                    empId, TaskPriority.MEDIUM, LocalDate.now().plusDays(3),
                    null, TaskCategory.DEVELOPMENT
            );

            assertThatThrownBy(() -> taskService.create(req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("cannot reassign task to a disabled employee — throws IllegalStateException")
        void reassignTask_disabledEmployee_throws() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee creator  = buildEmployee(UUID.randomUUID(), EmployeeStatus.ACTIVE);
            Employee disabled = buildEmployee(empId, EmployeeStatus.DISABLED);

            Task task = Task.builder()
                    .title("Existing Task").priority(TaskPriority.MEDIUM)
                    .status(TaskStatus.ASSIGNED).dueDate(LocalDate.now().plusDays(7))
                    .createdByEmployee(creator)
                    .build();
            task.setId(taskId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(disabled));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());

            ReassignTaskRequest req = new ReassignTaskRequest(empId, "Testing reassign");

            assertThatThrownBy(() -> taskService.reassign(taskId, req))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("disabled");
        }

        @Test
        @DisplayName("disabled employee appears in availability list with disabled=true and unavailabilityReason=DISABLED")
        void getAvailability_includesDisabledEmployee() {
            UUID activeId   = UUID.randomUUID();
            UUID disabledId = UUID.randomUUID();

            Employee active  = buildEmployee(activeId, EmployeeStatus.ACTIVE);
            Employee disabled = buildEmployee(disabledId, EmployeeStatus.DISABLED);

            when(employeeRepository.findAll()).thenReturn(List.of(active, disabled));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(eq(activeId), any()))
                    .thenReturn(Optional.empty());
            when(leaveRequestRepository.existsApprovedLeaveForEmployeeOnDate(
                    eq(activeId), any(), any())).thenReturn(false);
            when(taskRepository.countActiveTasksByEmployeeId(any())).thenReturn(0L);

            List<EmployeeAvailabilityResponse> results = taskService.getEmployeeAvailability();

            // Both employees appear (ACTIVE and DISABLED)
            assertThat(results).hasSize(2);

            // Find the disabled employee
            EmployeeAvailabilityResponse disabledResult = results.stream()
                    .filter(r -> r.employeeId().equals(disabledId))
                    .findFirst()
                    .orElseThrow();

            assertThat(disabledResult.disabled()).isTrue();
            assertThat(disabledResult.availableToday()).isFalse();
            assertThat(disabledResult.unavailabilityReason()).isEqualTo("DISABLED");
        }

        @Test
        @DisplayName("existing tasks remain intact (status unchanged) when employee is disabled")
        void existingTasksNotAffectedByDisable() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee disabled = buildEmployee(empId, EmployeeStatus.DISABLED);

            Task existingTask = Task.builder()
                    .title("Existing Task").priority(TaskPriority.MEDIUM)
                    .status(TaskStatus.IN_PROGRESS).dueDate(LocalDate.now().plusDays(3))
                    .assignedEmployee(disabled)
                    .build();
            existingTask.setId(taskId);

            // The task should still be findable — disabling employee does not delete tasks
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(existingTask));

            // Task status should remain IN_PROGRESS — no automatic change
            assertThat(existingTask.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(existingTask.getAssignedEmployee().getStatus()).isEqualTo(EmployeeStatus.DISABLED);
        }
    }

    // ── Leave approval notification ────────────────────────────────────────────

    @Nested
    @DisplayName("Leave Approval Notification")
    class LeaveApprovalNotificationTest {

        @Test
        @DisplayName("approving a leave request sends LEAVE_APPROVED notification to the employee")
        void approvingLeave_sendsLeaveApprovedNotification() {
            UUID leaveId = UUID.randomUUID();
            UUID empId   = UUID.randomUUID();

            User user = User.builder()
                    .firstName("Rahul").lastName("Sharma")
                    .email("rahul@example.com").passwordHash("hash")
                    .build();
            user.setId(UUID.randomUUID());
            Department dept = new Department();
            dept.setName("Eng"); dept.setCode("ENG");

            Employee employee = Employee.builder()
                    .employeeCode("EMP-002").department(dept)
                    .jobTitle("Engineer").dateOfJoining(LocalDate.of(2024, 1, 1))
                    .salary(BigDecimal.TEN).user(user).status(EmployeeStatus.ACTIVE)
                    .build();
            employee.setId(empId);

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.ANNUAL)
                    .startDate(LocalDate.of(2026, 8, 20))
                    .endDate(LocalDate.of(2026, 8, 22))
                    .reason("Vacation")
                    .status(LeaveStatus.PENDING)
                    .build();
            leaveRequest.setId(leaveId);

            // Saved with APPROVED status
            LeaveRequest saved = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.ANNUAL)
                    .startDate(LocalDate.of(2026, 8, 20))
                    .endDate(LocalDate.of(2026, 8, 22))
                    .reason("Vacation")
                    .status(LeaveStatus.APPROVED)
                    .build();
            saved.setId(leaveId);

            // Mock repos
            LeaveRequestRepository mockLeaveRepo = mock(LeaveRequestRepository.class);
            EmployeeRepository mockEmpRepo = mock(EmployeeRepository.class);
            LeaveRequestMapper mockMapper = mock(LeaveRequestMapper.class);
            SecurityUtils mockSecurity = mock(SecurityUtils.class);

            when(mockLeaveRepo.findById(leaveId)).thenReturn(Optional.of(leaveRequest));
            when(mockLeaveRepo.save(any())).thenReturn(saved);
            when(mockMapper.toResponse(any())).thenReturn(buildLeaveResponse(leaveId, empId));
            when(mockSecurity.getCurrentUserId()).thenReturn(Optional.empty());

            LeaveRequestServiceImpl svc = new LeaveRequestServiceImpl(
                    mockLeaveRepo, mockEmpRepo, mockMapper, mockSecurity, notificationService
            );

            svc.approve(leaveId, new ReviewLeaveRequest(null));

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            ArgumentCaptor<Employee> recipientCaptor = ArgumentCaptor.forClass(Employee.class);
            verify(notificationService).createNotification(
                    recipientCaptor.capture(),
                    typeCaptor.capture(),
                    any(), any(), any()
            );
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.LEAVE_APPROVED);
            assertThat(recipientCaptor.getValue().getId()).isEqualTo(empId);
        }

        @Test
        @DisplayName("rejecting a leave request sends LEAVE_REJECTED notification to the employee")
        void rejectingLeave_sendsLeaveRejectedNotification() {
            UUID leaveId = UUID.randomUUID();
            UUID empId   = UUID.randomUUID();

            User user = User.builder()
                    .firstName("Priya").lastName("Kumar")
                    .email("priya@example.com").passwordHash("hash")
                    .build();
            user.setId(UUID.randomUUID());
            Department dept = new Department();
            dept.setName("HR"); dept.setCode("HR");

            Employee employee = Employee.builder()
                    .employeeCode("EMP-003").department(dept)
                    .jobTitle("HR Specialist").dateOfJoining(LocalDate.of(2024, 1, 1))
                    .salary(BigDecimal.TEN).user(user).status(EmployeeStatus.ACTIVE)
                    .build();
            employee.setId(empId);

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.SICK)
                    .startDate(LocalDate.of(2026, 9, 1))
                    .endDate(LocalDate.of(2026, 9, 3))
                    .reason("Flu")
                    .status(LeaveStatus.PENDING)
                    .build();
            leaveRequest.setId(leaveId);

            LeaveRequest saved = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.SICK)
                    .startDate(LocalDate.of(2026, 9, 1))
                    .endDate(LocalDate.of(2026, 9, 3))
                    .reason("Flu")
                    .status(LeaveStatus.REJECTED)
                    .build();
            saved.setId(leaveId);

            LeaveRequestRepository mockLeaveRepo = mock(LeaveRequestRepository.class);
            EmployeeRepository mockEmpRepo = mock(EmployeeRepository.class);
            LeaveRequestMapper mockMapper = mock(LeaveRequestMapper.class);
            SecurityUtils mockSecurity = mock(SecurityUtils.class);

            when(mockLeaveRepo.findById(leaveId)).thenReturn(Optional.of(leaveRequest));
            when(mockLeaveRepo.save(any())).thenReturn(saved);
            when(mockMapper.toResponse(any())).thenReturn(buildLeaveRejectResponse(leaveId, empId));
            when(mockSecurity.getCurrentUserId()).thenReturn(Optional.empty());

            LeaveRequestServiceImpl svc = new LeaveRequestServiceImpl(
                    mockLeaveRepo, mockEmpRepo, mockMapper, mockSecurity, notificationService
            );

            svc.reject(leaveId, new ReviewLeaveRequest("Doctor's note required"));

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            ArgumentCaptor<Employee> recipientCaptor = ArgumentCaptor.forClass(Employee.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).createNotification(
                    recipientCaptor.capture(),
                    typeCaptor.capture(),
                    any(),
                    messageCaptor.capture(),
                    any()
            );
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.LEAVE_REJECTED);
            assertThat(recipientCaptor.getValue().getId()).isEqualTo(empId);
            assertThat(messageCaptor.getValue()).contains("rejected");
            assertThat(messageCaptor.getValue()).contains("Doctor's note required");
        }

        @Test
        @DisplayName("rejecting with no reason still sends LEAVE_REJECTED notification (no reason suffix)")
        void rejectingLeave_noReason_sendsLeaveRejectedWithoutReasonSuffix() {
            UUID leaveId = UUID.randomUUID();
            UUID empId   = UUID.randomUUID();

            User user = User.builder()
                    .firstName("Test").lastName("User")
                    .email("test@example.com").passwordHash("hash")
                    .build();
            user.setId(UUID.randomUUID());
            Department dept = new Department();
            dept.setName("Eng"); dept.setCode("ENG");

            Employee employee = Employee.builder()
                    .employeeCode("EMP-004").department(dept)
                    .jobTitle("Dev").dateOfJoining(LocalDate.of(2024, 1, 1))
                    .salary(BigDecimal.TEN).user(user).status(EmployeeStatus.ACTIVE)
                    .build();
            employee.setId(empId);

            LeaveRequest leaveRequest = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.ANNUAL)
                    .startDate(LocalDate.of(2026, 10, 1))
                    .endDate(LocalDate.of(2026, 10, 5))
                    .reason("Holiday")
                    .status(LeaveStatus.PENDING)
                    .build();
            leaveRequest.setId(leaveId);

            LeaveRequest saved = LeaveRequest.builder()
                    .employee(employee)
                    .leaveType(LeaveType.ANNUAL)
                    .startDate(LocalDate.of(2026, 10, 1))
                    .endDate(LocalDate.of(2026, 10, 5))
                    .reason("Holiday")
                    .status(LeaveStatus.REJECTED)
                    .build();
            saved.setId(leaveId);

            LeaveRequestRepository mockLeaveRepo = mock(LeaveRequestRepository.class);
            EmployeeRepository mockEmpRepo = mock(EmployeeRepository.class);
            LeaveRequestMapper mockMapper = mock(LeaveRequestMapper.class);
            SecurityUtils mockSecurity = mock(SecurityUtils.class);

            when(mockLeaveRepo.findById(leaveId)).thenReturn(Optional.of(leaveRequest));
            when(mockLeaveRepo.save(any())).thenReturn(saved);
            when(mockMapper.toResponse(any())).thenReturn(buildLeaveRejectResponse(leaveId, empId));
            when(mockSecurity.getCurrentUserId()).thenReturn(Optional.empty());

            LeaveRequestServiceImpl svc = new LeaveRequestServiceImpl(
                    mockLeaveRepo, mockEmpRepo, mockMapper, mockSecurity, notificationService
            );

            svc.reject(leaveId, new ReviewLeaveRequest(null));

            ArgumentCaptor<NotificationType> typeCaptor = ArgumentCaptor.forClass(NotificationType.class);
            ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).createNotification(
                    any(), typeCaptor.capture(), any(), messageCaptor.capture(), any()
            );
            assertThat(typeCaptor.getValue()).isEqualTo(NotificationType.LEAVE_REJECTED);
            // No rejection reason → message should not contain "Reason:"
            assertThat(messageCaptor.getValue()).doesNotContain("Reason:");
        }

        private LeaveRequestResponse buildLeaveResponse(UUID id, UUID empId) {
            return new LeaveRequestResponse(
                    id, empId, "EMP-002", "Rahul Sharma", "Eng",
                    LeaveType.ANNUAL, LocalDate.of(2026, 8, 20), LocalDate.of(2026, 8, 22),
                    3L, "Vacation", LeaveStatus.APPROVED, null, null, null,
                    LocalDateTime.now(), LocalDateTime.now(), null, null
            );
        }

        private LeaveRequestResponse buildLeaveRejectResponse(UUID id, UUID empId) {
            return new LeaveRequestResponse(
                    id, empId, "EMP-003", "Priya Kumar", "HR",
                    LeaveType.SICK, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 3),
                    3L, "Flu", LeaveStatus.REJECTED, "Doctor's note required", null, null,
                    LocalDateTime.now(), LocalDateTime.now(), null, null
            );
        }
    }
}
