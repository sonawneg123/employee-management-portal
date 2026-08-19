package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /api/analytics/summary} endpoint.
 *
 * <p>Combines employee, attendance, leave, task, and AI performance KPIs
 * into a single payload for the analytics dashboard.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Aggregate HR analytics summary across all domains")
public record AnalyticsSummaryResponse(

        // ── Employee KPIs ─────────────────────────────────────────────────────

        @Schema(description = "Total employee headcount", example = "120")
        long totalEmployees,

        @Schema(description = "Employees with ACTIVE status", example = "110")
        long activeEmployees,

        @Schema(description = "Employees with INACTIVE or DISABLED status", example = "10")
        long inactiveEmployees,

        @Schema(description = "Employees currently on approved leave today", example = "5")
        long employeesOnLeave,

        @Schema(description = "Employees who joined in the current calendar month", example = "3")
        long newEmployees,

        // ── Attendance KPIs ───────────────────────────────────────────────────

        @Schema(description = "Overall attendance rate for the period as a fraction 0-1", example = "0.87")
        double attendanceRate,

        @Schema(description = "Total PRESENT records in the period", example = "2400")
        long presentCount,

        @Schema(description = "Total ABSENT records in the period", example = "200")
        long absentCount,

        @Schema(description = "Total HALF_DAY records in the period", example = "50")
        long halfDayCount,

        @Schema(description = "Total ON_LEAVE records in the period", example = "150")
        long onLeaveCount,

        // ── Leave KPIs ────────────────────────────────────────────────────────

        @Schema(description = "Total leave requests in the period", example = "80")
        long totalLeaveRequests,

        @Schema(description = "Pending leave requests", example = "12")
        long pendingLeaveRequests,

        @Schema(description = "Approved leave requests in the period", example = "55")
        long approvedLeaveRequests,

        @Schema(description = "Rejected leave requests in the period", example = "13")
        long rejectedLeaveRequests,

        // ── Task KPIs ─────────────────────────────────────────────────────────

        @Schema(description = "Total tasks visible to the requester", example = "95")
        long totalTasks,

        @Schema(description = "Completed tasks", example = "60")
        long completedTasks,

        @Schema(description = "Pending/in-progress tasks", example = "25")
        long pendingTasks,

        @Schema(description = "Overdue tasks (past due date, not completed)", example = "10")
        long overdueTasks,

        @Schema(description = "Task completion rate as a fraction 0-1", example = "0.63")
        double taskCompletionRate,

        // ── AI Performance KPIs ───────────────────────────────────────────────

        @Schema(description = "Average AI evaluation score (0-100); -1 if no evaluations", example = "78.5")
        double avgAiScore,

        @Schema(description = "Number of completed AI evaluations", example = "42")
        long completedAiEvaluations,

        @Schema(description = "Number of failed AI evaluations", example = "3")
        long failedAiEvaluations,

        // ── Trend data (serialized as lists for sparklines) ───────────────────

        @Schema(description = "Attendance trend: daily rates for the period")
        List<TrendPoint> attendanceTrend,

        @Schema(description = "AI performance score trend: average score per period bucket")
        List<TrendPoint> aiScoreTrend
) {

    /**
     * A single data point in a trend series.
     *
     * @param label Human-readable label for the x-axis (e.g., date string)
     * @param value Numeric value for the y-axis
     */
    @Schema(description = "A single trend data point")
    public record TrendPoint(
            @Schema(description = "X-axis label (date or period)", example = "2024-06-01")
            String label,

            @Schema(description = "Numeric value", example = "0.85")
            double value
    ) {}
}
