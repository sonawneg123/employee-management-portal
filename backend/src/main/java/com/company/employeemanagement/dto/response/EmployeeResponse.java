package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing an employee record returned to API consumers.
 *
 * <p>Entities are never exposed directly. This record is the sole
 * publicly visible representation of an {@link com.company.employeemanagement.entity.Employee}.
 *
 * @param id              UUID primary key of the employee
 * @param employeeCode    unique HR-assigned employee code
 * @param departmentId    UUID of the associated department
 * @param departmentName  human-readable department name
 * @param userId          UUID of the linked user account, or {@code null}
 * @param firstName       first name sourced from the linked user
 * @param lastName        last name sourced from the linked user
 * @param email           email sourced from the linked user
 * @param jobTitle        current job title
 * @param phone           contact phone number
 * @param address         mailing or physical address
 * @param dateOfJoining   date the employee joined the company
 * @param salary          gross salary
 * @param status          current employment status
 * @param createdAt       record creation timestamp
 * @param updatedAt       record last-updated timestamp
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Employee record as returned by the API")
public record EmployeeResponse(

        @Schema(description = "UUID of the employee",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Unique HR-assigned employee code", example = "EMP-0001")
        String employeeCode,

        @Schema(description = "UUID of the employee's department",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID departmentId,

        @Schema(description = "Name of the employee's department", example = "Engineering")
        String departmentName,

        @Schema(description = "UUID of the linked user account",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "First name", example = "John")
        String firstName,

        @Schema(description = "Last name", example = "Doe")
        String lastName,

        @Schema(description = "Email address", example = "john.doe@example.com")
        String email,

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

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt
) {
}
