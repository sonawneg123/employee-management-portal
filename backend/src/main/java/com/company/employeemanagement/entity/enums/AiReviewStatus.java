package com.company.employeemanagement.entity.enums;

/**
 * Lifecycle status of a {@link com.company.employeemanagement.entity.TaskAiReview}.
 *
 * @author Employee Management Portal Team
 */
public enum AiReviewStatus {

    /** Analysis has been requested but not yet started. */
    PENDING,

    /** Analysis is actively being processed by the AI provider. */
    PROCESSING,

    /** Analysis completed successfully; results are available. */
    COMPLETED,

    /** Analysis failed; see {@code errorMessage} for details. */
    FAILED
}
