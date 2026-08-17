package com.company.employeemanagement.entity.enums;

/**
 * The AI's advisory recommendation for how a manager should handle a submission.
 *
 * <p><strong>Important:</strong> this is purely advisory — the manager always
 * makes the final decision. The AI must never automatically approve or reject.
 *
 * @author Employee Management Portal Team
 */
public enum AiRecommendedAction {

    /** AI analysis suggests the submission meets the requirements. */
    APPROVE,

    /** AI analysis suggests the submission has gaps that need correction. */
    REQUEST_CHANGES,

    /** AI analysis is inconclusive; manager should review manually. */
    MANUAL_REVIEW
}
