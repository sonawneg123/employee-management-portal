package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateTaskCommentRequest;
import com.company.employeemanagement.dto.response.TaskCommentResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskComment;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskCommentRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
import com.company.employeemanagement.service.TaskCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TaskCommentService}.
 *
 * @author Employee Management Portal Team
 */
@Service
public class TaskCommentServiceImpl implements TaskCommentService {

    private static final Logger log = LoggerFactory.getLogger(TaskCommentServiceImpl.class);

    private final TaskCommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public TaskCommentServiceImpl(final TaskCommentRepository commentRepository,
                                   final TaskRepository taskRepository,
                                   final SecurityUtils securityUtils,
                                   final NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.taskRepository = taskRepository;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<TaskCommentResponse> findByTaskId(final UUID taskId) {
        Task task = loadTaskWithAccessCheck(taskId);
        return commentRepository.findByTaskIdOrderByCreatedAtAsc(task.getId())
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskCommentResponse create(final UUID taskId, final CreateTaskCommentRequest request) {
        Task task = loadTaskWithAccessCheck(taskId);

        Employee author = securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));

        TaskComment comment = TaskComment.builder()
                .task(task)
                .author(author)
                .content(request.content())
                .build();

        TaskComment saved = commentRepository.save(comment);
        log.info("TaskComment.create: taskId={} authorId={}", taskId, author.getId());

        // Notify the other party: if author is the assignee → notify creator; otherwise notify assignee.
        sendCommentNotification(task, author, saved);

        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Loads the task and verifies the current principal may access it.
     * Employees can only access tasks assigned to them.
     */
    private Task loadTaskWithAccessCheck(final UUID taskId) {
        Task task = taskRepository.findByIdWithAssociations(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        if (isEmployeeOnly()) {
            Employee currentEmployee = securityUtils.getCurrentEmployee()
                    .orElseThrow(() -> new AccessDeniedException(
                            "No employee record is linked to your account."));
            if (task.getAssignedEmployee() == null
                    || !currentEmployee.getId().equals(task.getAssignedEmployee().getId())) {
                throw new AccessDeniedException("You may only access tasks assigned to you.");
            }
        }
        return task;
    }

    private boolean isEmployeeOnly() {
        return securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged();
    }

    /**
     * Sends a comment notification to the other party on the task.
     * Never notifies the author about their own comment.
     */
    private void sendCommentNotification(final Task task,
                                          final Employee author,
                                          final TaskComment comment) {
        try {
            Employee assignee = task.getAssignedEmployee();
            Employee creator  = task.getCreatedByEmployee();

            Employee recipient = null;
            if (assignee != null && !assignee.getId().equals(author.getId())) {
                recipient = assignee;
            } else if (creator != null && !creator.getId().equals(author.getId())) {
                recipient = creator;
            }

            if (recipient == null) return;

            String authorName = employeeName(author);
            notificationService.createNotification(
                    recipient,
                    NotificationType.TASK_COMMENT,
                    "New Comment on Task",
                    authorName + " commented on \"" + task.getTitle() + "\": "
                            + truncate(comment.getContent(), 100),
                    task.getId()
            );
        } catch (Exception e) {
            log.warn("TaskComment: failed to send notification for task={}: {}",
                    task.getId(), e.getMessage());
        }
    }

    private TaskCommentResponse toResponse(final TaskComment c) {
        return new TaskCommentResponse(
                c.getId(),
                c.getTask().getId(),
                c.getAuthor().getId(),
                employeeName(c.getAuthor()),
                c.getContent(),
                c.isEdited(),
                c.getCreatedAt(),
                c.getUpdatedAt()
        );
    }

    private String employeeName(final Employee e) {
        if (e == null) return "Unknown";
        User u = e.getUser();
        if (u != null) return u.getFirstName() + " " + u.getLastName();
        if (e.getFirstName() != null || e.getLastName() != null) {
            return ((e.getFirstName() != null ? e.getFirstName() : "")
                    + " " + (e.getLastName() != null ? e.getLastName() : "")).trim();
        }
        return e.getEmployeeCode();
    }

    private String truncate(final String text, final int maxLen) {
        if (text == null) return "";
        return text.length() <= maxLen ? text : text.substring(0, maxLen) + "…";
    }
}
