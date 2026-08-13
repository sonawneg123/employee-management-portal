package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.EmployeeStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for updating an existing employee record.
 *
 * <p>All fields are required — this is a full replacement (PUT semantics).
 * Clients wishing to update only specific fields should use a PATCH endpoint
 * (to be introduced in a future phase).
 *
 * @param firstName     updated first name
 * @param lastName      updated last name
 * @param departmentId  UUID of the employee's department
 * @param jobTitle      updated job title
 * @param phone         updated contact phone number
 * @param address       updated mailing or physical address
 * @param dateOfJoining updated joining date
 * @param salary        updated gross salary
 * @param status        updated employment status
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to update an existing employee record (full replacement)")
public record UpdateEmployeeRequest(

        @Schema(description = "First name of the employee", example = "John")
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Last name of the employee", example = "Doe")
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "UUID of the department", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @Schema(description = "Job title of the employee", example = "Principal Engineer")
        @NotBlank(message = "Job title is required")
        @Size(max = 150, message = "Job title must not exceed 150 characters")
        String jobTitle,

        @Schema(description = "Contact phone number", example = "+1-555-0100")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,

        @Schema(description = "Mailing or physical address", example = "456 Oak Ave, Springfield")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Schema(description = "Date the employee officially joined", example = "2024-01-15")
        @NotNull(message = "Date of joining is required")
        @PastOrPresent(message = "Date of joining must not be in the future")
        LocalDate dateOfJoining,

        @Schema(description = "Gross salary amount", example = "90000.00")
        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Salary must be zero or positive")
        BigDecimal salary,

        @Schema(description = "Employment status", example = "ACTIVE")
        @NotNull(message = "Status is required")
        EmployeeStatus status
) {
}
