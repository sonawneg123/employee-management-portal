package com.company.employeemanagement.entity.enums;

/**
 * Review/approval status of a {@link com.company.employeemanagement.entity.TaskSubmission}.
 *
 * <p>This is separate from {@link TaskStatus} — a submission has its own lifecycle
 * that maps onto the parent task's status transitions, but is conceptually distinct:
 * a task moves through states while a submission captures what the manager decided
 * about a specific piece of submitted work.
 *
 * @author Employee Management Portal Team
 */
public enum SubmissionStatus {

    /**
     * The employee has submitted the work and is awaiting manager review.
     * Corresponds to task status {@link TaskStatus#SUBMITTED}.
     */
    PENDING_REVIEW,

    /**
     * The manager has approved the submission — the task is complete.
     * Corresponds to task status {@link TaskStatus#COMPLETED}.
     */
    APPROVED,

    /**
     * The manager has reviewed the submission and requires changes before approval.
     * Corresponds to task status {@link TaskStatus#IN_PROGRESS} (reverted).
     */
    CHANGES_REQUESTED
}
