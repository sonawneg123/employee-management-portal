package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /api/analytics/attendance} endpoint.
 *
 * <p>Contains attendance breakdown counts and a daily trend series.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Attendance analytics data for the selected period")
public record AnalyticsAttendanceResponse(

        @Schema(description = "Total attendance records in the period", example = "2800")
        long totalRecords,

        @Schema(description = "PRESENT count", example = "2400")
        long presentCount,

        @Schema(description = "ABSENT count", example = "200")
        long absentCount,

        @Schema(description = "HALF_DAY count", example = "50")
        long halfDayCount,

        @Schema(description = "WORK_FROM_HOME count", example = "100")
        long workFromHomeCount,

        @Schema(description = "ON_LEAVE count", example = "50")
        long onLeaveCount,

        @Schema(description = "Overall attendance rate for the period as a fraction 0-1", example = "0.87")
        double attendanceRate,

        @Schema(description = "Daily attendance trend: date → present/total/absent")
        List<DailyAttendancePoint> trend
) {

    /**
     * One day's attendance metrics.
     *
     * @param date    The date in {@code YYYY-MM-DD} format.
     * @param present Number of PRESENT records on this date.
     * @param absent  Number of ABSENT records on this date.
     * @param total   Total records on this date.
     * @param rate    Attendance rate for this date (present/totalEmployees).
     */
    @Schema(description = "Attendance metrics for a single day")
    public record DailyAttendancePoint(
            @Schema(description = "Date in YYYY-MM-DD format", example = "2024-06-01")
            String date,

            @Schema(description = "PRESENT count", example = "95")
            long present,

            @Schema(description = "ABSENT count", example = "5")
            long absent,

            @Schema(description = "Total records on this date", example = "100")
            long total,

            @Schema(description = "Rate (present / total employees) for this date", example = "0.95")
            double rate
    ) {}
}
