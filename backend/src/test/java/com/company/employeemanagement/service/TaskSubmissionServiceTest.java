package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskSubmissionRequest;
import com.company.employeemanagement.dto.request.RequestChangesRequest;
import com.company.employeemanagement.dto.request.UpdateTaskSubmissionRequest;
import com.company.employeemanagement.dto.response.TaskSubmissionResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.SubmissionStatus;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskActivityRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.TaskSubmissionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskSubmissionServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskSubmissionServiceImpl")
class TaskSubmissionServiceTest {

    @Mock private TaskSubmissionRepository submissionRepository;
    @Mock private TaskRepository           taskRepository;
    @Mock private TaskActivityRepository   taskActivityRepository;
    @Mock private SecurityUtils            securityUtils;
    @Mock private NotificationService      notificationService;
    @Mock private FileStorageService       fileStorageService;
    @Mock private FileValidationService    fileValidationService;

    private TaskSubmissionServiceImpl submissionService;

    @BeforeEach
    void setUp() {
        submissionService = new TaskSubmissionServiceImpl(
                submissionRepository, taskRepository, taskActivityRepository,
                securityUtils, notificationService,
                fileStorageService, fileValidationService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(final UUID id, final String firstName, final String lastName) {
        User user = User.builder()
                .firstName(firstName)
                .lastName(lastName)
                .email(firstName.toLowerCase() + "@example.com")
                .passwordHash("hash")
                .build();
        user.setId(UUID.randomUUID());

        Department dept = new Department();
        dept.setName("Engineering");
        dept.setCode("ENG");

        Employee emp = Employee.builder()
                .employeeCode("EMP-00" + id.toString().charAt(0))
                .department(dept)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(75000))
                .user(user)
                .build();
        emp.setId(id);
        return emp;
    }

    private Task buildTask(final UUID taskId, final Employee assignee,
                            final Employee creator, final TaskStatus status) {
        Task task = Task.builder()
                .title("Test Task")
                .description("A test task")
                .priority(TaskPriority.MEDIUM)
                .status(status)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedEmployee(assignee)
                .createdByEmployee(creator)
                .build();
        task.setId(taskId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private TaskSubmission buildSubmission(final UUID subId, final Task task,
                                            final Employee submittedBy,
                                            final SubmissionStatus status) {
        TaskSubmission sub = TaskSubmission.builder()
                .task(task)
                .submittedBy(submittedBy)
                .submissionNotes("Did the work")
                .workCompleted("All done")
                .reviewStatus(status)
                .submittedAt(LocalDateTime.now().minusHours(1))
                .build();
        sub.setId(subId);
        sub.setCreatedAt(LocalDateTime.now().minusHours(1));
        sub.setUpdatedAt(LocalDateTime.now().minusHours(1));
        return sub;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // createSubmission()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("createSubmission()")
    class CreateSubmission {

        @Test
        @DisplayName("employee can submit an IN_PROGRESS task")
        void employeeCanSubmit() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(UUID.randomUUID(), "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.IN_PROGRESS);
            TaskSubmission saved = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Done the work", "Implemented feature", null
            );

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(saved);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(saved));

            TaskSubmissionResponse result = submissionService.createSubmission(taskId, request, null);

            assertThat(result.reviewStatus()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
            verify(taskRepository).save(task);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
        }

        @Test
        @DisplayName("cannot submit an ASSIGNED task (must be IN_PROGRESS first)")
        void cannotSubmitAssignedTask() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Task task = buildTask(taskId, employee, null, TaskStatus.ASSIGNED);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Notes", null, null
            );

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> submissionService.createSubmission(taskId, request, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("IN_PROGRESS");

            verify(taskRepository, never()).save(any());
        }

        @Test
        @DisplayName("cannot submit a task assigned to another employee")
        void cannotSubmitOtherEmployeeTask() {
            UUID ownEmpId   = UUID.randomUUID();
            UUID otherEmpId = UUID.randomUUID();
            UUID taskId     = UUID.randomUUID();
            Employee ownEmployee   = buildEmployee(ownEmpId, "Jane", "Doe");
            Employee otherEmployee = buildEmployee(otherEmpId, "Bob", "Smith");
            Task task = buildTask(taskId, otherEmployee, null, TaskStatus.IN_PROGRESS);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Notes", null, null
            );

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(ownEmployee));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> submissionService.createSubmission(taskId, request, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("assigned to you");
        }

        @Test
        @DisplayName("throws ResourceNotFoundException when task not found")
        void throwsWhenTaskNotFound() {
            UUID missing = UUID.randomUUID();
            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Notes", null, null
            );

            when(taskRepository.findByIdWithAssociations(missing)).thenReturn(Optional.empty());
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(
                    buildEmployee(UUID.randomUUID(), "Jane", "Doe")));

            assertThatThrownBy(() -> submissionService.createSubmission(missing, request, null))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Task");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // approve()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("approve()")
    class Approve {

        @Test
        @DisplayName("manager can approve a PENDING_REVIEW submission")
        void managerCanApprove() {
            UUID empId  = UUID.randomUUID();
            UUID mgId   = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(mgId, "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(sub)).thenReturn(sub);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));

            TaskSubmissionResponse result = submissionService.approve(subId);

            assertThat(sub.getReviewStatus()).isEqualTo(SubmissionStatus.APPROVED);
            assertThat(task.getStatus()).isEqualTo(TaskStatus.COMPLETED);
        }

        @Test
        @DisplayName("employee cannot approve a submission")
        void employeeCannotApprove() {
            UUID subId = UUID.randomUUID();

            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> submissionService.approve(subId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("managers");
        }

        @Test
        @DisplayName("cannot approve an already-approved submission")
        void cannotApproveAlreadyApproved() {
            UUID empId  = UUID.randomUUID();
            UUID mgId   = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(mgId, "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.COMPLETED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.APPROVED);

            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));

            assertThatThrownBy(() -> submissionService.approve(subId))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING_REVIEW");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // requestChanges()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("requestChanges()")
    class RequestChanges {

        @Test
        @DisplayName("manager can request changes on PENDING_REVIEW submission")
        void managerCanRequestChanges() {
            UUID empId  = UUID.randomUUID();
            UUID mgId   = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(mgId, "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            RequestChangesRequest request = new RequestChangesRequest("Please add tests");

            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(manager));
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(sub)).thenReturn(sub);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));

            TaskSubmissionResponse result = submissionService.requestChanges(subId, request);

            assertThat(sub.getReviewStatus()).isEqualTo(SubmissionStatus.CHANGES_REQUESTED);
            assertThat(sub.getReviewComment()).isEqualTo("Please add tests");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        }

        @Test
        @DisplayName("employee cannot request changes")
        void employeeCannotRequestChanges() {
            UUID subId = UUID.randomUUID();
            RequestChangesRequest request = new RequestChangesRequest("comment");

            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> submissionService.requestChanges(subId, request))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // resubmit()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("resubmit()")
    class Resubmit {

        @Test
        @DisplayName("employee can resubmit after changes requested")
        void employeeCanResubmit() {
            UUID empId  = UUID.randomUUID();
            UUID mgId   = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(mgId, "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.IN_PROGRESS);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.CHANGES_REQUESTED);
            sub.setReviewComment("Please add tests");

            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest(
                    "Added tests as requested", "Wrote unit tests", null
            );

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(sub)).thenReturn(sub);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));

            TaskSubmissionResponse result = submissionService.resubmit(subId, request, null);

            assertThat(sub.getReviewStatus()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
            assertThat(sub.getSubmissionNotes()).isEqualTo("Added tests as requested");
            assertThat(task.getStatus()).isEqualTo(TaskStatus.SUBMITTED);
        }

        @Test
        @DisplayName("cannot resubmit a PENDING_REVIEW submission")
        void cannotResubmitPendingReview() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Task task = buildTask(taskId, employee, null, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest(
                    "Updated notes", null, null
            );

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> submissionService.resubmit(subId, request, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("changes have been requested");
        }

        @Test
        @DisplayName("cannot resubmit another employee's submission")
        void cannotResubmitOtherEmployeeSubmission() {
            UUID ownEmpId   = UUID.randomUUID();
            UUID otherEmpId = UUID.randomUUID();
            UUID taskId     = UUID.randomUUID();
            UUID subId      = UUID.randomUUID();
            Employee ownEmployee   = buildEmployee(ownEmpId, "Jane", "Doe");
            Employee otherEmployee = buildEmployee(otherEmpId, "Bob", "Smith");
            Task task = buildTask(taskId, otherEmployee, null, TaskStatus.IN_PROGRESS);
            TaskSubmission sub = buildSubmission(subId, task, otherEmployee, SubmissionStatus.CHANGES_REQUESTED);

            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest(
                    "Notes", null, null
            );

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(ownEmployee));

            assertThatThrownBy(() -> submissionService.resubmit(subId, request, null))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own submissions");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // getSubmissionsForTask()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getSubmissionsForTask()")
    class GetSubmissions {

        @Test
        @DisplayName("manager can view submissions for any task")
        void managerCanViewAll() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Task task = buildTask(taskId, employee, null, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(submissionRepository.findAllByTaskIdOrderBySubmittedAtDesc(taskId))
                    .thenReturn(List.of(sub));

            List<TaskSubmissionResponse> result = submissionService.getSubmissionsForTask(taskId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).reviewStatus()).isEqualTo(SubmissionStatus.PENDING_REVIEW);
        }

        @Test
        @DisplayName("employee can view submissions for their own task")
        void employeeCanViewOwnTaskSubmissions() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Task task = buildTask(taskId, employee, null, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(submissionRepository.findAllByTaskIdOrderBySubmittedAtDesc(taskId))
                    .thenReturn(List.of(sub));

            List<TaskSubmissionResponse> result = submissionService.getSubmissionsForTask(taskId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("employee cannot view submissions for another employee's task")
        void employeeCannotViewOtherTaskSubmissions() {
            UUID ownEmpId   = UUID.randomUUID();
            UUID otherEmpId = UUID.randomUUID();
            UUID taskId     = UUID.randomUUID();
            Employee ownEmployee   = buildEmployee(ownEmpId, "Jane", "Doe");
            Employee otherEmployee = buildEmployee(otherEmpId, "Bob", "Smith");
            Task task = buildTask(taskId, otherEmployee, null, TaskStatus.SUBMITTED);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(ownEmployee));

            assertThatThrownBy(() -> submissionService.getSubmissionsForTask(taskId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Attachment functionality (Phase 6B.1)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("File attachment (Phase 6B.1)")
    class Attachment {

        @Test
        @DisplayName("submission without a file still works")
        void submissionWithoutFileWorks() {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(UUID.randomUUID(), "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.IN_PROGRESS);
            TaskSubmission saved = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Text-only submission", null, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(saved);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(saved));

            assertThatCode(() -> submissionService.createSubmission(taskId, request, null))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("submission with notes + file stores attachment metadata")
        void submissionWithFileStoresMetadata() throws IOException {
            UUID empId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(UUID.randomUUID(), "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.IN_PROGRESS);
            TaskSubmission saved = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);

            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file", "report.pdf", "application/pdf", new byte[1024]);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Submission with attachment", null, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(any(TaskSubmission.class))).thenReturn(saved);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(saved));
            when(fileStorageService.store(pdfFile, subId))
                    .thenReturn("submissions/" + subId + "/abc123.pdf");

            // Should not throw — file metadata is stored on the entity
            assertThatCode(() -> submissionService.createSubmission(taskId, request, pdfFile))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("resubmission with replacement file works")
        void resubmitWithReplacementFile() throws IOException {
            UUID empId  = UUID.randomUUID();
            UUID mgId   = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(mgId, "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.IN_PROGRESS);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.CHANGES_REQUESTED);
            // Simulate existing attachment
            sub.setAttachmentStorageKey("submissions/" + subId + "/old.pdf");
            sub.setAttachmentOriginalName("old.pdf");

            MockMultipartFile newFile = new MockMultipartFile(
                    "file", "updated.pdf", "application/pdf", new byte[2048]);

            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest(
                    "Updated with new file", null, null);

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(taskRepository.save(task)).thenReturn(task);
            when(submissionRepository.save(sub)).thenReturn(sub);
            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(fileStorageService.store(newFile, subId))
                    .thenReturn("submissions/" + subId + "/new.pdf");

            assertThatCode(() -> submissionService.resubmit(subId, request, newFile))
                    .doesNotThrowAnyException();

            // Previous file should have been deleted
            org.mockito.Mockito.verify(fileStorageService)
                    .delete("submissions/" + subId + "/old.pdf");
        }

        @Test
        @DisplayName("employee cannot download another employee's attachment")
        void employeeCannotDownloadOtherAttachment() {
            UUID ownEmpId   = UUID.randomUUID();
            UUID otherEmpId = UUID.randomUUID();
            UUID subId      = UUID.randomUUID();
            Employee ownEmployee   = buildEmployee(ownEmpId, "Jane", "Doe");
            Employee otherEmployee = buildEmployee(otherEmpId, "Bob", "Smith");
            UUID taskId = UUID.randomUUID();
            Task task = buildTask(taskId, otherEmployee, null, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, otherEmployee, SubmissionStatus.PENDING_REVIEW);
            sub.setAttachmentStorageKey("submissions/" + subId + "/file.pdf");

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(ownEmployee));

            assertThatThrownBy(() -> submissionService.downloadAttachment(subId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("own submissions");
        }

        @Test
        @DisplayName("employee can download their own attachment")
        void employeeCanDownloadOwnAttachment() throws IOException {
            UUID empId  = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Task task = buildTask(taskId, employee, null, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);
            sub.setAttachmentStorageKey("submissions/" + subId + "/file.pdf");
            sub.setAttachmentOriginalName("report.pdf");
            sub.setAttachmentMimeType("application/pdf");
            sub.setAttachmentSizeBytes(1024L);

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(fileStorageService.openForRead("submissions/" + subId + "/file.pdf"))
                    .thenReturn(new java.io.ByteArrayInputStream(new byte[1024]));

            TaskSubmissionService.AttachmentDownload dl = submissionService.downloadAttachment(subId);

            assertThat(dl.contentType()).isEqualTo("application/pdf");
            assertThat(dl.originalFilename()).isEqualTo("report.pdf");
            assertThat(dl.sizeBytes()).isEqualTo(1024L);
        }

        @Test
        @DisplayName("manager can download any attachment")
        void managerCanDownloadAnyAttachment() throws IOException {
            UUID empId  = UUID.randomUUID();
            UUID mgId   = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Employee manager  = buildEmployee(mgId, "John", "Manager");
            Task task = buildTask(taskId, employee, manager, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);
            sub.setAttachmentStorageKey("submissions/" + subId + "/file.pdf");
            sub.setAttachmentOriginalName("report.pdf");
            sub.setAttachmentMimeType("application/pdf");
            sub.setAttachmentSizeBytes(2048L);

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false); // manager
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(fileStorageService.openForRead("submissions/" + subId + "/file.pdf"))
                    .thenReturn(new java.io.ByteArrayInputStream(new byte[2048]));

            assertThatCode(() -> submissionService.downloadAttachment(subId))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("downloadAttachment throws 404 when no attachment present")
        void downloadThrowsWhenNoAttachment() {
            UUID empId  = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane", "Doe");
            Task task = buildTask(taskId, employee, null, TaskStatus.SUBMITTED);
            TaskSubmission sub = buildSubmission(subId, task, employee, SubmissionStatus.PENDING_REVIEW);
            // No attachment

            when(submissionRepository.findByIdWithAssociations(subId)).thenReturn(Optional.of(sub));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));

            assertThatThrownBy(() -> submissionService.downloadAttachment(subId))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("no file attachment");
        }
    }
}
