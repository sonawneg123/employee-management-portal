package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a department record returned to API consumers.
 *
 * @param id             UUID primary key of the department
 * @param name           human-readable department name
 * @param code           unique short department code
 * @param employeeCount  number of employees currently assigned to this department
 * @param createdAt      record creation timestamp
 * @param updatedAt      record last-modified timestamp
 * @param createdBy      email of the principal who created this department, or {@code "SYSTEM"}
 * @param updatedBy      email of the principal who last modified this department, or {@code "SYSTEM"}
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Department record as returned by the API")
public record DepartmentResponse(

        @Schema(description = "UUID of the department",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "Department name", example = "Engineering")
        String name,

        @Schema(description = "Short department code", example = "ENG")
        String code,

        @Schema(description = "Number of employees assigned to this department", example = "12")
        long employeeCount,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Email of the principal who created this department, or SYSTEM",
                example = "admin@example.com")
        String createdBy,

        @Schema(description = "Email of the principal who last modified this department, or SYSTEM",
                example = "hr@example.com")
        String updatedBy
) {
}
