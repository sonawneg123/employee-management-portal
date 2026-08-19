package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Response DTO for the {@code GET /api/analytics/tasks} endpoint.
 *
 * <p>Contains task status breakdown and completion rate.
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Task analytics data for the selected scope")
public record AnalyticsTasksResponse(

        @Schema(description = "Total tasks in scope", example = "95")
        long totalTasks,

        @Schema(description = "COMPLETED tasks", example = "60")
        long completedTasks,

        @Schema(description = "ASSIGNED tasks (not yet started)", example = "10")
        long assignedTasks,

        @Schema(description = "IN_PROGRESS tasks", example = "12")
        long inProgressTasks,

        @Schema(description = "SUBMITTED tasks (pending review)", example = "3")
        long submittedTasks,

        @Schema(description = "Overdue tasks (past due date, not completed)", example = "7")
        long overdueTasks,

        @Schema(description = "DRAFT tasks", example = "3")
        long draftTasks,

        @Schema(description = "Task completion rate (completed/total) as 0-1", example = "0.63")
        double completionRate,

        @Schema(description = "Task status distribution for charts")
        List<TaskStatusBreakdown> statusBreakdown
) {

    /**
     * Count of tasks for a specific status.
     *
     * @param status The task status name.
     * @param count  Number of tasks with this status.
     */
    @Schema(description = "Task count for a specific status")
    public record TaskStatusBreakdown(
            @Schema(description = "Task status", example = "COMPLETED")
            String status,

            @Schema(description = "Count of tasks", example = "60")
            long count
    ) {}
}
