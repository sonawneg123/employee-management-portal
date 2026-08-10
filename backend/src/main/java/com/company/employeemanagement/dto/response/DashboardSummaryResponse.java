package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO for the {@code GET /dashboard/summary} endpoint.
 *
 * <p>Contains KPI counters and month-over-month trend deltas for the
 * dashboard summary tile row.
 *
 * @param totalEmployees    Total number of employee records.
 * @param totalDepartments  Total number of departments.
 * @param pendingLeaves     Leave requests in {@code PENDING} status.
 * @param activeEmployees   Employees with {@code ACTIVE} status.
 * @param presentToday      Employees with an attendance record marked
 *                          {@code PRESENT} on today's date.
 * @param onLeaveToday      Employees with an {@code APPROVED} leave whose
 *                          date range covers today.
 * @param newThisMonth      Employees whose {@code dateOfJoining} falls in
 *                          the current calendar month.
 * @param trendEmployees    Change in total headcount vs the previous month
 *                          (positive = growth).
 * @param trendLeaves       Change in pending leave count vs 7 days ago.
 * @param trendAttendance   Fractional change in today's attendance rate vs
 *                          yesterday (e.g. {@code 0.05} = +5 pp).
 * @param attendanceRate    Today's attendance rate as a fraction 0–1.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "KPI summary for the dashboard header tiles")
public record DashboardSummaryResponse(

        @Schema(description = "Total number of employees", example = "42")
        long totalEmployees,

        @Schema(description = "Total number of departments", example = "5")
        long totalDepartments,

        @Schema(description = "Leave requests awaiting approval", example = "3")
        long pendingLeaves,

        @Schema(description = "Employees with ACTIVE status", example = "40")
        long activeEmployees,

        @Schema(description = "Employees marked PRESENT today", example = "38")
        long presentToday,

        @Schema(description = "Employees on approved leave today", example = "2")
        long onLeaveToday,

        @Schema(description = "Employees who joined this calendar month", example = "4")
        long newThisMonth,

        @Schema(description = "Month-over-month headcount change", example = "2")
        long trendEmployees,

        @Schema(description = "Change in pending leaves vs 7 days ago", example = "-1")
        long trendLeaves,

        @Schema(description = "Attendance rate change vs yesterday (fraction)", example = "0.05")
        double trendAttendance,

        @Schema(description = "Today's attendance rate as a fraction 0–1", example = "0.90")
        double attendanceRate
) {
}
