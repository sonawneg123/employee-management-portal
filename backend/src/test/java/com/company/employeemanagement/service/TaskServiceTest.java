package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskStatusRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.TaskMapper;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.TaskActivityRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
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
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskServiceImpl")
class TaskServiceTest {

    @Mock private TaskRepository         taskRepository;
    @Mock private EmployeeRepository     employeeRepository;
    @Mock private AttendanceRepository   attendanceRepository;
    @Mock private TaskActivityRepository taskActivityRepository;
    @Mock private TaskMapper             taskMapper;
    @Mock private SecurityUtils          securityUtils;
    @Mock private NotificationService    notificationService;

    private TaskServiceImpl taskService;

    @BeforeEach
    void setUp() {
        taskService = new TaskServiceImpl(
                taskRepository, employeeRepository, attendanceRepository,
                taskActivityRepository, taskMapper, securityUtils, notificationService);
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

    private Task buildTask(final UUID taskId, final Employee assignedEmployee,
                            final Employee creator) {
        Task task = Task.builder()
                .title("Test Task")
                .description("A test task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.ASSIGNED)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedEmployee(assignedEmployee)
                .createdByEmployee(creator)
                .build();
        task.setId(taskId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private TaskResponse buildTaskResponse(final UUID taskId, final UUID employeeId) {
        return new TaskResponse(
                taskId, "Test Task", "desc", null, null,
                employeeId, "Jane Doe", "EMP-001",
                UUID.randomUUID(), "Manager Name",
                TaskPriority.MEDIUM, TaskStatus.ASSIGNED, false,
                LocalDate.now().plusDays(7), null, TaskCategory.DEVELOPMENT,
                LocalDateTime.now(), LocalDateTime.now(), "manager@test.com", "manager@test.com"
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("creates task with ASSIGNED status when assignee provided")
        void createsWithAssignedStatus() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee assignee = buildEmployee(empId);
            Task saved = buildTask(taskId, assignee, null);
            TaskResponse response = buildTaskResponse(taskId, empId);

            CreateTaskRequest request = new CreateTaskRequest(
                    "Test Task", "desc", null, null,
                    empId, TaskPriority.MEDIUM, LocalDate.now().plusDays(7),
                    null, TaskCategory.DEVELOPMENT
            );

            // Employee must be checked in — stub attendance
            Attendance attendance = Attendance.builder()
                    .employee(assignee)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(java.time.LocalTime.of(9, 0))
                    .status(AttendanceStatus.PRESENT)
                    .build();
            attendance.setId(UUID.randomUUID());
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(attendance));
            when(employeeRepository.findById(empId)).thenReturn(Optional.of(assignee));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
            when(taskRepository.save(any(Task.class))).thenReturn(saved);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(saved));
            when(taskMapper.toResponse(saved)).thenReturn(response);

            TaskResponse result = taskService.create(request);

            assertThat(result.title()).isEqualTo("Test Task");
            verify(taskRepository).save(any(Task.class));
        }

        @Test
        @DisplayName("creates task with DRAFT status when no assignee provided")
        void createsWithDraftStatus() {
            UUID taskId = UUID.randomUUID();
            Task saved = buildTask(taskId, null, null);
            saved.setStatus(TaskStatus.DRAFT);
            TaskResponse response = new TaskResponse(
                    taskId, "Draft Task", null, null, null,
                    null, null, null, null, null,
                    TaskPriority.MEDIUM, TaskStatus.DRAFT, false,
                    LocalDate.now().plusDays(3), null, null,
                    LocalDateTime.now(), LocalDateTime.now(), null, null
            );

            CreateTaskRequest request = new CreateTaskRequest(
                    "Draft Task", null, null, null,
                    null, TaskPriority.MEDIUM, LocalDate.now().plusDays(3),
                    null, null
            );

            // securityUtils.hasRole returns false by default (no ROLE_EMPLOYEE stub needed)
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
            when(taskRepository.save(any(Task.class))).thenReturn(saved);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(saved));
            when(taskMapper.toResponse(saved)).thenReturn(response);

            TaskResponse result = taskService.create(request);

            assertThat(result.status()).isEqualTo(TaskStatus.DRAFT);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when assignee not found")
        void throwsWhenAssigneeNotFound() {
            UUID missingEmpId = UUID.randomUUID();
            CreateTaskRequest request = new CreateTaskRequest(
                    "Title", null, null, null,
                    missingEmpId, null, LocalDate.now().plusDays(1),
                    null, null
            );

            // securityUtils.hasRole returns false by default
            when(employeeRepository.findById(missingEmpId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.create(request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee");

            verify(taskRepository, never()).save(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findById()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findById()")
    class FindById {

        @Test
        @DisplayName("returns task DTO when found")
        void returnsTask() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Task task = buildTask(taskId, buildEmployee(empId), null);
            TaskResponse response = buildTaskResponse(taskId, empId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.findById(taskId);

            assertThat(result.id()).isEqualTo(taskId);
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void throwsWhenNotFound() {
            UUID missing = UUID.randomUUID();
            when(taskRepository.findByIdWithAssociations(missing)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.findById(missing))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task");
        }

        @Test
        @DisplayName("employee can view their own assigned task")
        void employeeCanViewOwnTask() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            TaskResponse response = buildTaskResponse(taskId, empId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.findById(taskId);

            assertThat(result.id()).isEqualTo(taskId);
        }

        @Test
        @DisplayName("employee cannot view another employee's task")
        void employeeCannotViewOtherTask() {
            UUID ownEmpId    = UUID.randomUUID();
            UUID otherEmpId  = UUID.randomUUID();
            UUID taskId      = UUID.randomUUID();
            Employee ownEmployee   = buildEmployee(ownEmpId);
            Employee otherEmployee = buildEmployee(otherEmpId);
            Task task = buildTask(taskId, otherEmployee, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(ownEmployee));

            assertThatThrownBy(() -> taskService.findById(taskId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("assigned to you");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // updateStatus()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("updateStatus()")
    class UpdateStatus {

        @Test
        @DisplayName("checked-in employee can move ASSIGNED task to IN_PROGRESS")
        void employeeCanStartTask() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            task.setStatus(TaskStatus.ASSIGNED);

            // Build a checked-in attendance (no checkout time)
            Attendance todayAttendance = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(java.time.LocalTime.of(9, 0))
                    .status(AttendanceStatus.PRESENT)
                    .build();
            todayAttendance.setId(UUID.randomUUID());

            TaskResponse response = buildTaskResponse(taskId, empId);
            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(todayAttendance));
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.updateStatus(taskId, request);

            assertThat(result).isNotNull();
            verify(taskRepository).save(task);
        }

        @Test
        @DisplayName("checked-out employee cannot start a task")
        void checkedOutEmployeeCannotStartTask() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            task.setStatus(TaskStatus.ASSIGNED);

            // Attendance with both check-in and check-out times
            Attendance checkedOut = Attendance.builder()
                    .employee(employee)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(java.time.LocalTime.of(9, 0))
                    .checkOutTime(java.time.LocalTime.of(17, 0))
                    .status(AttendanceStatus.PRESENT)
                    .build();
            checkedOut.setId(UUID.randomUUID());

            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedOut));

            assertThatThrownBy(() -> taskService.updateStatus(taskId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked in");
        }

        @Test
        @DisplayName("employee without attendance record cannot start a task")
        void notCheckedInEmployeeCannotStartTask() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            task.setStatus(TaskStatus.ASSIGNED);

            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.updateStatus(taskId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked in");
        }

        @Test
        @DisplayName("employee can still view task after checkout (findById)")
        void checkedOutEmployeeCanStillViewTask() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            TaskResponse response = buildTaskResponse(taskId, empId);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(taskMapper.toResponse(task)).thenReturn(response);

            // findById does not check attendance — viewing is always allowed
            TaskResponse result = taskService.findById(taskId);
            assertThat(result.id()).isEqualTo(taskId);
        }

        @Test
        @DisplayName("employee cannot move task directly to COMPLETED")
        void employeeCannotSkipToCompleted() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            task.setStatus(TaskStatus.ASSIGNED);

            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> taskService.updateStatus(taskId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("not permitted");
        }

        @Test
        @DisplayName("employee cannot update another employee's task status")
        void employeeCannotUpdateOtherTaskStatus() {
            UUID ownEmpId   = UUID.randomUUID();
            UUID otherEmpId = UUID.randomUUID();
            UUID taskId     = UUID.randomUUID();
            Employee ownEmployee   = buildEmployee(ownEmpId);
            Employee otherEmployee = buildEmployee(otherEmpId);
            Task task = buildTask(taskId, otherEmployee, null);

            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(ownEmployee));

            assertThatThrownBy(() -> taskService.updateStatus(taskId, request))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("manager can update task status to any value")
        void managerCanUpdateToAnyStatus() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            task.setStatus(TaskStatus.SUBMITTED);
            TaskResponse response = buildTaskResponse(taskId, empId);
            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.updateStatus(taskId, request);

            assertThat(result).isNotNull();
            verify(taskRepository).save(task);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // update() — authorization
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("update()")
    class Update {

        @Test
        @DisplayName("employee cannot fully update a task")
        void employeeCannotFullyUpdate() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);

            UpdateTaskRequest request = new UpdateTaskRequest(
                    "New Title", null, null, null, null,
                    TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                    LocalDate.now().plusDays(5), null, TaskCategory.TESTING
            );

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> taskService.update(taskId, request))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Employees may not update");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void throwsWhenNotFound() {
            UUID missing = UUID.randomUUID();
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Title", null, null, null, null, null, null,
                    LocalDate.now().plusDays(1), null, TaskCategory.OTHER
            );
            when(taskRepository.findByIdWithAssociations(missing)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.update(missing, request))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("manager can update invalid employee assignment throws")
        void throwsWhenNewAssigneeNotFound() {
            UUID taskId   = UUID.randomUUID();
            UUID empId    = UUID.randomUUID();
            UUID badEmpId = UUID.randomUUID();
            Task task = buildTask(taskId, buildEmployee(empId), null);

            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Title", null, null, null, badEmpId, null, null,
                    LocalDate.now().plusDays(1), null, null
            );
            // Also stub checked-in for a hypothetical good employee (not needed here since findById returns empty)

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(employeeRepository.findById(badEmpId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.update(taskId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Employee");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // delete()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("delete()")
    class Delete {

        @Test
        @DisplayName("employee cannot delete a task")
        void employeeCannotDelete() {
            UUID taskId = UUID.randomUUID();
            when(taskRepository.existsById(taskId)).thenReturn(true);
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> taskService.delete(taskId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("Employees may not delete");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when task does not exist")
        void throwsWhenNotFound() {
            UUID missing = UUID.randomUUID();
            when(taskRepository.existsById(missing)).thenReturn(false);

            assertThatThrownBy(() -> taskService.delete(missing))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findAll() — pagination / scoping
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findAll()")
    class FindAll {

        @Test
        @DisplayName("manager gets all tasks with pagination")
        void managerGetsAll() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            TaskResponse response = buildTaskResponse(taskId, empId);
            Pageable pageable = PageRequest.of(0, 20);

            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            Page<UUID> idPage = new PageImpl<>(List.of(taskId), pageable, 1);
            when(taskRepository.findIdsByFilters(any(), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(idPage);
            when(taskRepository.findAllWithAssociationsByIds(List.of(taskId)))
                    .thenReturn(List.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            PageResponse<TaskResponse> result = taskService.findAll(null, null, null, null, null, pageable);

            assertThat(result.content()).hasSize(1);
            assertThat(result.totalElements()).isEqualTo(1L);
        }

        @Test
        @DisplayName("employee gets only their assigned tasks")
        void employeeGetsOwnTasks() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId);
            Task task = buildTask(taskId, employee, null);
            TaskResponse response = buildTaskResponse(taskId, empId);
            Pageable pageable = PageRequest.of(0, 20);

            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));

            Page<UUID> idPage = new PageImpl<>(List.of(taskId), pageable, 1);
            when(taskRepository.findIdsByFilters(eq(empId), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(idPage);
            when(taskRepository.findAllWithAssociationsByIds(List.of(taskId)))
                    .thenReturn(List.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            PageResponse<TaskResponse> result = taskService.findAll(null, null, null, null, null, pageable);

            assertThat(result.content()).hasSize(1);
            verify(taskRepository).findIdsByFilters(eq(empId), any(), any(), any(), any(), eq(pageable));
        }

        @Test
        @DisplayName("returns empty page when no tasks exist")
        void returnsEmptyPage() {
            Pageable pageable = PageRequest.of(0, 20);
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(taskRepository.findIdsByFilters(any(), any(), any(), any(), any(), eq(pageable)))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0L));

            PageResponse<TaskResponse> result = taskService.findAll(null, null, null, null, null, pageable);

            assertThat(result.content()).isEmpty();
            assertThat(result.totalElements()).isZero();
            verify(taskRepository, never()).findAllWithAssociationsByIds(anyList());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // create() — attendance-aware assignment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create() — attendance check")
    class CreateAttendanceCheck {

        @Test
        @DisplayName("throws when assigned employee is not checked in")
        void throwsWhenAssigneeNotCheckedIn() {
            UUID empId = UUID.randomUUID();
            Employee assignee = buildEmployee(empId);

            CreateTaskRequest request = new CreateTaskRequest(
                    "Task", null, null, null,
                    empId, TaskPriority.MEDIUM, LocalDate.now().plusDays(1),
                    null, null
            );

            when(employeeRepository.findById(empId)).thenReturn(Optional.of(assignee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked in");
        }

        @Test
        @DisplayName("throws when assigned employee has already checked out")
        void throwsWhenAssigneeCheckedOut() {
            UUID empId = UUID.randomUUID();
            Employee assignee = buildEmployee(empId);

            Attendance checkedOut = Attendance.builder()
                    .employee(assignee)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(java.time.LocalTime.of(9, 0))
                    .checkOutTime(java.time.LocalTime.of(17, 0))
                    .status(AttendanceStatus.PRESENT)
                    .build();
            checkedOut.setId(UUID.randomUUID());

            CreateTaskRequest request = new CreateTaskRequest(
                    "Task", null, null, null,
                    empId, TaskPriority.MEDIUM, LocalDate.now().plusDays(1),
                    null, null
            );

            when(employeeRepository.findById(empId)).thenReturn(Optional.of(assignee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(empId, LocalDate.now()))
                    .thenReturn(Optional.of(checkedOut));

            assertThatThrownBy(() -> taskService.create(request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked in");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // reassign()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("reassign()")
    class Reassign {

        @Test
        @DisplayName("reassigns task and notifies both parties when new employee is checked in")
        void successfulReassign() {
            UUID taskId     = UUID.randomUUID();
            UUID oldEmpId   = UUID.randomUUID();
            UUID newEmpId   = UUID.randomUUID();
            Employee oldEmployee = buildEmployee(oldEmpId);
            Employee newEmployee = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmployee, null);
            TaskResponse response = buildTaskResponse(taskId, newEmpId);

            Attendance attendance = Attendance.builder()
                    .employee(newEmployee)
                    .attendanceDate(LocalDate.now())
                    .checkInTime(java.time.LocalTime.of(9, 0))
                    .status(AttendanceStatus.PRESENT)
                    .build();
            attendance.setId(UUID.randomUUID());

            ReassignTaskRequest request = new ReassignTaskRequest(newEmpId, "Coverage needed");

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmployee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(newEmpId, LocalDate.now()))
                    .thenReturn(Optional.of(attendance));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());
            when(taskRepository.save(task)).thenReturn(task);
            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(taskMapper.toResponse(task)).thenReturn(response);

            TaskResponse result = taskService.reassign(taskId, request);

            assertThat(result).isNotNull();
            verify(notificationService).createNotification(eq(oldEmployee), any(), any(), any(), any());
            verify(notificationService).createNotification(eq(newEmployee), any(), any(), any(), any());
        }

        @Test
        @DisplayName("throws when new employee is not checked in")
        void throwsWhenNewEmployeeNotCheckedIn() {
            UUID taskId   = UUID.randomUUID();
            UUID newEmpId = UUID.randomUUID();
            Employee oldEmployee = buildEmployee(UUID.randomUUID());
            Employee newEmployee = buildEmployee(newEmpId);
            Task task = buildTask(taskId, oldEmployee, null);

            ReassignTaskRequest request = new ReassignTaskRequest(newEmpId, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(employeeRepository.findById(newEmpId)).thenReturn(Optional.of(newEmployee));
            when(attendanceRepository.findByEmployeeIdAndAttendanceDate(newEmpId, LocalDate.now()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> taskService.reassign(taskId, request))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("checked in");
        }
    }
}
