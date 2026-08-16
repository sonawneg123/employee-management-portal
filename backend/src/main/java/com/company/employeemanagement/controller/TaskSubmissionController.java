package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateTaskSubmissionRequest;
import com.company.employeemanagement.dto.request.RequestChangesRequest;
import com.company.employeemanagement.dto.request.UpdateTaskSubmissionRequest;
import com.company.employeemanagement.dto.response.TaskSubmissionResponse;
import com.company.employeemanagement.service.TaskSubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing task submission and manager review endpoints.
 *
 * <p>Base paths:
 * <ul>
 *   <li>{@code /tasks/{taskId}/submissions} — task-scoped operations (submit, list)</li>
 *   <li>{@code /task-submissions/{submissionId}} — submission-level operations
 *       (resubmit, approve, request-changes, download)</li>
 * </ul>
 *
 * <p>Phase 6B.1: create and resubmit endpoints now accept
 * {@code multipart/form-data} with an optional {@code file} part.
 * JSON-only requests (no file) continue to work by omitting the {@code file} part.
 *
 * @author Employee Management Portal Team
 */
@RestController
@Tag(name = "Task Submissions", description = "Employee submission and manager review of task work")
@SecurityRequirement(name = "BearerAuth")
public class TaskSubmissionController {

    private final TaskSubmissionService submissionService;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param submissionService the task submission service
     * @param objectMapper      Jackson mapper used to deserialise the JSON submission part
     */
    public TaskSubmissionController(final TaskSubmissionService submissionService,
                                    final ObjectMapper objectMapper) {
        this.submissionService = submissionService;
        this.objectMapper = objectMapper;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Task-scoped endpoints  /tasks/{taskId}/submissions
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Employee submits work for a task for manager review.
     *
     * <p>Accepts {@code multipart/form-data} with:
     * <ul>
     *   <li>{@code submission} — JSON part ({@link CreateTaskSubmissionRequest})</li>
     *   <li>{@code file} — optional file attachment (PDF, CSV, DOCX, TXT; max 10 MB)</li>
     * </ul>
     *
     * <p>For text-only submissions, omit the {@code file} part entirely.
     *
     * @param taskId         the UUID of the task
     * @param submissionJson the submission JSON as a string part
     * @param file           optional file attachment
     * @return the created submission
     */
    @PostMapping(value = "/tasks/{taskId}/submissions",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Submit task for review",
               description = "Employee submits completed work for manager review. "
                           + "Task must be IN_PROGRESS. "
                           + "Use multipart/form-data with a 'submission' JSON part "
                           + "and an optional 'file' part (PDF/CSV/DOCX/TXT, max 10 MB).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Submission created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskSubmissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or unsupported file type",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not the assigned employee",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Task is not IN_PROGRESS",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskSubmissionResponse> createSubmission(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId,
            @Parameter(description = "Submission data as a JSON string")
            @RequestPart("submission") final String submissionJson,
            @Parameter(description = "Optional file attachment (PDF, CSV, DOCX, TXT; max 10 MB)")
            @RequestPart(value = "file", required = false) final MultipartFile file)
            throws IOException {

        CreateTaskSubmissionRequest request = objectMapper.readValue(
                submissionJson, CreateTaskSubmissionRequest.class);

        // Manual bean validation (since @Valid cannot be applied to @RequestPart String)
        validateCreateRequest(request);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(submissionService.createSubmission(taskId, request, file));
    }

    /**
     * Returns all submissions for the given task.
     * Employees may only view submissions for tasks assigned to them.
     *
     * @param taskId the UUID of the task
     * @return list of submissions ordered by submission time descending
     */
    @GetMapping(value = "/tasks/{taskId}/submissions",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List task submissions",
               description = "Returns all submissions for a task. Employees scoped to own tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submissions returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TaskSubmissionResponse>> getSubmissionsForTask(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId) {
        return ResponseEntity.ok(submissionService.getSubmissionsForTask(taskId));
    }

    /**
     * Returns the latest submission for the given task.
     *
     * @param taskId the UUID of the task
     * @return the latest submission
     */
    @GetMapping(value = "/tasks/{taskId}/submissions/latest",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get latest task submission",
               description = "Returns the most recent submission for the given task.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Latest submission returned"),
            @ApiResponse(responseCode = "404", description = "Task or submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskSubmissionResponse> getLatestSubmission(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId) {
        return ResponseEntity.ok(submissionService.getLatestSubmission(taskId));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Submission-level endpoints  /task-submissions/{submissionId}
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Employee resubmits after manager has requested changes.
     *
     * <p>Accepts {@code multipart/form-data} with:
     * <ul>
     *   <li>{@code submission} — JSON part ({@link UpdateTaskSubmissionRequest})</li>
     *   <li>{@code file} — optional replacement file attachment</li>
     * </ul>
     *
     * <p>When {@code file} is omitted, the existing attachment (if any) is preserved.
     *
     * @param submissionId the UUID of the submission to update
     * @param submissionJson the updated submission JSON as a string part
     * @param file           optional replacement file
     * @return the updated submission
     */
    @PutMapping(value = "/task-submissions/{submissionId}/resubmit",
                consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Resubmit task after changes requested",
               description = "Employee updates and resubmits work after manager requested changes. "
                           + "Use multipart/form-data with a 'submission' JSON part "
                           + "and an optional 'file' part to replace the attachment.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskSubmissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or unsupported file type",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not the original submitter",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Submission not in CHANGES_REQUESTED state",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskSubmissionResponse> resubmit(
            @Parameter(description = "UUID of the submission")
            @PathVariable final UUID submissionId,
            @Parameter(description = "Updated submission data as a JSON string")
            @RequestPart("submission") final String submissionJson,
            @Parameter(description = "Optional replacement file attachment")
            @RequestPart(value = "file", required = false) final MultipartFile file)
            throws IOException {

        UpdateTaskSubmissionRequest request = objectMapper.readValue(
                submissionJson, UpdateTaskSubmissionRequest.class);

        validateUpdateRequest(request);

        return ResponseEntity.ok(submissionService.resubmit(submissionId, request, file));
    }

    /**
     * Manager approves a submission.
     *
     * @param submissionId the UUID of the submission to approve
     * @return the approved submission
     */
    @PostMapping(value = "/task-submissions/{submissionId}/approve",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Approve task submission",
               description = "Manager approves a task submission. Task transitions to COMPLETED.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Submission approved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskSubmissionResponse.class))),
            @ApiResponse(responseCode = "403", description = "Not a privileged user",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Submission not in PENDING_REVIEW state",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskSubmissionResponse> approve(
            @Parameter(description = "UUID of the submission")
            @PathVariable final UUID submissionId) {
        return ResponseEntity.ok(submissionService.approve(submissionId));
    }

    /**
     * Manager requests changes on a submission.
     *
     * @param submissionId the UUID of the submission
     * @param request      the request with review comment
     * @return the updated submission
     */
    @PostMapping(value = "/task-submissions/{submissionId}/request-changes",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Request changes on task submission",
               description = "Manager requests modifications from the employee. Task reverts to IN_PROGRESS.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Changes requested",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskSubmissionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Review comment is required",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Not a privileged user",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Submission not in PENDING_REVIEW state",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskSubmissionResponse> requestChanges(
            @Parameter(description = "UUID of the submission")
            @PathVariable final UUID submissionId,
            @Valid @RequestBody final RequestChangesRequest request) {
        return ResponseEntity.ok(submissionService.requestChanges(submissionId, request));
    }

    /**
     * Downloads the file attachment for the given submission.
     *
     * <p>Authorization:
     * <ul>
     *   <li>EMPLOYEE — can only download their own submission attachment.</li>
     *   <li>MANAGER / HR / ADMIN — can download any attachment.</li>
     * </ul>
     *
     * @param submissionId the UUID of the submission
     * @return streamed file with appropriate Content-Disposition header
     */
    @GetMapping(value = "/task-submissions/{submissionId}/attachment")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Download submission attachment",
               description = "Streams the file attachment for the given submission. "
                           + "Employees may only download their own attachments.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "File streamed"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission or attachment not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InputStreamResource> downloadAttachment(
            @Parameter(description = "UUID of the submission")
            @PathVariable final UUID submissionId) {

        TaskSubmissionService.AttachmentDownload dl =
                submissionService.downloadAttachment(submissionId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(dl.contentType()));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename(dl.originalFilename(), StandardCharsets.UTF_8)
                .build());
        if (dl.sizeBytes() > 0) {
            headers.setContentLength(dl.sizeBytes());
        }

        return ResponseEntity.ok()
                .headers(headers)
                .body(new InputStreamResource(dl.inputStream()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private validation helpers
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Validates a {@link CreateTaskSubmissionRequest} manually (cannot use @Valid on RequestPart String).
     */
    private void validateCreateRequest(final CreateTaskSubmissionRequest request) {
        if (request.submissionNotes() == null || request.submissionNotes().isBlank()) {
            throw new IllegalArgumentException("Submission notes are required.");
        }
    }

    /**
     * Validates an {@link UpdateTaskSubmissionRequest} manually.
     */
    private void validateUpdateRequest(final UpdateTaskSubmissionRequest request) {
        if (request.submissionNotes() == null || request.submissionNotes().isBlank()) {
            throw new IllegalArgumentException("Submission notes are required.");
        }
    }
}
