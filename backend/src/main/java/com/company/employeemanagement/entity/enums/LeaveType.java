package com.company.employeemanagement.entity.enums;

/**
 * Represents the category of leave that an employee may request.
 *
 * @author Employee Management Portal Team
 */
public enum LeaveType {

    /** Standard paid annual/vacation leave. */
    ANNUAL,

    /** Paid leave for medical illness or injury. */
    SICK,

    /** Maternity leave for the birth or adoption of a child. */
    MATERNITY,

    /** Paternity leave for the birth or adoption of a child. */
    PATERNITY,

    /** Unpaid leave approved by the company. */
    UNPAID,

    /** Emergency leave for urgent, unforeseen circumstances. */
    EMERGENCY,

    /** Leave taken for study, exams, or professional development. */
    STUDY,

    /** Any other leave type not covered by the above categories. */
    OTHER
}
