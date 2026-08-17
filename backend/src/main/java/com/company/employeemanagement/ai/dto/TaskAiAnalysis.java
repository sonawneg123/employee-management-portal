package com.company.employeemanagement.ai.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Structured AI analysis result for a task submission review.
 *
 * <p>This is the contract between the AI service and the rest of the system.
 * The AI must always return a JSON object matching this schema.
 * All fields that the AI cannot determine should be left at their defaults
 * rather than hallucinated.
 *
 * <p>Phase 7A: advisory only. The AI NEVER automatically approves or rejects.
 *
 * @author Employee Management Portal Team
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TaskAiAnalysis(

        /**
         * Overall completion percentage (0–100).
         * How much of the required work appears to have been done.
         */
        int completionScore,

        /**
         * One-paragraph overall assessment of the submission.
         */
        String overallAssessment,

        /**
         * Per-requirement breakdown.
         */
        List<RequirementAnalysis> requirements,

        /**
         * List of items the AI believes are clearly completed.
         */
        List<String> completedItems,

        /**
         * List of items that appear to be missing from the submission.
         */
        List<String> missingItems,

        /**
         * List of items that appear to be partially addressed.
         */
        List<String> partialItems,

        /**
         * Quality assessment of the submission.
         */
        QualityAssessment qualityAssessment,

        /**
         * Specific issues found in the submission.
         */
        List<String> issues,

        /**
         * Concrete suggestions for improving the submission.
         */
        List<String> modificationSuggestions,

        /**
         * A concise summary written for the manager's benefit.
         */
        String managerSummary,

        /**
         * Advisory recommended action. Manager always decides.
         * One of: APPROVE, REQUEST_CHANGES, MANUAL_REVIEW
         */
        String recommendedAction,

        /**
         * AI confidence in its own analysis (0–100).
         */
        int confidence

) {

    /**
     * Analysis of a single requirement extracted from the task guidelines.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RequirementAnalysis(

            /** Description of the requirement. */
            String requirement,

            /**
             * Status of this requirement.
             * One of: COMPLETED, PARTIALLY_COMPLETED, MISSING, UNCLEAR
             */
            String status,

            /** Evidence from the submission that supports the status assessment. */
            String evidence,

            /** Suggestion for addressing this requirement if not fully met. */
            String suggestion

    ) {}

    /**
     * Quality dimensions of the submission beyond mere requirement completion.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record QualityAssessment(

            /** Quality score (0–100). */
            int score,

            /** One-paragraph quality summary. */
            String summary,

            /** Things the submission does well. */
            List<String> strengths,

            /** Areas where the submission could be improved. */
            List<String> weaknesses

    ) {}
}
