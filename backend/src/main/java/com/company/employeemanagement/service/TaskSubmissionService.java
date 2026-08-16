package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskSubmissionRequest;
import com.company.employeemanagement.dto.request.RequestChangesRequest;
import com.company.employeemanagement.dto.request.UpdateTaskSubmissionRequest;
import com.company.employeemanagement.dto.response.TaskSubmissionResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for task submission and manager review operations.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>EMPLOYEE — can submit their own task (IN_PROGRESS only), view their own submissions,
 *       and resubmit after CHANGES_REQUESTED.</li>
 *   <li>MANAGER / HR / ADMIN — can view any submission for tasks they manage, approve
 *       submissions, and request changes.</li>
 *   <li>EMPLOYEE cannot approve or request changes on any submission.</li>
 *   <li>MANAGER cannot submit on behalf of an employee.</li>
 * </ul>
 *
 * <p>Phase 6B.1: {@code file} parameters are optional. Passing {@code null} or an empty
 * multipart part results in a text-only submission, which remains fully supported.
 *
 * @author Employee Management Portal Team
 */
public interface TaskSubmissionService {

    /**
     * Creates a new submission for the given task with an optional file attachment.
     * The task must be in {@link com.company.employeemanagement.entity.enums.TaskStatus#IN_PROGRESS}
     * and must be assigned to the currently authenticated employee.
     * Transitions the task to {@link com.company.employeemanagement.entity.enums.TaskStatus#SUBMITTED}.
     *
     * @param taskId  the UUID of the task to submit
     * @param request the submission payload
     * @param file    optional file attachment (may be null)
     * @return the created {@link TaskSubmissionResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if task not found
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if caller is not the assignee
     * @throws IllegalStateException if the task is not in IN_PROGRESS state
     * @throws IllegalArgumentException if the file fails validation
     */
    TaskSubmissionResponse createSubmission(UUID taskId, CreateTaskSubmissionRequest request,
                                            MultipartFile file);

    /**
     * Returns all submissions for the given task.
     * Employees may only view submissions for their own assigned tasks.
     * Managers/HR/Admin may view all.
     *
     * @param taskId the UUID of the task
     * @return list of submissions ordered by submission time descending
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if task not found
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if access is denied
     */
    List<TaskSubmissionResponse> getSubmissionsForTask(UUID taskId);

    /**
     * Returns the latest submission for the given task.
     *
     * @param taskId the UUID of the task
     * @return the latest {@link TaskSubmissionResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if no submission exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if access is denied
     */
    TaskSubmissionResponse getLatestSubmission(UUID taskId);

    /**
     * Updates an existing submission (employee resubmits after changes requested),
     * optionally replacing the previous file attachment.
     * Only allowed when the submission's review status is
     * {@link com.company.employeemanagement.entity.enums.SubmissionStatus#CHANGES_REQUESTED}.
     * Transitions the task back to SUBMITTED and resets review status to PENDING_REVIEW.
     *
     * @param submissionId the UUID of the submission to update
     * @param request      the updated submission payload
     * @param file         optional replacement file (null = keep existing attachment if any)
     * @return the updated {@link TaskSubmissionResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if submission not found
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if caller is not the original submitter
     * @throws IllegalStateException if the submission is not in CHANGES_REQUESTED state
     * @throws IllegalArgumentException if the file fails validation
     */
    TaskSubmissionResponse resubmit(UUID submissionId, UpdateTaskSubmissionRequest request,
                                    MultipartFile file);

    /**
     * Approves a task submission (manager operation).
     * Transitions the task from SUBMITTED to COMPLETED.
     * Sets submission review status to APPROVED.
     * Creates an activity event and notifies the employee.
     *
     * @param submissionId the UUID of the submission to approve
     * @return the updated {@link TaskSubmissionResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if submission not found
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if caller is not privileged
     * @throws IllegalStateException if the submission is not in PENDING_REVIEW state
     */
    TaskSubmissionResponse approve(UUID submissionId);

    /**
     * Requests changes on a task submission (manager operation).
     * Transitions the task from SUBMITTED back to IN_PROGRESS.
     * Sets submission review status to CHANGES_REQUESTED.
     * Creates an activity event and notifies the employee.
     *
     * @param submissionId the UUID of the submission
     * @param request      the request containing the review comment
     * @return the updated {@link TaskSubmissionResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if submission not found
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if caller is not privileged
     * @throws IllegalStateException if the submission is not in PENDING_REVIEW state
     */
    TaskSubmissionResponse requestChanges(UUID submissionId, RequestChangesRequest request);

    /**
     * Downloads the attachment for the given submission.
     *
     * <p>Authorization:
     * <ul>
     *   <li>EMPLOYEE — may only download their own submission attachment.</li>
     *   <li>MANAGER / HR / ADMIN — may download any attachment.</li>
     * </ul>
     *
     * @param submissionId the UUID of the submission
     * @return {@link AttachmentDownload} containing the stream and metadata
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException if submission or attachment not found
     * @throws com.company.employeemanagement.exception.AccessDeniedException     if access is denied
     */
    AttachmentDownload downloadAttachment(UUID submissionId);

    /**
     * Carries the data needed to stream an attachment back to the client.
     *
     * @param inputStream        file bytes
     * @param contentType        MIME type
     * @param originalFilename   filename for Content-Disposition
     * @param sizeBytes          file size for Content-Length header
     */
    record AttachmentDownload(
            java.io.InputStream inputStream,
            String contentType,
            String originalFilename,
            long sizeBytes
    ) {}
}
