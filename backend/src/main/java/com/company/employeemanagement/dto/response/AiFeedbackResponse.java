package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.AiReviewStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Employee-safe AI feedback response (Phase 7D).
 *
 * <p>This DTO exposes only the fields that are appropriate for an employee to see.
 * It intentionally OMITS:
 * <ul>
 *   <li>Manager-only recommendation ({@code recommendedAction})</li>
 *   <li>Internal manager summary</li>
 *   <li>Raw AI model response or structured JSON</li>
 *   <li>Error details / stack traces</li>
 *   <li>AI provider internals (API keys, model name in raw form)</li>
 *   <li>Prompt or context data</li>
 *   <li>Any data from other employees' submissions</li>
 * </ul>
 *
 * <p>The AI evaluation is advisory — it helps employees understand areas for improvement,
 * but does NOT replace manager decisions. The manager is always the final authority.
 *
 * @author Employee Management Portal Team
 */
public record AiFeedbackResponse(

        /** UUID of the AI review record. */
        UUID id,

        /** UUID of the submission this review covers. */
        UUID submissionId,

        /** Current lifecycle status of the AI evaluation. */
        AiReviewStatus status,

        /**
         * Overall completion score (0–100). {@code null} when not yet completed.
         */
        Integer overallScore,

        /**
         * Work quality score (0–100). {@code null} when not yet completed.
         */
        Integer workQualityScore,

        /**
         * Completeness score (0–100). {@code null} when not yet completed.
         */
        Integer completenessScore,

        /**
         * Relevance score — derived from confidence (0–100). {@code null} when not yet completed.
         */
        Integer relevanceScore,

        /**
         * Short employee-friendly summary of the evaluation. Never exposes internal scoring logic.
         */
        String summary,

        /**
         * Strengths identified in the submission. From the AI quality assessment.
         */
        List<String> strengths,

        /**
         * Areas that could be improved. Derived from AI weaknesses/missing items.
         */
        List<String> areasToImprove,

        /**
         * Concrete suggestions for the next submission. From modification suggestions.
         */
        List<String> suggestionsForNextSubmission,

        /**
         * Timestamp when the evaluation was completed. {@code null} when not yet completed.
         */
        LocalDateTime evaluatedAt,

        /**
         * Timestamp when the evaluation was requested.
         */
        LocalDateTime requestedAt,

        /**
         * Human-readable explanation of how this evaluation was produced.
         * Safe for display — does NOT expose system prompt, API keys, or internal logic.
         */
        String evaluationExplanation

) {

    /**
     * Safe explanation text shown to employees.
     * Does NOT expose: system prompt, API keys, prompt instructions, or model details.
     */
    public static final String STANDARD_EVALUATION_EXPLANATION =
            "This evaluation was produced by an AI assistant that analysed:\n"
            + "• The task title and description\n"
            + "• The task guidelines and acceptance criteria\n"
            + "• Your submission text\n"
            + "• Any uploaded document content (when supported)\n\n"
            + "The AI applied structured evaluation criteria to assess completeness, "
            + "work quality, and alignment with the task requirements.\n\n"
            + "⚠ AI evaluation is advisory only. Your manager makes the final decision.";

    /**
     * Message shown when the AI evaluation is pending.
     */
    public static final String PENDING_MESSAGE =
            "AI evaluation is in the queue and will start shortly.";

    /**
     * Message shown when the AI evaluation is being processed.
     */
    public static final String PROCESSING_MESSAGE =
            "AI evaluation is being generated. This usually takes under a minute.";

    /**
     * Friendly message shown when the AI evaluation failed.
     * Does NOT expose raw error messages, API exceptions, or internal failure details.
     */
    public static final String FAILED_MESSAGE =
            "The AI evaluation could not be completed at this time. "
            + "Your manager has been notified and may request a retry.";
}
