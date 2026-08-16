package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskCommentRequest;
import com.company.employeemanagement.dto.response.TaskCommentResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskComment;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.repository.TaskCommentRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.TaskCommentServiceImpl;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TaskCommentServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("TaskCommentServiceImpl")
class TaskCommentServiceTest {

    @Mock private TaskCommentRepository commentRepository;
    @Mock private TaskRepository        taskRepository;
    @Mock private SecurityUtils         securityUtils;
    @Mock private NotificationService   notificationService;

    private TaskCommentServiceImpl commentService;

    @BeforeEach
    void setUp() {
        commentService = new TaskCommentServiceImpl(
                commentRepository, taskRepository, securityUtils, notificationService);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Employee buildEmployee(final UUID id, final String firstName) {
        User user = User.builder()
                .firstName(firstName)
                .lastName("Doe")
                .email(firstName.toLowerCase() + "@example.com")
                .passwordHash("hash")
                .build();
        user.setId(UUID.randomUUID());

        Department dept = new Department();
        dept.setName("Engineering");
        dept.setCode("ENG");

        Employee emp = Employee.builder()
                .employeeCode("EMP-" + id.toString().substring(0, 4))
                .department(dept)
                .jobTitle("Engineer")
                .dateOfJoining(LocalDate.of(2024, 1, 1))
                .salary(BigDecimal.valueOf(70000))
                .user(user)
                .build();
        emp.setId(id);
        return emp;
    }

    private Task buildTask(final UUID taskId, final Employee assignee, final Employee creator) {
        Task task = Task.builder()
                .title("Test Task")
                .priority(TaskPriority.MEDIUM)
                .status(TaskStatus.ASSIGNED)
                .dueDate(LocalDate.now().plusDays(7))
                .assignedEmployee(assignee)
                .createdByEmployee(creator)
                .build();
        task.setId(taskId);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        return task;
    }

    private TaskComment buildComment(final UUID commentId, final Task task, final Employee author) {
        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(author)
                .content("A test comment")
                .build();
        comment.setId(commentId);
        comment.setCreatedAt(LocalDateTime.now());
        comment.setUpdatedAt(LocalDateTime.now());
        return comment;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // findByTaskId()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("findByTaskId()")
    class FindByTaskId {

        @Test
        @DisplayName("manager can view comments on any task")
        void managerCanViewComments() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee assignee = buildEmployee(empId, "Jane");
            Task task = buildTask(taskId, assignee, null);
            TaskComment comment = buildComment(UUID.randomUUID(), task, assignee);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId))
                    .thenReturn(List.of(comment));

            List<TaskCommentResponse> result = commentService.findByTaskId(taskId);

            assertThat(result).hasSize(1);
            assertThat(result.get(0).content()).isEqualTo("A test comment");
        }

        @Test
        @DisplayName("employee can view comments on their own task")
        void employeeCanViewOwnTaskComments() {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            Employee employee = buildEmployee(empId, "Jane");
            Task task = buildTask(taskId, employee, null);
            TaskComment comment = buildComment(UUID.randomUUID(), task, employee);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(employee));
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(taskId))
                    .thenReturn(List.of(comment));

            List<TaskCommentResponse> result = commentService.findByTaskId(taskId);

            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("employee cannot view comments on another employee's task (IDOR)")
        void employeeCannotViewOtherTaskComments() {
            UUID taskId  = UUID.randomUUID();
            UUID emp1Id  = UUID.randomUUID();
            UUID emp2Id  = UUID.randomUUID();
            Employee assignee    = buildEmployee(emp1Id, "Jane");
            Employee otherEmployee = buildEmployee(emp2Id, "John");
            Task task = buildTask(taskId, assignee, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(otherEmployee));

            assertThatThrownBy(() -> commentService.findByTaskId(taskId))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("assigned to you");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // create()
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("create()")
    class Create {

        @Test
        @DisplayName("employee can post a comment on their own task")
        void employeeCanCommentOnOwnTask() {
            UUID taskId   = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            Employee author  = buildEmployee(authorId, "Jane");
            Employee creator = buildEmployee(creatorId, "Boss");
            Task task = buildTask(taskId, author, creator);
            TaskComment saved = buildComment(UUID.randomUUID(), task, author);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(author));
            when(commentRepository.save(any(TaskComment.class))).thenReturn(saved);

            CreateTaskCommentRequest request = new CreateTaskCommentRequest("Great progress!");

            TaskCommentResponse result = commentService.create(taskId, request);

            assertThat(result).isNotNull();
            assertThat(result.content()).isEqualTo("A test comment");
            verify(commentRepository).save(any(TaskComment.class));
            // Notification should be sent to the creator (not the author)
            verify(notificationService).createNotification(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("employee cannot post a comment on another employee's task")
        void employeeCannotCommentOnOtherTask() {
            UUID taskId    = UUID.randomUUID();
            UUID assigneeId = UUID.randomUUID();
            UUID otherId   = UUID.randomUUID();
            Employee assignee   = buildEmployee(assigneeId, "Jane");
            Employee other      = buildEmployee(otherId, "John");
            Task task = buildTask(taskId, assignee, null);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.isPrivileged()).thenReturn(false);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(other));

            assertThatThrownBy(() -> commentService.create(taskId, new CreateTaskCommentRequest("Hi")))
                    .isInstanceOf(AccessDeniedException.class)
                    .hasMessageContaining("assigned to you");

            verify(commentRepository, never()).save(any());
        }

        @Test
        @DisplayName("author's own comment does not trigger a notification back to themselves")
        void authorDoesNotReceiveOwnNotification() {
            UUID taskId    = UUID.randomUUID();
            UUID authorId  = UUID.randomUUID();
            // Task where the author IS the creator and the ONLY party (no other party to notify)
            Employee author = buildEmployee(authorId, "Solo");
            Task task = buildTask(taskId, author, author); // same employee is both assignee and creator
            TaskComment saved = buildComment(UUID.randomUUID(), task, author);

            when(taskRepository.findByIdWithAssociations(taskId)).thenReturn(Optional.of(task));
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false); // privileged
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(author));
            when(commentRepository.save(any(TaskComment.class))).thenReturn(saved);

            commentService.create(taskId, new CreateTaskCommentRequest("Self-comment"));

            // No notification should be sent (the only party is the author themselves)
            verify(notificationService, never()).createNotification(any(), any(), any(), any(), any());
        }
    }
}
