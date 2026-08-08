package com.company.employeemanagement.entity.enums;

/**
 * Represents the approval lifecycle status of a
 * {@link com.company.employeemanagement.entity.LeaveRequest}.
 *
 * @author Employee Management Portal Team
 */
public enum LeaveStatus {

    /** Request has been submitted and is awaiting HR / manager review. */
    PENDING,

    /** Request has been approved by an authorised reviewer. */
    APPROVED,

    /** Request has been rejected by an authorised reviewer. */
    REJECTED,

    /** Request was cancelled by the employee before a decision was made. */
    CANCELLED
}
