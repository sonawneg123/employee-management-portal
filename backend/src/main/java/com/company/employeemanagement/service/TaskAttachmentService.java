package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.TaskAttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * Service contract for task attachment management.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>Upload / delete — MANAGER, HR, ADMIN only.</li>
 *   <li>Download / list — MANAGER, HR, ADMIN, or the task's assigned employee.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public interface TaskAttachmentService {

    /**
     * Uploads and stores an attachment for the given task.
     *
     * @param taskId the UUID of the task
     * @param file   the multipart file to upload
     * @return the saved {@link TaskAttachmentResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the task does not exist
     */
    TaskAttachmentResponse upload(UUID taskId, MultipartFile file);

    /**
     * Returns all attachments for the given task.
     *
     * @param taskId the UUID of the task
     * @return list of attachment metadata
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the task does not exist
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if an employee tries to access attachments on another employee's task
     */
    List<TaskAttachmentResponse> findByTaskId(UUID taskId);

    /**
     * Opens an {@link InputStream} for the given attachment.
     *
     * @param taskId       the UUID of the task (for IDOR verification)
     * @param attachmentId the UUID of the attachment
     * @return the attachment's content stream (caller must close)
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the attachment does not exist
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if the caller is not permitted to download this attachment
     * @throws java.io.IOException if the underlying file cannot be read
     */
    InputStream download(UUID taskId, UUID attachmentId) throws java.io.IOException;

    /**
     * Returns metadata for a single attachment.
     *
     * @param taskId       the UUID of the task
     * @param attachmentId the UUID of the attachment
     * @return attachment metadata
     */
    TaskAttachmentResponse findById(UUID taskId, UUID attachmentId);

    /**
     * Deletes the given attachment and removes the stored file.
     *
     * @param taskId       the UUID of the task (for IDOR verification)
     * @param attachmentId the UUID of the attachment
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the attachment does not exist
     */
    void delete(UUID taskId, UUID attachmentId);
}
