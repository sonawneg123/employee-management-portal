package com.company.employeemanagement.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Score trend response for manager view (Phase 7D).
 *
 * <p>Shows the score history for a task/submission so managers can see
 * whether the employee is improving, stable, or declining.
 *
 * <p>IMPORTANT: Failed evaluations are excluded from score trend calculations.
 * A single failed evaluation does NOT classify an employee negatively.
 *
 * @author Employee Management Portal Team
 */
public record AiScoreTrendResponse(

        /** UUID of the task this trend is for. */
        UUID taskId,

        /** List of score data points, oldest to newest. */
        List<ScorePoint> scoreHistory,

        /** Trend direction based on completed evaluations. */
        TrendDirection trendDirection,

        /** Score change between the oldest and newest completed evaluation. */
        Integer totalScoreChange,

        /** Most recent completed overall score, or null. */
        Integer latestScore,

        /** Previous completed overall score (second-most-recent), or null. */
        Integer previousScore,

        /** Score change between the last two completed evaluations. */
        Integer latestScoreChange,

        /**
         * Whether there is enough data to compute a trend.
         * False when fewer than 2 completed evaluations exist.
         */
        boolean hasTrendData

) {

    /**
     * A single score data point in the trend history.
     */
    public record ScorePoint(
            /** UUID of the AI review this data point comes from. */
            UUID reviewId,
            /** Submission number (1 = first submission for this task). */
            int submissionNumber,
            /** Overall completion score. */
            int overallScore,
            /** Work quality score. */
            Integer qualityScore,
            /** When this evaluation was completed. */
            LocalDateTime evaluatedAt
    ) {}

    /**
     * Trend direction categories.
     * Only computed when 2+ completed evaluations exist.
     * Failed evaluations are never used in trend classification.
     */
    public enum TrendDirection {
        /** Score increased by more than 5 points. */
        IMPROVING,
        /** Score changed by 5 points or less (either direction). */
        STABLE,
        /** Score decreased by more than 5 points. */
        DECLINING,
        /** Not enough completed evaluations to determine a trend. */
        INSUFFICIENT_DATA
    }
}
