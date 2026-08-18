package com.company.employeemanagement.dto.response;

import java.util.List;
import java.util.UUID;

/**
 * AI task insights response for manager view (Phase 7D).
 *
 * <p>Aggregates stored AI review results to produce task-level insights.
 * This does NOT call the AI provider — it reads stored evaluation data only.
 *
 * <p>AI insights are advisory. Manager decisions always take precedence.
 *
 * @author Employee Management Portal Team
 */
public record AiTaskInsightsResponse(

        /** UUID of the task these insights are for. */
        UUID taskId,

        /** Total number of AI evaluations performed for this task. */
        int totalEvaluations,

        /** Number of completed evaluations. */
        int completedEvaluations,

        /** Average overall score across all completed evaluations, or null if none. */
        Double averageScore,

        /**
         * Common issues seen across evaluations.
         * Deduplicated and ordered by frequency.
         */
        List<String> commonIssues,

        /**
         * Repeated weaknesses identified across evaluations.
         * Deduplicated and ordered by frequency.
         */
        List<String> repeatedWeaknesses,

        /**
         * Most recent strengths from the latest completed evaluation.
         */
        List<String> mostRecentStrengths,

        /**
         * Most recent improvement suggestions from the latest completed evaluation.
         * Deduplicated.
         */
        List<String> mostRecentSuggestions,

        /**
         * Trend direction based on completed evaluations.
         */
        AiScoreTrendResponse.TrendDirection improvementTrend

) {}
