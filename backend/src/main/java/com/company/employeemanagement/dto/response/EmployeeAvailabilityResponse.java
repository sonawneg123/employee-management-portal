package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO for the employee availability endpoint used in the task assignment form.
 *
 * <p>Availability logic:
 * <pre>
 *   availableForTaskAssignmentToday =
 *       NOT disabled
 *       AND checkedInToday
 *       AND NOT checkedOutToday
 *       AND NOT onApprovedLeaveToday
 * </pre>
 *
 * <p>Priority of unavailability reasons (highest to lowest):
 * <ol>
 *   <li>DISABLED — employee account has been administratively disabled</li>
 *   <li>APPROVED_LEAVE — employee has an approved leave covering today</li>
 *   <li>CHECKED_OUT — employee checked out today</li>
 *   <li>NOT_CHECKED_IN — employee has not checked in today</li>
 * </ol>
 *
 * @param employeeId          UUID of the employee
 * @param employeeName        Display name of the employee
 * @param employeeCode        Employee code
 * @param checkedIn           Whether the employee has an active check-in today
 *                            (today attendance record exists with null check-out time)
 * @param activeTasks         Number of active (ASSIGNED + IN_PROGRESS) tasks for this employee
 * @param onApprovedLeaveToday Whether the employee has an APPROVED leave request
 *                            covering today's date
 * @param availableToday      Computed convenience flag: true iff NOT disabled AND checkedIn AND NOT onApprovedLeaveToday
 * @param disabled            Whether the employee has been administratively disabled
 * @param profilePhotoUrl     Relative URL to retrieve the employee's profile photo, or {@code null}
 * @param unavailabilityReason Short human-readable reason key for unavailability, or {@code null}
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

        @Schema(description = "True if the employee has an active check-in today (no check-out)")
        boolean checkedIn,

        @Schema(description = "Number of active tasks assigned to this employee")
        long activeTasks,

        @Schema(description = "True if the employee has an APPROVED leave covering today")
        boolean onApprovedLeaveToday,

        @Schema(description = "True if the employee is available for new task assignment today")
        boolean availableToday,

        @Schema(description = "True if the employee has been administratively disabled")
        boolean disabled,

        @Schema(description = "URL to retrieve the employee's profile photo, or null if no photo uploaded",
                example = "/api/employees/3fa85f64-5717-4562-b3fc-2c963f66afa6/profile-photo")
        String profilePhotoUrl,

        @Schema(description = "Short key for why the employee is unavailable: DISABLED, APPROVED_LEAVE, CHECKED_OUT, NOT_CHECKED_IN, or null if available",
                example = "DISABLED")
        String unavailabilityReason
) {
}
