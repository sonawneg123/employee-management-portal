package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * Response DTO for the task management dashboard statistics endpoint.
 *
 * @param totalTasks         Total number of tasks
 * @param countsByStatus     Map of TaskStatus name → count
 * @param overdueCount       Number of non-completed tasks past their due date
 * @param urgentCount        Number of tasks with URGENT priority that are not completed
 * @param completionPercentage Percentage of all tasks that are COMPLETED (0–100)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Aggregate statistics for the task management dashboard")
public record TaskDashboardStatsResponse(

        @Schema(description = "Total number of tasks in the system")
        long totalTasks,

        @Schema(description = "Task counts grouped by status, e.g. {ASSIGNED: 5, IN_PROGRESS: 3}")
        Map<String, Long> countsByStatus,

        @Schema(description = "Number of non-completed tasks whose due date has passed")
        long overdueCount,

        @Schema(description = "Number of URGENT-priority tasks that are not yet completed")
        long urgentCount,

        @Schema(description = "Completion percentage (completed / total * 100)", example = "42.5")
        double completionPercentage
) {
}
