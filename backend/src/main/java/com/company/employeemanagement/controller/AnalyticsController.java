package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.response.AnalyticsAttendanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsDepartmentsResponse;
import com.company.employeemanagement.dto.response.AnalyticsLeavesResponse;
import com.company.employeemanagement.dto.response.AnalyticsPerformanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsSummaryResponse;
import com.company.employeemanagement.dto.response.AnalyticsTasksResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for the HR Analytics Dashboard (Phase 8A).
 *
 * <p>Base path: {@code /api/analytics}
 *
 * <p>Authorization model:
 * <ul>
 *   <li><strong>ADMIN / HR</strong>: full org-wide analytics with optional department filter.</li>
 *   <li><strong>MANAGER</strong>: same as HR — org-wide visibility (can filter by department).</li>
 *   <li><strong>EMPLOYEE</strong>: scoped to their own employee record only.
 *       Any attempt to supply a different {@code employeeId} is blocked server-side.</li>
 * </ul>
 *
 * <p>Performance and department endpoints are restricted to ADMIN, HR, and MANAGER.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/analytics")
@Tag(name = "Analytics", description = "HR Analytics Dashboard — Phase 8A")
@SecurityRequirement(name = "BearerAuth")
public class AnalyticsController {

    private static final int DEFAULT_DAYS_BACK = 30;

    private final AnalyticsService analyticsService;
    private final SecurityUtils    securityUtils;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param analyticsService the analytics aggregation service
     * @param securityUtils    security context helper
     */
    public AnalyticsController(
            final AnalyticsService analyticsService,
            final SecurityUtils    securityUtils) {
        this.analyticsService = analyticsService;
        this.securityUtils    = securityUtils;
    }

    // ── GET /analytics/summary ─────────────────────────────────────────────────

    /**
     * Returns the aggregated analytics summary for the dashboard KPI tiles.
     *
     * <p>EMPLOYEE role receives only their own personal data. ADMIN/HR/MANAGER
     * receive org-wide data (optionally filtered by department).
     *
     * @param from         start of the analysis period (defaults to 30 days ago)
     * @param to           end of the analysis period (defaults to today)
     * @param departmentId optional department UUID filter (privileged roles only)
     * @param employeeId   optional employee UUID filter (privileged roles only;
     *                     EMPLOYEE role is always scoped to themselves)
     * @return the analytics summary DTO
     */
    @GetMapping(value = "/summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get analytics summary KPIs",
            description = "Returns aggregate HR KPIs for employees, attendance, leaves, tasks, "
                        + "and AI performance. EMPLOYEE role receives only their own data."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Analytics summary returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticsSummaryResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AnalyticsSummaryResponse> getSummary(
            @Parameter(description = "Period start date (YYYY-MM-DD)", example = "2024-05-01")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @Parameter(description = "Period end date (YYYY-MM-DD)", example = "2024-05-31")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @Parameter(description = "Filter by department UUID (privileged roles only)")
            @RequestParam(required = false) UUID departmentId,
            @Parameter(description = "Filter by employee UUID (privileged roles only)")
            @RequestParam(required = false) UUID employeeId) {

        final LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        final LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_DAYS_BACK);

        final ScopeParams scope = resolveScope(departmentId, employeeId);

        return ResponseEntity.ok(analyticsService.getSummary(
                effectiveFrom, effectiveTo, scope.departmentId(), scope.employeeId()));
    }

    // ── GET /analytics/attendance ──────────────────────────────────────────────

    /**
     * Returns detailed attendance analytics with daily trend.
     *
     * @param from         start of the analysis period
     * @param to           end of the analysis period
     * @param departmentId optional department filter (privileged roles only)
     * @param employeeId   optional employee filter
     * @return attendance breakdown DTO
     */
    @GetMapping(value = "/attendance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get attendance analytics",
            description = "Returns attendance breakdown by status and daily trend. "
                        + "EMPLOYEE role is scoped to their own records only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance analytics returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticsAttendanceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AnalyticsAttendanceResponse> getAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID employeeId) {

        final LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        final LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_DAYS_BACK);

        final ScopeParams scope = resolveScope(departmentId, employeeId);

