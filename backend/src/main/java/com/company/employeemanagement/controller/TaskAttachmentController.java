package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.response.TaskAttachmentResponse;
import com.company.employeemanagement.entity.TaskAttachment;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskAttachmentRepository;
import com.company.employeemanagement.service.TaskAttachmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for task attachment operations.
 *
 * <p>Base path: {@code /api/tasks/{taskId}/attachments}
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/tasks/{taskId}/attachments")
@Tag(name = "Task Attachments", description = "Manager-uploaded files attached to a task")
@SecurityRequirement(name = "BearerAuth")
public class TaskAttachmentController {

    private final TaskAttachmentService attachmentService;
    private final TaskAttachmentRepository attachmentRepository;

    public TaskAttachmentController(final TaskAttachmentService attachmentService,
                                     final TaskAttachmentRepository attachmentRepository) {
        this.attachmentService = attachmentService;
        this.attachmentRepository = attachmentRepository;
    }

    /**
     * Lists all attachments for the given task.
     *
     * @param taskId UUID of the task
     * @return list of attachment metadata
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List task attachments",
               description = "Returns metadata for all attachments on the task. Employees restricted to own tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attachments returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TaskAttachmentResponse>> findByTaskId(
            @PathVariable final UUID taskId) {
        return ResponseEntity.ok(attachmentService.findByTaskId(taskId));
    }

    /**
     * Uploads a file attachment to the given task.
     *
     * @param taskId UUID of the task
     * @param file   the file to upload
     * @return {@code 201 Created} with the attachment metadata
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Upload task attachment",
               description = "Uploads a file as a task attachment. Requires MANAGER, HR, or ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attachment uploaded"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskAttachmentResponse> upload(
            @PathVariable final UUID taskId,
            @RequestParam("file") final MultipartFile file) {
        return ResponseEntity.status(201).body(attachmentService.upload(taskId, file));
    }

    /**
     * Downloads the file content of the given attachment.
     *
     * @param taskId       UUID of the task
     * @param attachmentId UUID of the attachment
     * @return the file as a downloadable response
     */
    @GetMapping(value = "/{attachmentId}/download")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Download task attachment",
               description = "Downloads the file. Employees restricted to own tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File content returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Attachment not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<org.springframework.core.io.InputStreamResource> download(
            @PathVariable final UUID taskId,
            @PathVariable final UUID attachmentId) throws IOException {

        TaskAttachmentResponse meta = attachmentService.findById(taskId, attachmentId);
        InputStream stream = attachmentService.download(taskId, attachmentId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + meta.originalName() + "\"")
                .header(HttpHeaders.CONTENT_TYPE, meta.mimeType())
                .header(HttpHeaders.CONTENT_LENGTH, String.valueOf(meta.sizeBytes()))
                .body(new org.springframework.core.io.InputStreamResource(stream));
    }

    /**
     * Deletes the given attachment.
     *
     * @param taskId       UUID of the task
     * @param attachmentId UUID of the attachment
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{attachmentId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Delete task attachment",
               description = "Deletes an attachment. Requires MANAGER, HR, or ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Attachment deleted"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Attachment not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @PathVariable final UUID taskId,
            @PathVariable final UUID attachmentId) {
        attachmentService.delete(taskId, attachmentId);
        return ResponseEntity.noContent().build();
    }
}
