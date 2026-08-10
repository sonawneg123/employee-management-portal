package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.response.ActivityItemResponse;
import com.company.employeemanagement.dto.response.DashboardChartsResponse;
import com.company.employeemanagement.dto.response.DashboardSummaryResponse;
import com.company.employeemanagement.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing dashboard aggregation endpoints.
 *
 * <p>Base path: {@code /api/dashboard}
 *
 * <p>All three endpoints are read-only aggregations that combine data from
 * employees, departments, leave requests, and attendance. No state is modified.
 *
 * <p>Access is restricted to authenticated users with any of the four
 * application roles (ADMIN, HR, MANAGER, EMPLOYEE). The data returned is
 * organisation-wide — role-based filtering of individual rows is handled by
 * the frontend's role-aware layout, not by this API.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/dashboard")
@Tag(name = "Dashboard", description = "Aggregated KPI and chart data for the dashboard page")
@SecurityRequirement(name = "BearerAuth")
public class DashboardController {

    private final DashboardService dashboardService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param dashboardService the dashboard aggregation service
     */
    public DashboardController(final DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    // ── GET /dashboard/summary ─────────────────────────────────────────────────

    /**
     * Returns the KPI summary for the dashboard header tile row.
     *
     * <p>Contains total employee count, department count, pending-leave count,
     * attendance rate for today, and month-over-month trend deltas.
     *
     * @return a {@link DashboardSummaryResponse} with all KPI values
     */
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get dashboard KPI summary",
            description = "Returns aggregated KPI counters and trend deltas for the dashboard "
                        + "header tiles. Data is organisation-wide."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "KPI summary returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DashboardSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DashboardSummaryResponse> getSummary() {
        return ResponseEntity.ok(dashboardService.getSummary());
    }

    // ── GET /dashboard/charts ──────────────────────────────────────────────────

    /**
     * Returns bundled chart datasets for the dashboard visualisations.
     *
     * <p>Returns department distribution (pie chart), employee status breakdown
     * (bar chart), and last-14-days attendance trend (line chart) in a single
     * round-trip.
     *
     * @return a {@link DashboardChartsResponse} with all three chart datasets
     */
    @GetMapping(value = "/charts", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get dashboard chart datasets",
            description = "Returns department distribution, employee status breakdown, "
                        + "and last-14-days attendance trend in one request."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Chart datasets returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DashboardChartsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DashboardChartsResponse> getCharts() {
        return ResponseEntity.ok(dashboardService.getCharts());
    }

    // ── GET /dashboard/activity ────────────────────────────────────────────────

    /**
     * Returns the recent portal activity feed.
     *
     * <p>Returns at most {@code limit} activity items, derived from the most
     * recent leave-request events. Items are ordered newest first.
     *
     * @param limit maximum number of activity items to return (default 10, max 50)
     * @return a list of {@link ActivityItemResponse} items
     */
    @GetMapping(value = "/activity", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get recent activity feed",
            description = "Returns the most recent portal activity events, newest first."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activity feed returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<ActivityItemResponse>> getActivity(
            @Parameter(description = "Maximum number of activity items (1–50)", example = "10")
            @RequestParam(defaultValue = "10") final int limit) {
        return ResponseEntity.ok(dashboardService.getActivity(limit));
    }
}