        return ResponseEntity.ok(analyticsService.getAttendance(
                effectiveFrom, effectiveTo, scope.departmentId(), scope.employeeId()));
    }

    // ── GET /analytics/leaves ──────────────────────────────────────────────────

    /**
     * Returns leave request analytics with type breakdown and monthly trend.
     *
     * @param from         start of the analysis period (request creation date)
     * @param to           end of the analysis period
     * @param departmentId optional department filter
     * @param employeeId   optional employee filter
     * @return leaves breakdown DTO
     */
    @GetMapping(value = "/leaves", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get leave analytics",
            description = "Returns leave breakdown by status and type, plus monthly trend. "
                        + "EMPLOYEE role is scoped to their own leave requests only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave analytics returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticsLeavesResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AnalyticsLeavesResponse> getLeaves(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID employeeId) {

        final LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        final LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_DAYS_BACK);

        final ScopeParams scope = resolveScope(departmentId, employeeId);

        return ResponseEntity.ok(analyticsService.getLeaves(
                effectiveFrom, effectiveTo, scope.departmentId(), scope.employeeId()));
    }

    // ── GET /analytics/tasks ───────────────────────────────────────────────────

    /**
     * Returns task analytics with status breakdown and completion rate.
     *
     * @param departmentId optional department filter
     * @param employeeId   optional employee filter
     * @return task analytics DTO
     */
    @GetMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary     = "Get task analytics",
            description = "Returns task status breakdown and completion rate. "
                        + "EMPLOYEE role is scoped to their own assigned tasks only."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task analytics returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticsTasksResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied")
    })
    public ResponseEntity<AnalyticsTasksResponse> getTasks(
            @RequestParam(required = false) UUID departmentId,
            @RequestParam(required = false) UUID employeeId) {

        final ScopeParams scope = resolveScope(departmentId, employeeId);

        return ResponseEntity.ok(analyticsService.getTasks(
                scope.departmentId(), scope.employeeId()));
    }

    // ── GET /analytics/performance ─────────────────────────────────────────────

    /**
     * Returns AI evaluation performance analytics.
     *
     * <p>Restricted to ADMIN, HR, and MANAGER roles — EMPLOYEE users must not
     * access org-wide AI score data.
     *
     * @param from start of the analysis period
     * @param to   end of the analysis period
     * @return AI performance analytics DTO
     */
    @GetMapping(value = "/performance", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary     = "Get AI performance analytics",
            description = "Returns aggregated AI evaluation scores and trend. "
                        + "Restricted to ADMIN, HR, and MANAGER roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Performance analytics returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticsPerformanceResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role prohibited")
    })
    public ResponseEntity<AnalyticsPerformanceResponse> getPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        final LocalDate effectiveTo   = to   != null ? to   : LocalDate.now();
        final LocalDate effectiveFrom = from != null ? from : effectiveTo.minusDays(DEFAULT_DAYS_BACK);

        return ResponseEntity.ok(analyticsService.getPerformance(effectiveFrom, effectiveTo));
    }

    // ── GET /analytics/departments ─────────────────────────────────────────────

    /**
     * Returns per-department headcount analytics.
     *
     * <p>Restricted to ADMIN, HR, and MANAGER roles.
     *
     * @return department analytics DTO
     */
    @GetMapping(value = "/departments", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(
            summary     = "Get department analytics",
            description = "Returns per-department headcount with active/leave breakdowns. "
                        + "Restricted to ADMIN, HR, and MANAGER roles."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department analytics returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AnalyticsDepartmentsResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Access denied — EMPLOYEE role prohibited")
    })
    public ResponseEntity<AnalyticsDepartmentsResponse> getDepartments() {
        return ResponseEntity.ok(analyticsService.getDepartments());
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Resolves the effective scope (departmentId, employeeId) based on the
     * authenticated user's role.
     *
     * <p>Rules:
     * <ul>
     *   <li>EMPLOYEE: always forced to their own employee UUID; departmentId ignored.</li>
     *   <li>ADMIN / HR / MANAGER: may pass any departmentId or employeeId as filters;
     *       if none supplied, null is used (no filter = org-wide).</li>
     * </ul>
     *
     * @param requestedDepartmentId the department filter from the request
     * @param requestedEmployeeId   the employee filter from the request
     * @return resolved scope parameters
     * @throws AccessDeniedException if an EMPLOYEE tries to access another employee's data
     */
    private ScopeParams resolveScope(
            final UUID requestedDepartmentId,
            final UUID requestedEmployeeId) {

        if (securityUtils.hasRole("ROLE_EMPLOYEE")) {
            // EMPLOYEE: must be scoped to their own record
            final Optional<Employee> currentEmployee = securityUtils.getCurrentEmployee();
            if (currentEmployee.isEmpty()) {
                throw new AccessDeniedException(
                        "No employee record linked to the current user.");
            }
            final UUID myEmployeeId = currentEmployee.get().getId();

            // If the caller explicitly passed a different employeeId, reject
            if (requestedEmployeeId != null && !requestedEmployeeId.equals(myEmployeeId)) {
                throw new AccessDeniedException(
                        "EMPLOYEE role may only access their own analytics.");
            }

            return new ScopeParams(null, myEmployeeId);
        }

        // ADMIN / HR / MANAGER: pass through the requested filters as-is
        return new ScopeParams(requestedDepartmentId, requestedEmployeeId);
    }

    /**
     * Immutable scope parameter record.
     *
     * @param departmentId effective department filter (null = all)
     * @param employeeId   effective employee filter (null = all)
     */
    private record ScopeParams(UUID departmentId, UUID employeeId) {}
}
