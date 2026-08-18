package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.dto.TaskAiAnalysis;
import com.company.employeemanagement.dto.response.AiDashboardSummaryResponse;
import com.company.employeemanagement.dto.response.AiFeedbackResponse;
import com.company.employeemanagement.dto.response.AiScoreTrendResponse;
import com.company.employeemanagement.dto.response.AiScoreTrendResponse.TrendDirection;
import com.company.employeemanagement.dto.response.AiTaskInsightsResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for Phase 7D AI feedback, trends, insights, and dashboard summaries.
 *
 * <h2>Authorization model</h2>
 * <ul>
 *   <li>Employee: may only view their own AI feedback and AI history.</li>
 *   <li>Manager / HR / Admin: may view AI trends, insights, and dashboard summary.</li>
 *   <li>Cross-employee data leakage is prevented at the service layer.</li>
 * </ul>
 *
 * <h2>Safety rules</h2>
 * <ul>
 *   <li>Employee-facing responses NEVER expose: recommendedAction, managerSummary,
 *       structuredAnalysisJson, errorMessage, or AI provider internals.</li>
 *   <li>AI scores are advisory only. AI never approves or rejects submissions.</li>
 *   <li>Failed evaluations are excluded from score trend calculations.</li>
 *   <li>No new AI API calls are made by this service — all data comes from stored reviews.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class AiFeedbackService {

    private static final Logger log = LoggerFactory.getLogger(AiFeedbackService.class);

    private static final int TREND_STABILITY_THRESHOLD = 5;

    private final TaskAiReviewRepository aiReviewRepository;
    private final TaskSubmissionRepository submissionRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    /**
     * Constructs the service with required dependencies.
     */
    public AiFeedbackService(
            final TaskAiReviewRepository aiReviewRepository,
            final TaskSubmissionRepository submissionRepository,
            final SecurityUtils securityUtils,
            final ObjectMapper objectMapper) {
        this.aiReviewRepository = aiReviewRepository;
        this.submissionRepository = submissionRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    // ── Employee AI Feedback ──────────────────────────────────────────────────

    /**
     * Returns the employee-safe AI feedback for the given submission.
     *
     * <p>Authorization: the authenticated employee must be the submitter of the submission.
     * Managers/HR/Admin may also call this to preview the employee-facing view.
     *
     * @param submissionId UUID of the submission
     * @return employee-safe AI feedback response
     * @throws ResourceNotFoundException if submission or AI review not found
     * @throws AccessDeniedException     if the employee does not own this submission
     */
    @Transactional(readOnly = true)
    public AiFeedbackResponse getEmployeeAiFeedback(final UUID submissionId) {
        // Load and authorize access to the submission
        TaskSubmission submission = loadSubmissionAndAuthorize(submissionId);

        // Load the latest AI review for this submission
        Optional<TaskAiReview> reviewOpt = aiReviewRepository.findLatestBySubmissionId(submissionId);

        if (reviewOpt.isEmpty()) {
            throw new ResourceNotFoundException("No AI review found for submission: " + submissionId);
        }

        TaskAiReview review = reviewOpt.get();
        return toAiFeedbackResponse(review);
    }

    /**
     * Returns the AI evaluation history for a given submission (employee-safe view).
     *
     * <p>Shows all past AI evaluations for a submission, ordered newest first.
     * Each item exposes only employee-safe fields.
     *
     * @param submissionId UUID of the submission
     * @return list of employee-safe AI feedback entries
     * @throws AccessDeniedException     if the employee does not own this submission
     * @throws ResourceNotFoundException if the submission does not exist
     */
    @Transactional(readOnly = true)
    public List<AiFeedbackResponse> getEmployeeAiHistory(final UUID submissionId) {
        // Load and authorize access to the submission
        loadSubmissionAndAuthorize(submissionId);

        return aiReviewRepository
                .findAllBySubmissionIdOrderByCreatedAtDesc(submissionId)
                .stream()
                .map(this::toAiFeedbackResponse)
                .collect(Collectors.toList());
    }

    // ── Manager AI Trend ──────────────────────────────────────────────────────

    /**
     * Returns the AI score trend for a task (manager view).
     *
     * <p>Computes trend from stored completed AI evaluations only.
     * Failed evaluations are excluded from trend calculations.
     * Requires ADMIN, HR, or MANAGER role.
     *
     * @param taskId UUID of the task
     * @return score trend response
     * @throws AccessDeniedException if the caller is not privileged
     */
    @Transactional(readOnly = true)
    public AiScoreTrendResponse getScoreTrend(final UUID taskId) {
        requirePrivileged();

        List<TaskAiReview> allReviews = aiReviewRepository.findAllByTaskId(taskId);

        // Only include COMPLETED reviews in trend
        List<TaskAiReview> completed = allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.COMPLETED)
                .sorted((a, b) -> {
                    // Sort by completedAt ascending (oldest first)
                    if (a.getCompletedAt() == null) return 1;
                    if (b.getCompletedAt() == null) return -1;
                    return a.getCompletedAt().compareTo(b.getCompletedAt());
                })
                .collect(Collectors.toList());

        if (completed.size() < 2) {
            // Not enough data for trend
            List<AiScoreTrendResponse.ScorePoint> points = buildScorePoints(completed);
            Integer latestScore = completed.isEmpty() ? null
                    : completed.get(completed.size() - 1).getCompletionScore();
            return new AiScoreTrendResponse(
                    taskId, points, TrendDirection.INSUFFICIENT_DATA,
                    null, latestScore, null, null, false);
        }

        List<AiScoreTrendResponse.ScorePoint> points = buildScorePoints(completed);

        TaskAiReview latest = completed.get(completed.size() - 1);
        TaskAiReview previous = completed.get(completed.size() - 2);
        TaskAiReview oldest = completed.get(0);

        int latestScore = nullToZero(latest.getCompletionScore());
        int previousScore = nullToZero(previous.getCompletionScore());
        int oldestScore = nullToZero(oldest.getCompletionScore());

        int latestChange = latestScore - previousScore;
        int totalChange = latestScore - oldestScore;

        TrendDirection direction;
        if (latestChange > TREND_STABILITY_THRESHOLD) {
            direction = TrendDirection.IMPROVING;
        } else if (latestChange < -TREND_STABILITY_THRESHOLD) {
            direction = TrendDirection.DECLINING;
        } else {
            direction = TrendDirection.STABLE;
        }

        return new AiScoreTrendResponse(
                taskId, points, direction, totalChange,
                latestScore, previousScore, latestChange, true);
    }

    // ── Manager AI Insights ───────────────────────────────────────────────────

    /**
     * Returns AI task insights for the given task (manager view).
     *
     * <p>Aggregates stored review data — NO new AI API calls.
     * Requires ADMIN, HR, or MANAGER role.
     *
     * @param taskId UUID of the task
     * @return AI task insights
     */
    @Transactional(readOnly = true)
    public AiTaskInsightsResponse getTaskInsights(final UUID taskId) {
        requirePrivileged();

        List<TaskAiReview> allReviews = aiReviewRepository.findAllByTaskId(taskId);
        List<TaskAiReview> completed = allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.COMPLETED)
                .collect(Collectors.toList());

        if (completed.isEmpty()) {
            return new AiTaskInsightsResponse(
                    taskId,
                    allReviews.size(),
                    0,
                    null,
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    Collections.emptyList(),
                    TrendDirection.INSUFFICIENT_DATA
            );
        }

        // Compute average score
        double avgScore = completed.stream()
                .filter(r -> r.getCompletionScore() != null)
                .mapToInt(TaskAiReview::getCompletionScore)
                .average()
                .orElse(0.0);

        // Aggregate issues and weaknesses from all completed reviews (most recent first for dedup)
        List<TaskAiReview> newestFirst = completed.stream()
                .sorted((a, b) -> {
                    if (a.getCompletedAt() == null) return 1;
                    if (b.getCompletedAt() == null) return -1;
                    return b.getCompletedAt().compareTo(a.getCompletedAt());
                })
                .collect(Collectors.toList());

        List<String> commonIssues = aggregateFromAnalyses(newestFirst, analysis -> {
            if (analysis == null || analysis.issues() == null) return Collections.emptyList();
            return analysis.issues();
        });

        List<String> repeatedWeaknesses = aggregateFromAnalyses(newestFirst, analysis -> {
            if (analysis == null || analysis.qualityAssessment() == null) return Collections.emptyList();
            List<String> weaknesses = analysis.qualityAssessment().weaknesses();
            return weaknesses != null ? weaknesses : Collections.emptyList();
        });

        // Most recent strengths and suggestions from the latest completed review
        TaskAiReview latestReview = newestFirst.get(0);
        List<String> mostRecentStrengths = getStrengthsFromReview(latestReview);
        List<String> mostRecentSuggestions = getSuggestionsFromReview(latestReview);

        // Trend from score trend service logic
        AiScoreTrendResponse trend = getScoreTrend(taskId);

        return new AiTaskInsightsResponse(
                taskId,
                allReviews.size(),
                completed.size(),
                Math.round(avgScore * 10.0) / 10.0,
                commonIssues,
                repeatedWeaknesses,
                mostRecentStrengths,
                mostRecentSuggestions,
                trend.trendDirection()
        );
    }

    // ── Dashboard AI Summary ──────────────────────────────────────────────────

    /**
     * Returns the AI summary for the manager dashboard (Phase 7D).
     *
     * <p>All counts come from stored data. No AI API calls are made.
     * Requires ADMIN, HR, or MANAGER role.
     *
     * @return AI dashboard summary
     */
    @Transactional(readOnly = true)
    public AiDashboardSummaryResponse getDashboardSummary() {
        requirePrivileged();

        List<TaskAiReview> allReviews = aiReviewRepository.findAll();

        int totalEvaluated = (int) allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.COMPLETED)
                .map(r -> r.getSubmission() != null ? r.getSubmission().getId() : null)
                .filter(id -> id != null)
                .distinct()
                .count();

        OptionalDouble avgOpt = allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.COMPLETED
                        && r.getCompletionScore() != null)
                .mapToInt(TaskAiReview::getCompletionScore)
                .average();
        Double averageScore = avgOpt.isPresent()
                ? Math.round(avgOpt.getAsDouble() * 10.0) / 10.0
                : null;

        // Group completed reviews by submission's assigned employee (via task)
        Map<UUID, List<TaskAiReview>> reviewsByTask = allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.COMPLETED
                        && r.getTask() != null)
                .collect(Collectors.groupingBy(r -> r.getTask().getId()));

        int improving = 0;
        int needingAttention = 0;
        for (Map.Entry<UUID, List<TaskAiReview>> entry : reviewsByTask.entrySet()) {
            List<TaskAiReview> taskCompleted = entry.getValue().stream()
                    .filter(r -> r.getCompletionScore() != null)
                    .sorted((a, b) -> {
                        if (a.getCompletedAt() == null) return 1;
                        if (b.getCompletedAt() == null) return -1;
                        return a.getCompletedAt().compareTo(b.getCompletedAt());
                    })
                    .collect(Collectors.toList());
            if (taskCompleted.size() >= 2) {
                int latest = taskCompleted.get(taskCompleted.size() - 1).getCompletionScore();
                int previous = taskCompleted.get(taskCompleted.size() - 2).getCompletionScore();
                int change = latest - previous;
                if (change > TREND_STABILITY_THRESHOLD) {
                    improving++;
                } else if (change < -TREND_STABILITY_THRESHOLD) {
                    needingAttention++;
                }
            }
        }

        long awaitingEvaluation = allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.PENDING
                        || r.getStatus() == AiReviewStatus.PROCESSING)
                .count();

        long failed = allReviews.stream()
                .filter(r -> r.getStatus() == AiReviewStatus.FAILED)
                .count();

        return new AiDashboardSummaryResponse(
                totalEvaluated,
                averageScore,
                improving,
                needingAttention,
                (int) awaitingEvaluation,
                (int) failed
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Loads a submission and verifies the current user has read access to it.
     *
     * <p>Employee: only their own submission.
     * Manager/HR/Admin: any submission.
     */
    private TaskSubmission loadSubmissionAndAuthorize(final UUID submissionId) {
        TaskSubmission submission = submissionRepository.findByIdWithAssociations(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task submission not found: " + submissionId));

        // Employee: must own the submission
        if (isEmployeeOnly()) {
            Employee currentEmployee = securityUtils.getCurrentEmployee()
                    .orElseThrow(() -> new AccessDeniedException(
                            "No employee record found for current user."));
            if (submission.getSubmittedBy() == null
                    || !currentEmployee.getId().equals(submission.getSubmittedBy().getId())) {
                // Return 404 to avoid leaking existence of other employees' submissions
                throw new ResourceNotFoundException(
                        "Task submission not found: " + submissionId);
            }
        }

        return submission;
    }

    /**
     * Converts a {@link TaskAiReview} to an employee-safe {@link AiFeedbackResponse}.
     *
     * <p>Intentionally omits: recommendedAction, managerSummary, structuredAnalysisJson,
     * errorMessage, AI provider details, prompt version.
     */
    private AiFeedbackResponse toAiFeedbackResponse(final TaskAiReview review) {
        List<String> strengths = Collections.emptyList();
        List<String> areasToImprove = Collections.emptyList();
        List<String> suggestions = Collections.emptyList();
        String summary = null;

        if (review.getStatus() == AiReviewStatus.COMPLETED
                && review.getStructuredAnalysisJson() != null) {
            TaskAiAnalysis analysis = parseAnalysisSafe(review.getStructuredAnalysisJson());
            if (analysis != null) {
                summary = analysis.overallAssessment();
                if (analysis.qualityAssessment() != null) {
                    strengths = nonNull(analysis.qualityAssessment().strengths());
                    areasToImprove = nonNull(analysis.qualityAssessment().weaknesses());
                }
                suggestions = nonNull(analysis.modificationSuggestions());
                // Supplement areasToImprove with missing items if empty
                if (areasToImprove.isEmpty() && analysis.missingItems() != null) {
                    areasToImprove = analysis.missingItems();
                }
            }
        } else if (review.getStatus() == AiReviewStatus.PENDING) {
            summary = AiFeedbackResponse.PENDING_MESSAGE;
        } else if (review.getStatus() == AiReviewStatus.PROCESSING) {
            summary = AiFeedbackResponse.PROCESSING_MESSAGE;
        } else if (review.getStatus() == AiReviewStatus.FAILED) {
            // Do NOT expose the raw error message — show the friendly version only
            summary = AiFeedbackResponse.FAILED_MESSAGE;
        }

        return new AiFeedbackResponse(
                review.getId(),
                review.getSubmission() != null ? review.getSubmission().getId() : null,
                review.getStatus(),
                review.getCompletionScore(),           // overallScore
                review.getQualityScore(),              // workQualityScore
                review.getCompletionScore(),           // completenessScore (same as overall in Phase 7D)
                review.getConfidence(),                // relevanceScore (confidence proxy)
                summary,
                strengths,
                areasToImprove,
                suggestions,
                review.getCompletedAt(),
                review.getCreatedAt(),
                AiFeedbackResponse.STANDARD_EVALUATION_EXPLANATION
        );
    }

    /**
     * Builds the list of score points from completed reviews (oldest first).
     */
    private List<AiScoreTrendResponse.ScorePoint> buildScorePoints(
            final List<TaskAiReview> completed) {
        List<AiScoreTrendResponse.ScorePoint> points = new ArrayList<>();
        for (int i = 0; i < completed.size(); i++) {
            TaskAiReview r = completed.get(i);
            points.add(new AiScoreTrendResponse.ScorePoint(
                    r.getId(),
                    i + 1,
                    nullToZero(r.getCompletionScore()),
                    r.getQualityScore(),
                    r.getCompletedAt()
            ));
        }
        return points;
    }

    /**
     * Aggregates a list of strings from all completed analyses.
     * Deduplicates by content and returns ordered by frequency (most frequent first).
     */
    private List<String> aggregateFromAnalyses(
            final List<TaskAiReview> reviews,
            final java.util.function.Function<TaskAiAnalysis, List<String>> extractor) {

        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (TaskAiReview review : reviews) {
            if (review.getStructuredAnalysisJson() == null) continue;
            TaskAiAnalysis analysis = parseAnalysisSafe(review.getStructuredAnalysisJson());
            if (analysis == null) continue;
            List<String> items = extractor.apply(analysis);
            for (String item : items) {
                if (item != null && !item.isBlank()) {
                    frequency.merge(item.trim(), 1, Integer::sum);
                }
            }
        }

        // Sort by frequency descending, then deduplicate
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .map(Map.Entry::getKey)
                .limit(10)
                .collect(Collectors.toList());
    }

    private List<String> getStrengthsFromReview(final TaskAiReview review) {
        if (review.getStructuredAnalysisJson() == null) return Collections.emptyList();
        TaskAiAnalysis analysis = parseAnalysisSafe(review.getStructuredAnalysisJson());
        if (analysis == null || analysis.qualityAssessment() == null) return Collections.emptyList();
        return nonNull(analysis.qualityAssessment().strengths());
    }

    private List<String> getSuggestionsFromReview(final TaskAiReview review) {
        if (review.getStructuredAnalysisJson() == null) return Collections.emptyList();
        TaskAiAnalysis analysis = parseAnalysisSafe(review.getStructuredAnalysisJson());
        if (analysis == null) return Collections.emptyList();
        // Deduplicate suggestions
        Set<String> seen = new LinkedHashSet<>(nonNull(analysis.modificationSuggestions()));
        return new ArrayList<>(seen);
    }

    /**
     * Parses structured JSON, returns null on failure (never throws).
     */
    private TaskAiAnalysis parseAnalysisSafe(final String json) {
        if (json == null || json.isBlank()) return null;
        try {
            String cleaned = json.trim();
            if (cleaned.startsWith("```")) {
                int firstNewline = cleaned.indexOf('\n');
                if (firstNewline >= 0) cleaned = cleaned.substring(firstNewline + 1);
                if (cleaned.endsWith("```")) cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
            return objectMapper.readValue(cleaned, TaskAiAnalysis.class);
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse AI analysis JSON for feedback response: {}", e.getMessage());
            return null;
        }
    }

    private boolean isEmployeeOnly() {
        return securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged();
    }

    private void requirePrivileged() {
        if (!securityUtils.isPrivileged()) {
            throw new AccessDeniedException(
                    "Only managers, HR, and administrators may view AI analytics.");
        }
    }

    private static int nullToZero(final Integer value) {
        return value != null ? value : 0;
    }

    @SuppressWarnings("unchecked")
    private static <T> List<T> nonNull(final List<T> list) {
        return list != null ? list : Collections.emptyList();
    }
}
