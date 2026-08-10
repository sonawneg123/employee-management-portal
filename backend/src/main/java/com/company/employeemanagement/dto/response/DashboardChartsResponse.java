package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /dashboard/charts} endpoint.
 *
 * <p>Bundles the three chart datasets in a single response to reduce
 * the number of round-trips for the dashboard page.
 *
 * @param departmentDistribution  Employee headcount per department — used
 *                                 by the pie chart.
 * @param employeeStatusBreakdown Employee count grouped by status —
 *                                 used by the bar chart.
 * @param attendanceTrend         Last-14-days daily attendance counts —
 *                                 used by the line chart.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Bundled chart datasets for the dashboard visualisations")
public record DashboardChartsResponse(

        @Schema(description = "Headcount per department for the pie chart")
        List<DepartmentDistribution> departmentDistribution,

        @Schema(description = "Employee count grouped by status for the bar chart")
        List<EmployeeStatusCount> employeeStatusBreakdown,

        @Schema(description = "Daily attendance counts for the last 14 days")
        List<AttendanceTrendPoint> attendanceTrend
) {

    /**
     * Department name, employee count, and short code.
     *
     * @param name  Human-readable department name.
     * @param count Number of employees in the department.
     * @param code  Short department code (e.g. {@code "ENG"}).
     */
    @Schema(description = "Employee headcount for a single department")
    public record DepartmentDistribution(

            @Schema(description = "Department name", example = "Engineering")
            String name,

            @Schema(description = "Department code", example = "ENG")
            String code,

            @Schema(description = "Employee headcount", example = "20")
            long count
    ) {
    }

    /**
     * Employee status enum value and the number of employees in that status.
     *
     * @param status The string representation of {@link com.company.employeemanagement.entity.enums.EmployeeStatus}.
     * @param count  Number of employees with that status.
     */
    @Schema(description = "Employee count for a single status")
    public record EmployeeStatusCount(

            @Schema(description = "Employee status key", example = "ACTIVE")
            String status,

            @Schema(description = "Employee count", example = "40")
            long count
    ) {
    }

    /**
     * A single day's present/absent counts.
     *
     * @param date    ISO-8601 date string ({@code "YYYY-MM-DD"}).
     * @param present Number of employees marked PRESENT.
     * @param absent  Number of employees absent (total employees − present).
     */
    @Schema(description = "Daily attendance counts for one calendar day")
    public record AttendanceTrendPoint(

            @Schema(description = "Date in YYYY-MM-DD format", example = "2024-01-15")
            String date,

            @Schema(description = "Present count", example = "38")
            long present,

            @Schema(description = "Absent count", example = "4")
            long absent
    ) {
    }
}
