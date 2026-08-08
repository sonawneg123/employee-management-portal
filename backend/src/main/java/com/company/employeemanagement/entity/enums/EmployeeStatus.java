package com.company.employeemanagement.entity.enums;

/**
 * Represents the employment status of an {@link com.company.employeemanagement.entity.Employee}.
 *
 * @author Employee Management Portal Team
 */
public enum EmployeeStatus {

    /** Employee is actively working. */
    ACTIVE,

    /** Employee is no longer active but not yet formally terminated. */
    INACTIVE,

    /** Employee is currently on an approved leave of absence. */
    ON_LEAVE,

    /** Employment has been formally ended. */
    TERMINATED
}
