package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /api/analytics/performance} endpoint.
 *
 * <p>Aggregates AI evaluation scores from {@code task_ai_reviews}.
 * Accessible to ADMIN, HR, and MANAGER roles only (not EMPLOYEE).
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "AI performance evaluation analytics")
public record AnalyticsPerformanceResponse(

        @Schema(description = "Average completion score across all completed AI evaluations (0-100); -1 if none", example = "78.5")
        double avgCompletionScore,

        @Schema(description = "Average quality score across all completed AI evaluations (0-100); -1 if none", example = "74.2")
        double avgQualityScore,

        @Schema(description = "Total completed AI evaluations", example = "42")
        long completedEvaluations,

        @Schema(description = "Total failed AI evaluations", example = "3")
        long failedEvaluations,

        @Schema(description = "Total pending/processing AI evaluations", example = "1")
        long pendingEvaluations,

        @Schema(description = "Average AI score trend over time")
        List<ScoreTrendPoint> scoreTrend
) {

    /**
     * AI score trend point.
     *
     * @param label           Period label (e.g., date or month string).
     * @param avgScore        Average score for this period.
     * @param evaluationCount Number of evaluations in this period.
     */
    @Schema(description = "Average AI score for a time period")
    public record ScoreTrendPoint(
            @Schema(description = "Period label (date or month)", example = "2024-06-01")
            String label,

            @Schema(description = "Average score (0-100)", example = "82.3")
            double avgScore,

            @Schema(description = "Number of evaluations in this period", example = "8")
            long evaluationCount
    ) {}
}
