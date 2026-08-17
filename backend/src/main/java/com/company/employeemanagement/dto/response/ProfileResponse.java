package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO representing the authenticated user's own profile.
 *
 * <p>Combines user account fields with the linked employee record (if present).
 *
 * @param userId          UUID of the user account
 * @param email           login email address
 * @param firstName       first name
 * @param lastName        last name
 * @param roles           comma-separated list of the user's roles
 * @param employeeId      UUID of the linked employee record, or {@code null}
 * @param employeeCode    HR-assigned employee code, or {@code null}
 * @param departmentId    UUID of the department, or {@code null}
 * @param departmentName  name of the department, or {@code null}
 * @param jobTitle        current job title, or {@code null}
 * @param phone           contact phone number, or {@code null}
 * @param address         mailing address, or {@code null}
 * @param dateOfJoining   date the employee joined the company, or {@code null}
 * @param salary          gross salary, or {@code null}
 * @param status          current employment status, or {@code null}
 * @param profilePhotoUrl relative URL to retrieve the profile photo, or {@code null}
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Authenticated user's own profile combining account and employee information")
public record ProfileResponse(

        @Schema(description = "UUID of the user account",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "Login email address", example = "jane.smith@example.com")
        String email,

        @Schema(description = "First name", example = "Jane")
        String firstName,

        @Schema(description = "Last name", example = "Smith")
        String lastName,

        @Schema(description = "Comma-separated list of roles", example = "ROLE_EMPLOYEE")
        String roles,

        @Schema(description = "UUID of the linked employee record")
        UUID employeeId,

        @Schema(description = "HR-assigned employee code", example = "EMP-0001")
        String employeeCode,

        @Schema(description = "UUID of the employee's department")
        UUID departmentId,

        @Schema(description = "Name of the employee's department", example = "Engineering")
        String departmentName,

        @Schema(description = "Job title", example = "Senior Software Engineer")
        String jobTitle,

        @Schema(description = "Contact phone number", example = "+1-555-0100")
        String phone,

        @Schema(description = "Mailing or physical address", example = "123 Main St")
        String address,

        @Schema(description = "Date of joining", example = "2024-01-15")
        LocalDate dateOfJoining,

        @Schema(description = "Gross salary", example = "75000.00")
        BigDecimal salary,

        @Schema(description = "Employment status", example = "ACTIVE")
        EmployeeStatus status,

        @Schema(description = "URL to retrieve the profile photo, or null if no photo uploaded",
                example = "/api/profile/photo")
        String profilePhotoUrl
) {
}
