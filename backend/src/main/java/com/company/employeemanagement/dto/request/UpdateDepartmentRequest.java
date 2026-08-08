package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for updating an existing department (full replacement / PUT semantics).
 *
 * @param name updated human-readable department name
 * @param code updated unique short department code
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload required to update an existing department")
public record UpdateDepartmentRequest(

        @Schema(description = "Updated department name", example = "Software Engineering")
        @NotBlank(message = "Department name is required")
        @Size(max = 100, message = "Department name must not exceed 100 characters")
        String name,

        @Schema(description = "Updated unique short department code", example = "SWE")
        @NotBlank(message = "Department code is required")
        @Size(max = 20, message = "Department code must not exceed 20 characters")
        @Pattern(regexp = "^[A-Z0-9_-]+$",
                 message = "Department code may only contain uppercase letters, digits, underscores, and hyphens")
        String code
) {
}
