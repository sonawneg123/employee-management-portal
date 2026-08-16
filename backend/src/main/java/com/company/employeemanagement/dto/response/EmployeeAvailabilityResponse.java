package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO for the employee availability endpoint used in the task assignment form.
 *
 * @param employeeId    UUID of the employee
 * @param employeeName  Display name of the employee
 * @param employeeCode  Employee code
 * @param checkedIn     Whether the employee is currently checked in (has a today attendance
 *                      record with no check-out time)
 * @param activeTasks   Number of active (ASSIGNED + IN_PROGRESS) tasks for this employee
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Employee availability status for task assignment")
public record EmployeeAvailabilityResponse(

        @Schema(description = "UUID of the employee")
        UUID employeeId,

        @Schema(description = "Display name of the employee", example = "Jane Doe")
        String employeeName,

        @Schema(description = "Employee code", example = "EMP-0042")
        String employeeCode,

        @Schema(description = "True if the employee is currently checked in")
        boolean checkedIn,

        @Schema(description = "Number of active tasks assigned to this employee")
        long activeTasks
) {
}
