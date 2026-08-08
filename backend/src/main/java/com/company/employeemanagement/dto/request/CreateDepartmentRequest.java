package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating a new department.
 *
 * @param name human-readable department name (e.g., "Engineering")
 * @param code unique short code for the department (e.g., "ENG") — uppercase letters and hyphens only
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to create a new department")
public record CreateDepartmentRequest(

        @Schema(description = "Human-readable department name", example = "Engineering")
        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String name,

        @Schema(description = "Unique short department code (uppercase letters, digits, hyphens)",
                example = "ENG")
        @NotBlank(message = "Department code is required")
        @Size(max = 20, message = "Department code must not exceed 20 characters")
        @Pattern(regexp = "^[A-Z0-9_-]+$",
                 message = "Department code may only contain uppercase letters, digits, underscores, and hyphens")
        String code
) {
}
