package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * Response DTO representing the task workload for a single employee.
 *
 * @param employeeId   UUID of the employee
 * @param employeeName Display name of the employee
 * @param activeTasks  Number of tasks in ASSIGNED or IN_PROGRESS state
 * @param pendingReview Number of tasks in SUBMITTED state
 * @param overdue      Number of non-completed tasks past their due date
 * @param workloadLevel Computed workload level based on activeTasks count
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Employee workload summary")
public record WorkloadResponse(

        @Schema(description = "UUID of the employee")
        UUID employeeId,

        @Schema(description = "Display name of the employee", example = "Jane Doe")
        String employeeName,

        @Schema(description = "Active tasks (ASSIGNED + IN_PROGRESS)")
        long activeTasks,

        @Schema(description = "Tasks pending review (SUBMITTED)")
        long pendingReview,

        @Schema(description = "Overdue non-completed tasks")
        long overdue,

        @Schema(description = "Workload level: LOW, MEDIUM, HIGH, or CRITICAL")
        WorkloadLevel workloadLevel
) {

    /**
     * Workload classification based on active task count.
     */
    public enum WorkloadLevel {
        /** Fewer than 3 active tasks. */
        LOW,
        /** 3–5 active tasks. */
        MEDIUM,
        /** 6–8 active tasks. */
        HIGH,
        /** 9 or more active tasks. */
        CRITICAL
    }

    /**
     * Derives the workload level from the given number of active tasks.
     *
     * @param activeTasks current active task count
     * @return the corresponding {@link WorkloadLevel}
     */
    public static WorkloadLevel levelFrom(final long activeTasks) {
        if (activeTasks < 3) return WorkloadLevel.LOW;
        if (activeTasks < 6) return WorkloadLevel.MEDIUM;
        if (activeTasks < 9) return WorkloadLevel.HIGH;
        return WorkloadLevel.CRITICAL;
    }
}
