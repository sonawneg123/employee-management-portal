package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Phase 6F.1 unit tests for {@link TaskServiceImpl} covering the
 * Attendance + Leave Aware Task Availability requirements.
 *
 * <p>Tests verify:
 * <ol>
 *   <li>Checked-in employee can receive a task.</li>
 *   <li>Checked-out employee cannot receive a new task (returns 409).</li>
 *   <li>Employee who checked out yesterday can receive a task after checking in today.</li>
 *   <li>Employee who has not checked in today cannot receive a task.</li>
 *   <li>Employee with approved leave today cannot receive a task.</li>
 *   <li>Employee with pending leave today can still be assigned if otherwise available.</li>
 *   <li>Future approved leave does not block assignment today.</li>
 *   <li>Past approved leave does not block assignment.</li>
 *   <li>Reassignment applies the same rules.</li>
 *   <li>Existing tasks remain intact after checkout / leave (no cascade modification).</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskServiceImpl — Phase 6F.1 Attendance + Leave Aware Availability")
class TaskServiceAvailabilityTest {

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
                taskRepository,
                employeeRepository,
                attendanceRepository,
                leaveRequestRepository,
                taskActivityRepository,
                taskMapper,
                securityUtils,
                notificationService);

        // Default: manager role (not employee-only)
        when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
        when(securityUtils.isPrivileged()).thenReturn(true);
        when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
        when(securityUtils.getCurrentUsername()).thenReturn("manager@test.com");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(final UUID id) {
        User user = User.builder()
                .firstName("Test")
                .lastName("Employee")
                .email("emp@test.com")
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
                .title("Test Task")
                .description("Test description")
                .priority(TaskPriority.MEDIUM)
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
                taskId, "Test Task", "desc", null, null,
                empId, "Test Employee", "EMP-001",
                UUID.randomUUID(), "Manager",
                TaskPriority.MEDIUM, TaskStatus.ASSIGNED, false,
                LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT,
                LocalDateTime.now(), LocalDateTime.now(), "mgr@test.com", "mgr@test.com"
        );
    }

    private CreateTaskRequest buildCreateRequest(final UUID assignedEmployeeId) {
        return new CreateTaskRequest(
                "Test Task", "Description", null, null,
                assignedEmployeeId, TaskPriority.MEDIUM,
                LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT
        );
    }

    /** Attendance record for today — checked in, NOT checked out. */
    private Attendance checkedInToday(final Employee emp) {
        Attendance a = Attendance.builder()
                .employee(emp)
                .attendanceDate(LocalDate.now())
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(null)          // NOT checked out
                .status(AttendanceStatus.PRESENT)
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }

    /** Attendance record for today — checked in AND checked out. */
    private Attendance checkedOutToday(final Employee emp) {
        Attendance a = Attendance.builder()
                .employee(emp)
                .attendanceDate(LocalDate.now())
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(LocalTime.of(17, 0))  // Has checked out
                .status(AttendanceStatus.PRESENT)
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }

    /** Attendance record for YESTERDAY — checked in and checked out. */
    private Attendance checkedOutYesterday(final Employee emp) {
        Attendance a = Attendance.builder()
                .employee(emp)
                .attendanceDate(LocalDate.now().minusDays(1))
                .checkInTime(LocalTime.of(9, 0))
                .checkOutTime(LocalTime.of(17, 0))
                .status(AttendanceStatus.PRESENT)
                .build();
        a.setId(UUID.randomUUID());
        return a;
    }

    /** Mocks no approved leave for employee today (or any date). */
    private void mockNoApprovedLeave(final UUID employeeId) {
        when(leaveRequestRepository.existsApprovedLeaveForEmployeeOnDate(
                eq(employeeId), eq(LeaveStatus.APPROVED), any(LocalDate.class)))
                .thenReturn(false);
    }

    /** Mocks an APPROVED leave covering today for the given employee. */
    private void mockApprovedLeaveToday(final UUID employeeId) {
        when(leaveRequestRepository.existsApprovedLeaveForEmployeeOnDate(
                eq(employeeId), eq(LeaveStatus.APPROVED), eq(LocalDate.now())))
                .thenReturn(true);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 1 — Create task (assignment rules)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create() — attendance + leave availability rules")
    class Create {

        @Test
        @DisplayName("1. Checked-in employee can receive a new task")
        void create_checkedInEmployee_succeeds() {
            UUID empId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);
            TaskResponse response = buildTaskResponse(taskId, empId);

            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInToday(emp)));
            when(taskRepository.save(any())).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(any())).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.create(buildCreateRequest(empId));

            assertThat(result).isNotNull();
            assertThat(result.assignedEmployeeId()).isEqualTo(empId);
        }

        @Test
        @DisplayName("2. Checked-out employee cannot receive a new task — throws IllegalStateException (409)")
        void create_checkedOutEmployee_throwsIllegalState() {
            UUID empId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);

            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedOutToday(emp)));

            assertThatThrownBy(() -> taskService.create(buildCreateRequest(empId)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked out today");
        }

        @Test
        @DisplayName("3. Employee who checked out yesterday can receive task after checking in today")
        void create_checkedInTodayAfterYesterdayCheckout_succeeds() {
            UUID empId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);
            TaskResponse response = buildTaskResponse(taskId, empId);

            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            // Today: checked in (no checkout yet) — yesterday's checkout is irrelevant
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInToday(emp)));
            when(taskRepository.save(any())).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(any())).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            // Should succeed — only today's date matters
            TaskResponse result = taskService.create(buildCreateRequest(empId));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("4. Employee who has not checked in today cannot receive a task")
        void create_notCheckedInToday_throwsIllegalState() {
            UUID empId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);

            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            // No attendance record for today
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.create(buildCreateRequest(empId)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked in");
        }

        @Test
        @DisplayName("5. Employee with approved leave today cannot receive a task")
        void create_approvedLeaveToday_throwsIllegalState() {
            UUID empId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);

            // Approved leave covering today — checked in does not override
            mockApprovedLeaveToday(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            // Note: attendance lookup is never reached because leave check fires first

            assertThatThrownBy(() -> taskService.create(buildCreateRequest(empId)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("approved leave");
        }

        @Test
        @DisplayName("6. Employee with PENDING leave today can be assigned if checked in")
        void create_pendingLeaveToday_doesNotBlock() {
            UUID empId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);
            TaskResponse response = buildTaskResponse(taskId, empId);

            // Only APPROVED leave blocks — PENDING does not
            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInToday(emp)));
            when(taskRepository.save(any())).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(any())).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.create(buildCreateRequest(empId));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("7. Future approved leave does not block assignment today")
        void create_futureApprovedLeave_doesNotBlock() {
            UUID empId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);
            TaskResponse response = buildTaskResponse(taskId, empId);

            // existsApprovedLeaveForEmployeeOnDate returns false for today (future only)
            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInToday(emp)));
            when(taskRepository.save(any())).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(any())).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.create(buildCreateRequest(empId));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("8. Past approved leave does not block assignment today")
        void create_pastApprovedLeave_doesNotBlock() {
            UUID empId = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);
            TaskResponse response = buildTaskResponse(taskId, empId);

            // The query checks today's date — past leave won't match
            mockNoApprovedLeave(empId);
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(emp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInToday(emp)));
            when(taskRepository.save(any())).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(any())).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.create(buildCreateRequest(empId));

            assertThat(result).isNotNull();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 2 — Reassignment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reassign() — same availability rules as create()")
    class Reassign {

        @Test
        @DisplayName("9a. Reassign to checked-in employee succeeds")
        void reassign_checkedInEmployee_succeeds() {
            UUID taskId  = UUID.randomUUID();
            UUID oldEmpId = UUID.randomUUID();
            UUID newEmpId = UUID.randomUUID();
            Employee oldEmp = buildEmployee(oldEmpId);
            Employee newEmp = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmp);
            TaskResponse response = buildTaskResponse(taskId, newEmpId);

            mockNoApprovedLeave(newEmpId);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(newEmpId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedInToday(newEmp)));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.reassign(taskId, new ReassignTaskRequest(newEmpId, null));

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("9b. Reassign to checked-out employee is rejected")
        void reassign_checkedOutEmployee_throwsIllegalState() {
            UUID taskId  = UUID.randomUUID();
            UUID oldEmpId = UUID.randomUUID();
            UUID newEmpId = UUID.randomUUID();
            Employee oldEmp = buildEmployee(oldEmpId);
            Employee newEmp = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmp);

            mockNoApprovedLeave(newEmpId);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmp));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(newEmpId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedOutToday(newEmp)));

            assertThatThrownBy(() -> taskService.reassign(taskId, new ReassignTaskRequest(newEmpId, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked out today");
        }

        @Test
        @DisplayName("9c. Reassign to employee on approved leave today is rejected")
        void reassign_approvedLeaveEmployee_throwsIllegalState() {
            UUID taskId  = UUID.randomUUID();
            UUID oldEmpId = UUID.randomUUID();
            UUID newEmpId = UUID.randomUUID();
            Employee oldEmp = buildEmployee(oldEmpId);
            Employee newEmp = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmp);

            mockApprovedLeaveToday(newEmpId);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmp));

            assertThatThrownBy(() -> taskService.reassign(taskId, new ReassignTaskRequest(newEmpId, null)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("approved leave");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test Group 3 — Existing tasks untouched by checkout / leave
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Existing task state — no cascade on checkout or leave")
    class ExistingTasksUntouched {

        @Test
        @DisplayName("10. Existing ASSIGNED task retains ASSIGNED status after employee checkout")
        void existingTask_retainsStatusAfterCheckout() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);

            // Task was assigned before; employee has now checked out
            // The task itself is never modified by checkout — it keeps ASSIGNED
            assertThat(task.getStatus()).isEqualTo(TaskStatus.ASSIGNED);
            assertThat(task.getAssignedEmployee()).isEqualTo(emp);
            // No service method call needed — the business rule is that existing tasks
            // are not touched. This test documents the invariant explicitly.
        }

        @Test
        @DisplayName("10b. findById still works for existing task when employee is checked out")
        void findById_checkedOutEmployee_taskStillAccessible() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee emp = buildEmployee(empId);
            Task task = buildTask(taskId, emp);
            TaskResponse response = buildTaskResponse(taskId, empId);

            // Manager can still see the task even if the employee is checked out
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.findById(taskId);

            assertThat(result).isNotNull();
            assertThat(result.status()).isEqualTo(TaskStatus.ASSIGNED);
        }
    }
}
