package com.company.employeemanagement.ai.controller;

import com.company.employeemanagement.ai.service.AiFeedbackService;
import com.company.employeemanagement.dto.response.AiDashboardSummaryResponse;
import com.company.employeemanagement.dto.response.AiFeedbackResponse;
import com.company.employeemanagement.dto.response.AiScoreTrendResponse;
import com.company.employeemanagement.dto.response.AiTaskInsightsResponse;
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
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for Phase 7D AI feedback, trends, insights, and dashboard.
 *
 * <p>New endpoints:
 * <ul>
 *   <li>{@code GET /task-submissions/{id}/ai-feedback}  — employee-safe feedback</li>
 *   <li>{@code GET /task-submissions/{id}/ai-history}   — employee AI history</li>
 *   <li>{@code GET /tasks/{id}/ai-trend}                — manager score trend</li>
 *   <li>{@code GET /tasks/{id}/ai-insights}             — manager task insights</li>
 *   <li>{@code GET /ai/dashboard-summary}               — manager AI dashboard summary</li>
 * </ul>
 *
 * <p>Security:
 * <ul>
 *   <li>Employee feedback/history: EMPLOYEE, MANAGER, HR, ADMIN (IDOR enforced in service).</li>
 *   <li>Manager trend/insights/dashboard: MANAGER, HR, ADMIN only.</li>
 * </ul>
 *
 * <p>AI evaluation is advisory only. Manager decisions always take precedence.
 *
 * @author Employee Management Portal Team
 */
@RestController
@Tag(name = "AI Feedback & Insights", description = "Phase 7D: Employee AI feedback and manager AI analytics")
@SecurityRequirement(name = "BearerAuth")
public class AiFeedbackController {

    private static final Logger log = LoggerFactory.getLogger(AiFeedbackController.class);

    private final AiFeedbackService aiFeedbackService;

    /**
     * Constructs the controller with required service dependency.
     *
     * @param aiFeedbackService the AI feedback service
     */
    public AiFeedbackController(final AiFeedbackService aiFeedbackService) {
        this.aiFeedbackService = aiFeedbackService;
    }

    // ── Employee AI Feedback ──────────────────────────────────────────────────

    /**
     * Returns the employee-safe AI feedback for a submission.
     *
     * <p>Employees may only view feedback for their own submissions.
     * Does NOT expose: recommendedAction, managerSummary, errorMessage, AI internals.
     *
     * @param submissionId UUID of the submission
     * @return employee-safe AI feedback
     */
    @GetMapping(value = "/task-submissions/{submissionId}/ai-feedback",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary = "Get employee-safe AI feedback for a submission",
            description = "Returns AI feedback safe for employee viewing. "
                        + "Employees may only view feedback for their own submissions. "
                        + "Does NOT expose managerSummary, recommendedAction, or AI internals. "
                        + "AI evaluation is advisory only — manager decisions take precedence.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI feedback returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AiFeedbackResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission or AI review not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AiFeedbackResponse> getEmployeeAiFeedback(
            @Parameter(description = "UUID of the task submission")
            @PathVariable final UUID submissionId) {
        log.debug("AI FEEDBACK REQUEST — submissionId={}", submissionId);
        return ResponseEntity.ok(aiFeedbackService.getEmployeeAiFeedback(submissionId));
    }

    /**
     * Returns the AI evaluation history for a submission (employee-safe view).
     *
     * <p>Shows all past AI evaluations for a submission, newest first.
     * Employees may only view history for their own submissions.
     *
     * @param submissionId UUID of the submission
     * @return list of employee-safe AI evaluations
     */
    @GetMapping(value = "/task-submissions/{submissionId}/ai-history",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary = "Get AI evaluation history for a submission (employee-safe)",
            description = "Returns all AI evaluations for a submission, newest first. "
                        + "Employees may only view history for their own submissions. "
                        + "Does NOT expose manager-only fields.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI history returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Submission not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<AiFeedbackResponse>> getEmployeeAiHistory(
            @Parameter(description = "UUID of the task submission")
            @PathVariable final UUID submissionId) {
        log.debug("AI HISTORY REQUEST — submissionId={}", submissionId);
        return ResponseEntity.ok(aiFeedbackService.getEmployeeAiHistory(submissionId));
    }

    // ── Manager AI Trend ──────────────────────────────────────────────────────

    /**
     * Returns the AI score trend for a task (manager view).
     *
     * <p>Shows score progression across completed AI evaluations.
     * Failed evaluations are excluded from trend calculations.
     * Requires ADMIN, HR, or MANAGER role.
     *
     * @param taskId UUID of the task
     * @return score trend with improvement/stable/declining indicator
     */
    @GetMapping(value = "/tasks/{taskId}/ai-trend",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Get AI score trend for a task",
            description = "Returns the score history and trend for a task across all completed "
                        + "AI evaluations. Failed evaluations are excluded from trend. "
                        + "Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Score trend returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AiScoreTrendResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<AiScoreTrendResponse> getScoreTrend(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId) {
        return ResponseEntity.ok(aiFeedbackService.getScoreTrend(taskId));
    }

    /**
     * Returns AI task insights for the given task (manager view).
     *
     * <p>Aggregates stored AI review data — no new AI API calls.
     * Requires ADMIN, HR, or MANAGER role.
     *
     * @param taskId UUID of the task
     * @return AI task insights including common issues, strengths, suggestions
     */
    @GetMapping(value = "/tasks/{taskId}/ai-insights",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Get AI insights for a task",
            description = "Aggregates stored AI review results for a task. "
                        + "No new AI API calls. "
                        + "Shows common issues, repeated weaknesses, recent strengths, suggestions. "
                        + "Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI insights returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AiTaskInsightsResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied"),
            @ApiResponse(responseCode = "404", description = "Task not found")
    })
    public ResponseEntity<AiTaskInsightsResponse> getTaskInsights(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId) {
        return ResponseEntity.ok(aiFeedbackService.getTaskInsights(taskId));
    }

    // ── Dashboard AI Summary ──────────────────────────────────────────────────

    /**
     * Returns the AI summary for the manager dashboard.
     *
     * <p>All numbers come from stored data. No AI API calls.
     * Requires ADMIN, HR, or MANAGER role.
     *
     * @return AI dashboard summary
     */
    @GetMapping(value = "/ai/dashboard-summary",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary = "Get AI summary for manager dashboard",
            description = "Returns aggregated AI evaluation metrics for the manager dashboard. "
                        + "All counts come from stored evaluation data. No new AI calls.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI dashboard summary returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AiDashboardSummaryResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AiDashboardSummaryResponse> getDashboardSummary() {
        return ResponseEntity.ok(aiFeedbackService.getDashboardSummary());
    }
}
