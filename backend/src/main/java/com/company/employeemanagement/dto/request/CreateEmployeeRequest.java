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
 * Request DTO for creating a new employee record.
 *
 * <p>The {@code userId} field is optional — an employee record can be
 * created before a portal user account exists. {@code firstName} and
 * {@code lastName} are stored directly on the employee record so that
 * HR-created employees have a displayable name even without a user account.
 *
 * @param userId        optional UUID of an existing {@link com.company.employeemanagement.entity.User}
 * @param firstName     employee first name
 * @param lastName      employee last name
 * @param employeeCode  unique short identifier assigned by HR (e.g., {@code EMP-0001})
 * @param departmentId  UUID of the department this employee belongs to
 * @param jobTitle      current job title of the employee
 * @param phone         optional contact phone number
 * @param address       optional mailing or physical address
 * @param dateOfJoining date the employee joined the company
 * @param salary        gross monthly salary
 * @param status        initial employment status
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to create a new employee record")
public record CreateEmployeeRequest(

        @Schema(description = "UUID of an existing user account to link to this employee",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID userId,

        @Schema(description = "First name of the employee", example = "John")
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @Schema(description = "Last name of the employee", example = "Doe")
        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        @Schema(description = "Unique employee code assigned by HR", example = "EMP-0001")
        @NotBlank(message = "Employee code is required")
        @Size(max = 20, message = "Employee code must not exceed 20 characters")
        String employeeCode,

        @Schema(description = "UUID of the department", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Department ID is required")
        UUID departmentId,

        @Schema(description = "Job title of the employee", example = "Senior Software Engineer")
        @NotBlank(message = "Job title is required")
        @Size(max = 150, message = "Job title must not exceed 150 characters")
        String jobTitle,

        @Schema(description = "Contact phone number", example = "+1-555-0100")
        @Size(max = 20, message = "Phone number must not exceed 20 characters")
        String phone,

        @Schema(description = "Mailing or physical address", example = "123 Main St, Springfield")
        @Size(max = 255, message = "Address must not exceed 255 characters")
        String address,

        @Schema(description = "Date the employee officially joined", example = "2024-01-15")
        @NotNull(message = "Date of joining is required")
        @PastOrPresent(message = "Date of joining must not be in the future")
        LocalDate dateOfJoining,

        @Schema(description = "Gross salary amount", example = "75000.00")
        @NotNull(message = "Salary is required")
        @DecimalMin(value = "0.0", inclusive = true, message = "Salary must be zero or positive")
        BigDecimal salary,

        @Schema(description = "Initial employment status", example = "ACTIVE")
        @NotNull(message = "Status is required")
        EmployeeStatus status
) {
}
