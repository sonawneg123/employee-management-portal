package com.company.employeemanagement.ai.controller;

import com.company.employeemanagement.ai.service.TaskAiReviewService;
import com.company.employeemanagement.dto.response.TaskAiReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing AI Task Review endpoints (Phase 7A).
 *
 * <p>Base paths:
 * <ul>
 *   <li>{@code POST /task-submissions/{id}/ai-review} — request a new AI review</li>
 *   <li>{@code GET  /task-submissions/{id}/ai-review} — get the latest AI review</li>
 *   <li>{@code GET  /task-submissions/{id}/ai-reviews} — get all AI reviews</li>
 *   <li>{@code GET  /task-ai-reviews/{id}} — get a specific AI review by its own ID</li>
 * </ul>
 *
 * <p>Authorization:
 * <ul>
 *   <li>All endpoints require ADMIN, HR, or MANAGER role.</li>
 *   <li>EMPLOYEE role is explicitly denied — employees may not request or view AI reviews.</li>
 *   <li>IDOR protection is enforced at the service layer.</li>
 * </ul>
 *
 * <p>The AI recommendation is advisory only. The API response includes a
 * {@code recommendedAction} field but this never automatically modifies
 * submission status or task status.
 *
 * @author Employee Management Portal Team
 */
@RestController
@Tag(name = "AI Task Reviews", description = "Phase 7A: AI-powered task submission analysis (manager-only)")
@SecurityRequirement(name = "BearerAuth")
public class TaskAiReviewController {

    private static final Logger log = LoggerFactory.getLogger(TaskAiReviewController.class);

    private final TaskAiReviewService aiReviewService;

    /**
     * Constructs the controller with the required service.
     *
     * @param aiReviewService the AI review service
     */
    public TaskAiReviewController(final TaskAiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    /**
     * Requests an AI analysis of a task submission.
     *
     * <p>A new {@link com.company.employeemanagement.entity.TaskAiReview} record is created
     * immediately. The analysis is performed synchronously and the response includes
     * the full structured result (or error information if the AI call failed).
     *
     * <p>Returns 409 Conflict if a PENDING/PROCESSING review already exists for this submission.
     *
     * @param submissionId the UUID of the submission to analyse
     * @return the created AI review
     */
    @PostMapping(value = "/task-submissions/{submissionId}/ai-review",
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Request AI analysis of a task submission",
            description = "Manager requests an AI-powered analysis of a task submission. "
                        + "Returns structured analysis including completion score, requirement "
                        + "breakdown, and advisory recommendation. "
                        + "ADVISORY ONLY — does not modify submission or task status. "
                        + "Returns 409 if an analysis is already in progress.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "AI review created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskAiReviewResponse.class))),
            @ApiResponse(responseCode = "403", description = "Requires ADMIN, HR, or MANAGER role",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Review already in progress",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskAiReviewResponse> requestReview(
            @Parameter(description = "UUID of the task submission")
            @PathVariable final UUID submissionId) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("AI REVIEW REQUEST RECEIVED — submissionId={} user={}",
                submissionId, auth != null ? auth.getName() : "unknown");
        TaskAiReviewResponse response = aiReviewService.requestReview(submissionId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns the most recent AI review for a submission.
     *
     * @param submissionId the UUID of the submission
     * @return the latest AI review
     */
    @GetMapping(value = "/task-submissions/{submissionId}/ai-review",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Get the latest AI review for a submission",
            description = "Returns the most recent AI analysis for the given submission. "
                        + "Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI review returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskAiReviewResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission or review not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskAiReviewResponse> getLatestReview(
            @Parameter(description = "UUID of the task submission")
            @PathVariable final UUID submissionId) {
        return ResponseEntity.ok(aiReviewService.getLatestReviewForSubmission(submissionId));
    }

    /**
     * Returns all AI reviews for a submission.
     *
     * @param submissionId the UUID of the submission
     * @return list of AI reviews, newest first
     */
    @GetMapping(value = "/task-submissions/{submissionId}/ai-reviews",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Get all AI reviews for a submission",
            description = "Returns all AI analyses for the given submission, newest first. "
                        + "Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI reviews returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TaskAiReviewResponse>> getAllReviews(
            @Parameter(description = "UUID of the task submission")
            @PathVariable final UUID submissionId) {
        return ResponseEntity.ok(aiReviewService.getAllReviewsForSubmission(submissionId));
    }

    /**
     * Returns a specific AI review by its own UUID.
     *
     * @param reviewId the UUID of the AI review record
     * @return the AI review
     */
    @GetMapping(value = "/task-ai-reviews/{reviewId}",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Get a specific AI review by ID",
            description = "Returns a specific AI analysis record by its own UUID. "
                        + "Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI review returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = TaskAiReviewResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "AI review not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskAiReviewResponse> getReviewById(
            @Parameter(description = "UUID of the AI review")
            @PathVariable final UUID reviewId) {
        return ResponseEntity.ok(aiReviewService.getReviewById(reviewId));
    }
}
