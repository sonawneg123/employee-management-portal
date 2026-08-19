package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.AnalyticsAttendanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsDepartmentsResponse;
import com.company.employeemanagement.dto.response.AnalyticsLeavesResponse;
import com.company.employeemanagement.dto.response.AnalyticsPerformanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsSummaryResponse;
import com.company.employeemanagement.dto.response.AnalyticsTasksResponse;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service contract for the HR Analytics Dashboard (Phase 8A).
 *
 * <p>All methods perform read-only aggregations. Authorization (RBAC + IDOR
 * scoping) is applied inside each implementation method — callers must pass
 * the authenticated user's employeeId where applicable.
 *
 * @author Employee Management Portal Team
 */
public interface AnalyticsService {

    /**
     * Returns the overall HR analytics summary for the given filters.
     *
     * @param from         inclusive start date for time-based metrics
     * @param to           inclusive end date
     * @param departmentId optional department filter (null = all departments)
     * @param employeeId   optional employee filter — used when scoping to a
     *                     single employee (e.g., EMPLOYEE role)
     * @return aggregated summary KPIs
     */
    AnalyticsSummaryResponse getSummary(LocalDate from, LocalDate to,
                                         UUID departmentId, UUID employeeId);

    /**
     * Returns detailed attendance analytics for the given filters.
     *
     * @param from         inclusive start date
     * @param to           inclusive end date
     * @param departmentId optional department filter
     * @param employeeId   optional employee filter
     * @return attendance breakdown and daily trend
     */
    AnalyticsAttendanceResponse getAttendance(LocalDate from, LocalDate to,
                                               UUID departmentId, UUID employeeId);

    /**
     * Returns leave request analytics for the given filters.
     *
     * @param from         inclusive start date (request creation)
     * @param to           inclusive end date
     * @param departmentId optional department filter
     * @param employeeId   optional employee filter
     * @return leave breakdown by status and type, plus monthly trend
     */
    AnalyticsLeavesResponse getLeaves(LocalDate from, LocalDate to,
                                       UUID departmentId, UUID employeeId);

    /**
     * Returns task analytics for the given filters.
     *
     * @param departmentId optional department filter
     * @param employeeId   optional employee filter
     * @return task status breakdown and completion rate
     */
    AnalyticsTasksResponse getTasks(UUID departmentId, UUID employeeId);

    /**
     * Returns AI performance evaluation analytics.
     *
     * <p>This endpoint is restricted to ADMIN, HR, and MANAGER roles —
     * EMPLOYEE users must not access organisation-wide AI scores.
     *
     * @param from inclusive start date (AI review completedAt)
     * @param to   inclusive end date
     * @return AI score aggregates and trend
     */
    AnalyticsPerformanceResponse getPerformance(LocalDate from, LocalDate to);

    /**
     * Returns per-department headcount analytics.
     *
     * <p>This endpoint is restricted to ADMIN, HR, and MANAGER roles.
     *
     * @return department headcounts with active/on-leave breakdowns
     */
    AnalyticsDepartmentsResponse getDepartments();
}
