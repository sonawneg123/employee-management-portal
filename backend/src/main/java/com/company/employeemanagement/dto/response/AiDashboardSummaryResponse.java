package com.company.employeemanagement.dto.response;

/**
 * AI summary for the manager dashboard (Phase 7D).
 *
 * <p>All counts come from stored data. No AI API calls are made for this endpoint.
 * All metrics are advisory — they help managers identify areas requiring attention.
 *
 * @author Employee Management Portal Team
 */
public record AiDashboardSummaryResponse(

        /** Total number of submissions that have been evaluated by AI. */
        int totalEvaluated,

        /** Average score across all completed AI evaluations (0–100), or null. */
        Double averageScore,

        /** Number of employees whose score trend is IMPROVING (based on last two evaluations). */
        int employeesImproving,

        /**
         * Number of employees whose score trend is DECLINING (needs attention).
         * Only classified as declining when 2+ evaluations exist — single-failure never classifies.
         */
        int employeesNeedingAttention,

        /** Number of submissions currently awaiting AI evaluation (PENDING or PROCESSING). */
        int submissionsAwaitingEvaluation,

        /** Number of evaluations that failed and have not been retried successfully. */
        int failedEvaluations

) {}
