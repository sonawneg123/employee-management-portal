package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.TaskAttachmentResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskAttachment;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.repository.TaskAttachmentRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.TaskAttachmentServiceImpl;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskAttachmentServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskAttachmentServiceImpl")
class TaskAttachmentServiceTest {

    @Mock private TaskAttachmentRepository attachmentRepository;
    @Mock private TaskRepository           taskRepository;
    @Mock private FileStorageService       fileStorageService;
    @Mock private FileValidationService    fileValidationService;
    @Mock private SecurityUtils            securityUtils;

    private TaskAttachmentServiceImpl attachmentService;

    @BeforeEach
    void setUp() {
        attachmentService = new TaskAttachmentServiceImpl(
                attachmentRepository, taskRepository,
                fileStorageService, fileValidationService, securityUtils);
    }

    private Employee buildEmployee(final UUID id, final String name) {
        User user = User.builder()
                .firstName(name)
                .lastName("Doe")
                .email(name.toLowerCase() + "@example.com")
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
                .salary(BigDecimal.valueOf(70000))
                .user(user)
                .build();
        emp.setId(id);
        return emp;
    }

    private Task buildTask(final UUID taskId, final Employee assignee) {
        Task task = Task.builder()
                .title("Test Task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.ASSIGNED)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedEmployee(assignee)
                .build();
        task.setId(taskId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private TaskAttachment buildAttachment(final UUID id, final Task task, final Employee uploader) {
        TaskAttachment att = TaskAttachment.builder()
                .task(task)
                .uploader(uploader)
                .originalName("requirements.pdf")
                .storedName("uuid.pdf")
                .mimeType("application/pdf")
                .sizeBytes(1024L)
                .storageKey("submissions/" + task.getId() + "/uuid.pdf")
                .build();
        att.setId(id);
        att.setCreatedAt(LocalDateTime.now());
        att.setUpdatedAt(LocalDateTime.now());
        return att;
    }

    @Nested
    @DisplayName("findByTaskId()")
    class FindByTaskId {

        @Test
        @DisplayName("manager can list attachments on any task")
        void managerCanList() {
            UUID taskId   = UUID.randomUUID();
            UUID uploaderId = UUID.randomUUID();
            Employee uploader = buildEmployee(uploaderId, "Manager");
            Task task = buildTask(taskId, buildEmployee(UUID.randomUUID(), "Employee"));
            TaskAttachment attachment = buildAttachment(UUID.randomUUID(), task, uploader);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(attachmentRepository.findByTaskIdOrderByCreatedAtAsc(taskId))
                    .thenReturn(List.of(attachment));

            List<TaskAttachmentResponse> result = attachmentService.findByTaskId(taskId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).originalName()).isEqualTo("requirements.pdf");
        }

        @Test
        @DisplayName("assigned employee can list attachments on their own task")
        void assignedEmployeeCanList() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane");
            Task task = buildTask(taskId, employee);
            TaskAttachment attachment = buildAttachment(UUID.randomUUID(), task, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(attachmentRepository.findByTaskIdOrderByCreatedAtAsc(taskId))
                    .thenReturn(List.of(attachment));

            List<TaskAttachmentResponse> result = attachmentService.findByTaskId(taskId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("non-assigned employee cannot list attachments (IDOR protection)")
        void nonAssignedEmployeeCannotList() {
            UUID taskId    = UUID.randomUUID();
            UUID assigneeId = UUID.randomUUID();
            UUID otherId   = UUID.randomUUID();
            Employee assignee = buildEmployee(assigneeId, "Jane");
            Employee other    = buildEmployee(otherId, "John");
            Task task = buildTask(taskId, assignee);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> attachmentService.findByTaskId(taskId))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("upload()")
    class Upload {

        @Test
        @DisplayName("manager can upload attachment and it is persisted")
        void managerCanUpload() throws Exception {
            UUID taskId    = UUID.randomUUID();
            UUID uploaderId = UUID.randomUUID();
            Employee uploader = buildEmployee(uploaderId, "Manager");
            Task task = buildTask(taskId, buildEmployee(UUID.randomUUID(), "Employee"));

            MockMultipartFile file = new MockMultipartFile(
                    "file", "requirements.pdf", "application/pdf", new byte[1024]);

            String storageKey = "submissions/" + taskId + "/uuid.pdf";
            TaskAttachment saved = buildAttachment(UUID.randomUUID(), task, uploader);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(uploader));
            when(fileStorageService.store(any(), any())).thenReturn(storageKey);
            when(attachmentRepository.save(any(TaskAttachment.class))).thenReturn(saved);

            TaskAttachmentResponse result = attachmentService.upload(taskId, file);

            assertThat(result).isNotNull();
            assertThat(result.originalName()).isEqualTo("requirements.pdf");
            verify(fileValidationService).validate(file);
            verify(attachmentRepository).save(any(TaskAttachment.class));
        }
    }
}
