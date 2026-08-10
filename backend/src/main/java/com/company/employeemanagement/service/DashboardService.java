package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.ActivityItemResponse;
import com.company.employeemanagement.dto.response.DashboardChartsResponse;
import com.company.employeemanagement.dto.response.DashboardSummaryResponse;

import java.util.List;

/**
 * Service contract for dashboard aggregation operations.
 *
 * <p>All methods are read-only — they aggregate counts and lists from
 * multiple repositories without modifying any state.
 *
 * @author Employee Management Portal Team
 */
public interface DashboardService {

    /**
     * Returns the KPI summary for the dashboard header tiles.
     *
     * @return a {@link DashboardSummaryResponse} with all KPI counters and trends
     */
    DashboardSummaryResponse getSummary();

    /**
     * Returns the bundled chart datasets for the dashboard visualisations.
     *
     * @return a {@link DashboardChartsResponse} containing department distribution,
     *         employee status breakdown, and the last-14-days attendance trend
     */
    DashboardChartsResponse getCharts();

    /**
     * Returns a limited list of recent portal activity events.
     *
     * @param limit maximum number of events to return (must be &gt; 0)
     * @return the most recent activity items, newest first
     */
    List<ActivityItemResponse> getActivity(int limit);
}
