package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO representing a user account in the admin user-management list.
 *
 * <p>For users with {@code ROLE_EMPLOYEE}, the {@code employee} field is
 * populated with the linked employee record details.
 *
 * @param id          UUID primary key of the user
 * @param email       unique email address / login credential
 * @param firstName   first name
 * @param lastName    last name
 * @param roles       list of role names assigned to this user (e.g. "ROLE_ADMIN")
 * @param isEnabled   whether the account can log in
 * @param isLocked    whether the account is temporarily locked
 * @param createdAt   account creation timestamp
 * @param employee    linked employee record summary (may be null for non-employee accounts)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "User account record as returned by the admin user-management API")
public record UserListResponse(

        @Schema(description = "UUID of the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Email address (login credential)", example = "admin@company.com")
        String email,

        @Schema(description = "First name", example = "Admin")
        String firstName,

        @Schema(description = "Last name", example = "User")
        String lastName,

        @Schema(description = "Assigned role names", example = "[\"ROLE_ADMIN\"]")
        List<String> roles,

        @Schema(description = "Whether the account is enabled", example = "true")
        boolean isEnabled,

        @Schema(description = "Whether the account is locked", example = "false")
        boolean isLocked,

        @Schema(description = "Account creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Linked employee record summary, populated for ROLE_EMPLOYEE accounts")
        EmployeeSummary employee
) {

    /**
     * Embedded summary of the linked employee record.
     *
     * @param id             UUID of the employee record
     * @param employeeCode   HR-assigned employee code (e.g. "EMP-0001")
     * @param departmentId   UUID of the department
     * @param departmentName name of the department
     * @param jobTitle       current job title
     * @param status         employment status
     */
    @Schema(description = "Linked employee record summary")
    public record EmployeeSummary(

            @Schema(description = "UUID of the employee record")
            UUID id,

            @Schema(description = "HR-assigned employee code", example = "EMP-0001")
            String employeeCode,

            @Schema(description = "UUID of the department")
            UUID departmentId,

            @Schema(description = "Department name", example = "Engineering")
            String departmentName,

            @Schema(description = "Job title", example = "Software Engineer")
            String jobTitle,

            @Schema(description = "Employment status", example = "ACTIVE")
            String status
    ) {}
}
