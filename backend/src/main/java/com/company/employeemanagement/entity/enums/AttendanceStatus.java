package com.company.employeemanagement.entity.enums;

/**
 * Represents the attendance status for a single day's
 * {@link com.company.employeemanagement.entity.Attendance} record.
 *
 * @author Employee Management Portal Team
 */
public enum AttendanceStatus {

    /** Employee was present in the office or on site. */
    PRESENT,

    /** Employee did not attend and no leave was recorded. */
    ABSENT,

    /** Employee was present for only part of the working day. */
    HALF_DAY,

    /** Employee worked remotely from home. */
    WORK_FROM_HOME,

    /** Employee was absent due to an approved leave request. */
    ON_LEAVE
}
