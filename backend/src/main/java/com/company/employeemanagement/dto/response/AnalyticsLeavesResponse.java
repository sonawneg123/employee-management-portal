package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /api/analytics/leaves} endpoint.
 *
 * <p>Contains leave request breakdown by status and type, plus a monthly trend.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Leave analytics data for the selected period")
public record AnalyticsLeavesResponse(

        @Schema(description = "Total leave requests in the period", example = "80")
        long totalRequests,

        @Schema(description = "PENDING requests", example = "12")
        long pendingCount,

        @Schema(description = "APPROVED requests", example = "55")
        long approvedCount,

        @Schema(description = "REJECTED requests", example = "8")
        long rejectedCount,

        @Schema(description = "CANCELLED requests", example = "5")
        long cancelledCount,

        @Schema(description = "Leave utilization: approved / total (0-1)", example = "0.69")
        double leaveUtilizationRate,

        @Schema(description = "Breakdown by leave type")
        List<LeaveTypeBreakdown> byType,

        @Schema(description = "Monthly leave request trend")
        List<MonthlyLeaveTrend> trend
) {

    /**
     * Leave count for a specific leave type.
     *
     * @param leaveType The leave type name (e.g., ANNUAL, SICK, etc.)
     * @param count     Number of requests of this type.
     */
    @Schema(description = "Leave count for a specific leave type")
    public record LeaveTypeBreakdown(
            @Schema(description = "Leave type (ANNUAL, SICK, CASUAL, etc.)", example = "ANNUAL")
            String leaveType,

            @Schema(description = "Count of requests", example = "30")
            long count
    ) {}

    /**
     * Monthly leave count trend point.
     *
     * @param month  Month label (e.g., "2024-06").
     * @param total  Total requests that month.
     * @param approved Approved requests that month.
     */
    @Schema(description = "Monthly leave trend point")
    public record MonthlyLeaveTrend(
            @Schema(description = "Month label YYYY-MM", example = "2024-06")
            String month,

            @Schema(description = "Total requests in this month", example = "15")
            long total,

            @Schema(description = "Approved requests in this month", example = "10")
            long approved
    ) {}
}
