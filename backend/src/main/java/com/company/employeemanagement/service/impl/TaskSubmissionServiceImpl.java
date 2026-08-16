package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateTaskSubmissionRequest;
import com.company.employeemanagement.dto.request.RequestChangesRequest;
import com.company.employeemanagement.dto.request.UpdateTaskSubmissionRequest;
import com.company.employeemanagement.dto.response.TaskSubmissionResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskActivity;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.SubmissionStatus;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskActivityRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.FileStorageService;
import com.company.employeemanagement.service.FileValidationService;
import com.company.employeemanagement.service.NotificationService;
import com.company.employeemanagement.service.TaskSubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TaskSubmissionService}.
 *
 * <p>Authorization model:
 * <ul>
 *   <li>EMPLOYEE — can submit their own IN_PROGRESS task, view own submissions,
 *       and resubmit after CHANGES_REQUESTED. Cannot approve or request changes.</li>
 *   <li>MANAGER / HR / ADMIN — can view any submission, approve, and request changes.</li>
 * </ul>
 *
 * <p>Phase 6B.1 additions:
 * <ul>
 *   <li>Optional file attachment on create and resubmit.</li>
 *   <li>Attachment download with per-role authorization.</li>
 *   <li>Attachment deletion when a replacement is uploaded on resubmit.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class TaskSubmissionServiceImpl implements TaskSubmissionService {

    private static final Logger log = LoggerFactory.getLogger(TaskSubmissionServiceImpl.class);

    private final TaskSubmissionRepository submissionRepository;
    private final TaskRepository taskRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;
    private final FileStorageService fileStorageService;
    private final FileValidationService fileValidationService;

    /**
     * Constructs the service with required dependencies.
     */
    public TaskSubmissionServiceImpl(
            final TaskSubmissionRepository submissionRepository,
            final TaskRepository taskRepository,
            final TaskActivityRepository taskActivityRepository,
            final SecurityUtils securityUtils,
            final NotificationService notificationService,
            final FileStorageService fileStorageService,
            final FileValidationService fileValidationService) {
        this.submissionRepository = submissionRepository;
        this.taskRepository = taskRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
        this.fileStorageService = fileStorageService;
        this.fileValidationService = fileValidationService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskSubmissionResponse createSubmission(final UUID taskId,
                                                    final CreateTaskSubmissionRequest request,
                                                    final MultipartFile file) {
        Task task = requireTask(taskId);
        Employee currentEmployee = requireCurrentEmployee();

        // Only the assigned employee can submit
        requireAssignedToEmployee(task, currentEmployee);

        // Task must be IN_PROGRESS to submit
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Task must be IN_PROGRESS to submit for review. Current status: " + task.getStatus());
        }

        // Validate file if provided
        boolean hasFile = isPresent(file);
        if (hasFile) {
            fileValidationService.validate(file);
        }

        // Transition task to SUBMITTED
        TaskStatus prevStatus = task.getStatus();
        task.setStatus(TaskStatus.SUBMITTED);
        taskRepository.save(task);

        TaskSubmission submission = TaskSubmission.builder()
                .task(task)
                .submittedBy(currentEmployee)
                .submissionNotes(request.submissionNotes())
                .workCompleted(request.workCompleted())
                .additionalComments(request.additionalComments())
                .submittedAt(LocalDateTime.now())
                .reviewStatus(SubmissionStatus.PENDING_REVIEW)
                .build();

        TaskSubmission saved = submissionRepository.save(submission);

        // Store attachment after saving (we need the submission ID for the storage key)
        if (hasFile) {
            storeAttachment(saved, file);
            saved = submissionRepository.save(saved);
        }

        log.info("TaskSubmission.create: task={} submission={} by={} attachment={}",
                taskId, saved.getId(), currentEmployee.getId(), hasFile);

        // Record activity
        recordActivity(task, currentEmployee, "TASK_SUBMITTED",
                employeeName(currentEmployee) + " submitted task for review",
                prevStatus.name(), TaskStatus.SUBMITTED.name());

        // Notify the task creator/manager
        if (task.getCreatedByEmployee() != null) {
            notificationService.createNotification(
                    task.getCreatedByEmployee(),
                    NotificationType.TASK_SUBMITTED,
                    "Task Submitted for Review",
                    "Task submitted for review: \"" + task.getTitle() + "\" by "
                            + employeeName(currentEmployee),
                    task.getId()
            );
        }

        // Re-fetch with full associations for the response
        return toResponse(submissionRepository.findByIdWithAssociations(saved.getId())
                .orElse(saved));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<TaskSubmissionResponse> getSubmissionsForTask(final UUID taskId) {
        Task task = requireTask(taskId);

        // Employee: scope to own task only
        if (isEmployeeOnly()) {
            Employee currentEmployee = requireCurrentEmployee();
            requireAssignedToEmployee(task, currentEmployee);
        }

        return submissionRepository.findAllByTaskIdOrderBySubmittedAtDesc(taskId)
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public TaskSubmissionResponse getLatestSubmission(final UUID taskId) {
        Task task = requireTask(taskId);

        if (isEmployeeOnly()) {
            Employee currentEmployee = requireCurrentEmployee();
            requireAssignedToEmployee(task, currentEmployee);
        }

        TaskSubmission submission = submissionRepository.findLatestByTaskId(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No submission found for task " + taskId));
        return toResponse(submission);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskSubmissionResponse resubmit(final UUID submissionId,
                                            final UpdateTaskSubmissionRequest request,
                                            final MultipartFile file) {
        TaskSubmission submission = requireSubmissionWithAssociations(submissionId);
        Employee currentEmployee = requireCurrentEmployee();

        // Only the original submitter can resubmit
        if (!currentEmployee.getId().equals(submission.getSubmittedBy().getId())) {
            throw new AccessDeniedException(
                    "You may only update your own submissions.");
        }

        // Must be in CHANGES_REQUESTED state
        if (submission.getReviewStatus() != SubmissionStatus.CHANGES_REQUESTED) {
            throw new IllegalStateException(
                    "You can only resubmit when changes have been requested. "
                    + "Current status: " + submission.getReviewStatus());
        }

        Task task = submission.getTask();
        // Task must be IN_PROGRESS (reverted by manager after requesting changes)
        if (task.getStatus() != TaskStatus.IN_PROGRESS) {
            throw new IllegalStateException(
                    "Task must be IN_PROGRESS to resubmit. Current status: " + task.getStatus());
        }

        // Validate replacement file if provided
        boolean hasNewFile = isPresent(file);
        if (hasNewFile) {
            fileValidationService.validate(file);
        }

        // Update submission text fields
        submission.setSubmissionNotes(request.submissionNotes());
        submission.setWorkCompleted(request.workCompleted());
        submission.setAdditionalComments(request.additionalComments());
        submission.setSubmittedAt(LocalDateTime.now());
        submission.setReviewStatus(SubmissionStatus.PENDING_REVIEW);
        submission.setReviewComment(null);
        submission.setReviewedBy(null);
        submission.setReviewedAt(null);

        // Handle file replacement
        if (hasNewFile) {
            // Delete previous attachment if exists
            deleteAttachmentIfPresent(submission);
            // Store new attachment
            storeAttachment(submission, file);
        }

        // Transition task back to SUBMITTED
        TaskStatus prevStatus = task.getStatus();
        task.setStatus(TaskStatus.SUBMITTED);
        taskRepository.save(task);

        TaskSubmission updated = submissionRepository.save(submission);
        log.info("TaskSubmission.resubmit: submission={} by={} newAttachment={}",
                submissionId, currentEmployee.getId(), hasNewFile);

        // Record activity
        recordActivity(task, currentEmployee, "TASK_SUBMITTED",
                employeeName(currentEmployee) + " resubmitted task for review",
                prevStatus.name(), TaskStatus.SUBMITTED.name());

        // Notify manager
        if (task.getCreatedByEmployee() != null) {
            notificationService.createNotification(
                    task.getCreatedByEmployee(),
                    NotificationType.TASK_SUBMITTED,
                    "Task Resubmitted for Review",
                    "Task resubmitted for review: \"" + task.getTitle() + "\" by "
                            + employeeName(currentEmployee),
                    task.getId()
            );
        }

        return toResponse(submissionRepository.findByIdWithAssociations(updated.getId())
                .orElse(updated));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskSubmissionResponse approve(final UUID submissionId) {
        requirePrivileged();
        TaskSubmission submission = requireSubmissionWithAssociations(submissionId);

        // Must be in PENDING_REVIEW to approve
        if (submission.getReviewStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Only PENDING_REVIEW submissions can be approved. "
                    + "Current status: " + submission.getReviewStatus());
        }

        Employee reviewer = requireCurrentEmployee();
        Task task = submission.getTask();

        // Task must be SUBMITTED
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Task must be SUBMITTED to approve. Current status: " + task.getStatus());
        }

        // Update submission
        submission.setReviewStatus(SubmissionStatus.APPROVED);
        submission.setReviewedBy(reviewer);
        submission.setReviewedAt(LocalDateTime.now());

        // Transition task to COMPLETED
        TaskStatus prevStatus = task.getStatus();
        task.setStatus(TaskStatus.COMPLETED);
        taskRepository.save(task);

        TaskSubmission updated = submissionRepository.save(submission);
        log.info("TaskSubmission.approve: submission={} by={}", submissionId, reviewer.getId());

        // Record activity
        recordActivity(task, reviewer, "TASK_APPROVED",
                employeeName(reviewer) + " approved the task submission",
                prevStatus.name(), TaskStatus.COMPLETED.name());

        // Notify the assigned employee
        if (task.getAssignedEmployee() != null) {
            notificationService.createNotification(
                    task.getAssignedEmployee(),
                    NotificationType.TASK_APPROVED,
                    "Task Completed",
                    "Task completed: \"" + task.getTitle() + "\"\n"
                            + "Approved by " + employeeName(reviewer),
                    task.getId()
            );
        }

        return toResponse(submissionRepository.findByIdWithAssociations(updated.getId())
                .orElse(updated));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskSubmissionResponse requestChanges(final UUID submissionId,
                                                  final RequestChangesRequest request) {
        requirePrivileged();
        TaskSubmission submission = requireSubmissionWithAssociations(submissionId);

        // Must be in PENDING_REVIEW to request changes
        if (submission.getReviewStatus() != SubmissionStatus.PENDING_REVIEW) {
            throw new IllegalStateException(
                    "Only PENDING_REVIEW submissions can have changes requested. "
                    + "Current status: " + submission.getReviewStatus());
        }

        Employee reviewer = requireCurrentEmployee();
        Task task = submission.getTask();

        // Task must be SUBMITTED
        if (task.getStatus() != TaskStatus.SUBMITTED) {
            throw new IllegalStateException(
                    "Task must be SUBMITTED to request changes. Current status: " + task.getStatus());
        }

        // Update submission
        submission.setReviewStatus(SubmissionStatus.CHANGES_REQUESTED);
        submission.setReviewComment(request.reviewComment());
        submission.setReviewedBy(reviewer);
        submission.setReviewedAt(LocalDateTime.now());

        // Revert task to IN_PROGRESS
        TaskStatus prevStatus = task.getStatus();
        task.setStatus(TaskStatus.IN_PROGRESS);
        taskRepository.save(task);

        TaskSubmission updated = submissionRepository.save(submission);
        log.info("TaskSubmission.requestChanges: submission={} by={}", submissionId, reviewer.getId());

        // Record activity
        recordActivity(task, reviewer, "TASK_CHANGES_REQUESTED",
                employeeName(reviewer) + " requested changes: " + request.reviewComment(),
                prevStatus.name(), TaskStatus.IN_PROGRESS.name());

        // Notify the assigned employee
        if (task.getAssignedEmployee() != null) {
            notificationService.createNotification(
                    task.getAssignedEmployee(),
                    NotificationType.TASK_CHANGES_REQUESTED,
                    "Changes Requested",
                    "Changes requested for: \"" + task.getTitle() + "\"\n"
                            + "Comment from " + employeeName(reviewer) + ": " + request.reviewComment(),
                    task.getId()
            );
        }

        return toResponse(submissionRepository.findByIdWithAssociations(updated.getId())
                .orElse(updated));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public AttachmentDownload downloadAttachment(final UUID submissionId) {
        TaskSubmission submission = requireSubmissionWithAssociations(submissionId);

        // Authorization: employee can only download their own attachment
        if (isEmployeeOnly()) {
            Employee currentEmployee = requireCurrentEmployee();
            if (!currentEmployee.getId().equals(submission.getSubmittedBy().getId())) {
                throw new AccessDeniedException(
                        "You may only download attachments from your own submissions.");
            }
        }

        if (!submission.hasAttachment()) {
            throw new ResourceNotFoundException(
                    "Submission " + submissionId + " has no file attachment.");
        }

        try {
            java.io.InputStream stream = fileStorageService.openForRead(
                    submission.getAttachmentStorageKey());
            return new AttachmentDownload(
                    stream,
                    submission.getAttachmentMimeType() != null
                            ? submission.getAttachmentMimeType()
                            : "application/octet-stream",
                    submission.getAttachmentOriginalName() != null
                            ? submission.getAttachmentOriginalName()
                            : "attachment",
                    submission.getAttachmentSizeBytes() != null
                            ? submission.getAttachmentSizeBytes()
                            : -1L
            );
        } catch (IOException e) {
            log.error("TaskSubmission.downloadAttachment: failed to read key={}: {}",
                    submission.getAttachmentStorageKey(), e.getMessage());
            throw new ResourceNotFoundException(
                    "Attachment file for submission " + submissionId + " could not be read.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Task requireTask(final UUID taskId) {
        return taskRepository.findByIdWithAssociations(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));
    }

    private TaskSubmission requireSubmissionWithAssociations(final UUID submissionId) {
        return submissionRepository.findByIdWithAssociations(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException("TaskSubmission", submissionId));
    }

    private Employee requireCurrentEmployee() {
        return securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));
    }

    private void requirePrivileged() {
        if (!securityUtils.isPrivileged()) {
            throw new AccessDeniedException(
                    "Only managers, HR, or admins can perform this action.");
        }
    }

    private boolean isEmployeeOnly() {
        return securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged();
    }

    private void requireAssignedToEmployee(final Task task, final Employee employee) {
        if (task.getAssignedEmployee() == null
                || !employee.getId().equals(task.getAssignedEmployee().getId())) {
            throw new AccessDeniedException(
                    "You may only submit tasks that are assigned to you.");
        }
    }

    /**
     * Returns {@code true} when the multipart file is non-null and non-empty.
     */
    private static boolean isPresent(final MultipartFile file) {
        return file != null && !file.isEmpty();
    }

    /**
     * Stores the file and writes attachment metadata onto the submission entity.
     * Callers must save the submission after calling this method.
     */
    private void storeAttachment(final TaskSubmission submission, final MultipartFile file) {
        try {
            String storageKey = fileStorageService.store(file, submission.getId());
            String safeName = sanitiseFilename(file.getOriginalFilename());
            submission.setAttachmentStorageKey(storageKey);
            submission.setAttachmentStoredName(extractFilename(storageKey));
            submission.setAttachmentOriginalName(safeName);
            submission.setAttachmentMimeType(
                    file.getContentType() != null ? file.getContentType().split(";")[0].trim() : null);
            submission.setAttachmentSizeBytes(file.getSize());
            submission.setAttachmentUploadedAt(LocalDateTime.now());
        } catch (IOException e) {
            log.error("TaskSubmission.storeAttachment: failed for submission={}: {}",
                    submission.getId(), e.getMessage());
            throw new IllegalStateException("Failed to store attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes the stored attachment file (if any) and clears the metadata fields.
     */
    private void deleteAttachmentIfPresent(final TaskSubmission submission) {
        if (submission.hasAttachment()) {
            fileStorageService.delete(submission.getAttachmentStorageKey());
            submission.setAttachmentStorageKey(null);
            submission.setAttachmentStoredName(null);
            submission.setAttachmentOriginalName(null);
            submission.setAttachmentMimeType(null);
            submission.setAttachmentSizeBytes(null);
            submission.setAttachmentUploadedAt(null);
        }
    }

    /**
     * Returns only the basename of a storage key (everything after the last {@code /}).
     */
    private static String extractFilename(final String storageKey) {
        if (storageKey == null) return null;
        int slash = storageKey.lastIndexOf('/');
        return slash >= 0 ? storageKey.substring(slash + 1) : storageKey;
    }

    /**
     * Returns the basename of the supplied filename, stripping any directory components
     * to prevent path traversal from being accidentally persisted into the database.
     */
    private static String sanitiseFilename(final String filename) {
        if (filename == null) return "attachment";
        String base = filename.replaceAll("[\\\\/:*?\"<>|]", "_");
        // Take only the final segment if the browser sent a path
        int slash = base.lastIndexOf('/');
        base = slash >= 0 ? base.substring(slash + 1) : base;
        return base.isBlank() ? "attachment" : base;
    }

    private void recordActivity(final Task task,
                                  final Employee actor,
                                  final String eventType,
                                  final String description,
                                  final String fromStatus,
                                  final String toStatus) {
        try {
            TaskActivity activity = TaskActivity.builder()
                    .task(task)
                    .actor(actor)
                    .eventType(eventType)
                    .description(description)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .build();
            taskActivityRepository.save(activity);
        } catch (Exception e) {
            log.warn("TaskSubmission.recordActivity: failed for task={} event={}: {}",
                    task.getId(), eventType, e.getMessage());
        }
    }

    private String employeeName(final Employee employee) {
        if (employee == null) return "Unknown";
        User user = employee.getUser();
        if (user != null) {
            String first = user.getFirstName();
            String last  = user.getLastName();
            if (first != null || last != null) {
                return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            }
        }
        if (employee.getFirstName() != null || employee.getLastName() != null) {
            return ((employee.getFirstName() != null ? employee.getFirstName() : "")
                    + " " + (employee.getLastName() != null ? employee.getLastName() : "")).trim();
        }
        return employee.getEmployeeCode();
    }

    /**
     * Converts a {@link TaskSubmission} entity to a {@link TaskSubmissionResponse} DTO.
     */
    private TaskSubmissionResponse toResponse(final TaskSubmission s) {
        Employee submittedBy = s.getSubmittedBy();
        Employee reviewedBy  = s.getReviewedBy();
        Task     task        = s.getTask();

        return new TaskSubmissionResponse(
                s.getId(),
                task != null ? task.getId() : null,
                task != null ? task.getTitle() : null,
                submittedBy != null ? submittedBy.getId() : null,
                submittedBy != null ? employeeName(submittedBy) : null,
                s.getSubmissionNotes(),
                s.getWorkCompleted(),
                s.getAdditionalComments(),
                s.getSubmittedAt(),
                s.getReviewStatus(),
                s.getReviewComment(),
                reviewedBy != null ? reviewedBy.getId() : null,
                reviewedBy != null ? employeeName(reviewedBy) : null,
                s.getReviewedAt(),
                s.getCreatedAt(),
                s.getUpdatedAt(),
                // Attachment fields
                s.getAttachmentOriginalName(),
                s.getAttachmentMimeType(),
                s.getAttachmentSizeBytes(),
                s.getAttachmentUploadedAt(),
                s.hasAttachment()
        );
    }
}
