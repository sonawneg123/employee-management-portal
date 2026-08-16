package com.company.employeemanagement.entity.enums;

/**
 * Priority levels for company tasks, ordered from lowest to highest urgency.
 *
 * @author Employee Management Portal Team
 */
public enum TaskPriority {

    /** Lowest urgency — can be addressed when time allows. */
    LOW,

    /** Normal priority — typical work item. */
    MEDIUM,

    /** Elevated urgency — should be addressed soon. */
    HIGH,

    /** Highest urgency — requires immediate attention. */
    URGENT
}
