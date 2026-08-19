package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /api/analytics/departments} endpoint.
 *
 * <p>Provides per-department headcount and analytics breakdown.
 * Accessible to ADMIN, HR, and MANAGER roles.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Department-level analytics with headcount")
public record AnalyticsDepartmentsResponse(

        @Schema(description = "Total number of departments", example = "8")
        long totalDepartments,

        @Schema(description = "Per-department breakdown")
        List<DepartmentStat> departments
) {

    /**
     * Analytics for a single department.
     *
     * @param departmentId   UUID of the department.
     * @param departmentName Department display name.
     * @param departmentCode Department short code.
     * @param headcount      Total employees in this department.
     * @param activeCount    Active employees in this department.
     * @param onLeaveCount   Employees currently on leave.
     */
    @Schema(description = "Analytics for a single department")
    public record DepartmentStat(
            @Schema(description = "Department UUID", example = "a1b2c3d4-...")
            String departmentId,

            @Schema(description = "Department name", example = "Engineering")
            String departmentName,

            @Schema(description = "Department short code", example = "ENG")
            String departmentCode,

            @Schema(description = "Total headcount", example = "25")
            long headcount,

            @Schema(description = "Active employees", example = "23")
            long activeCount,

            @Schema(description = "Employees currently on approved leave today", example = "2")
            long onLeaveCount
    ) {}
}
