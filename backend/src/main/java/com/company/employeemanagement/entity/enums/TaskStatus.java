package com.company.employeemanagement.entity.enums;

/**
 * Lifecycle states for a company task.
 *
 * <p>Phase 6A establishes these states for domain model completeness.
 * Full workflow transitions (submissions, approvals, rejections) are
 * implemented in later phases.
 *
 * @author Employee Management Portal Team
 */
public enum TaskStatus {

    /** Task created but not yet assigned or activated. */
    DRAFT,

    /** Task has been assigned to an employee. */
    ASSIGNED,

    /** Employee has started working on the task. */
    IN_PROGRESS,

    /** Employee has submitted work for manager review. */
    SUBMITTED,

    /** Manager has accepted the completed work. */
    COMPLETED,

    /** Manager has requested changes from the employee. */
    CHANGES_REQUESTED,

    /** Task was rejected by the manager. */
    REJECTED
}
