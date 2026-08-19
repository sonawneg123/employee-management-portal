package com.company.employeemanagement.controller;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.dto.response.AnalyticsAttendanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsDepartmentsResponse;
import com.company.employeemanagement.dto.response.AnalyticsLeavesResponse;
import com.company.employeemanagement.dto.response.AnalyticsPerformanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsSummaryResponse;
import com.company.employeemanagement.dto.response.AnalyticsTasksResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.security.JwtAuthenticationFilter;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.AnalyticsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link AnalyticsController} — Phase 8A.
 *
 * <p>Covers RBAC (ADMIN, HR, MANAGER, EMPLOYEE, unauthenticated), IDOR protection,
 * and response structure for all six endpoints.
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(AnalyticsController.class)
@Import({
        SecurityConfig.class,
        AnalyticsControllerTest.TestSecurityBeans.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000",
        "server.servlet.context-path="
})
@DisplayName("AnalyticsController — Phase 8A web-layer tests")
class AnalyticsControllerTest {

    // ── No-op JWT filter ─────────────────────────────────────────────────────

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityBeans {

        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(
                final JwtService jwtService,
                final UserDetailsService userDetailsService) {

            return new JwtAuthenticationFilter(jwtService, userDetailsService) {
                @Override
                protected void doFilterInternal(
                        final HttpServletRequest request,
                        final HttpServletResponse response,
                        final FilterChain chain) throws ServletException, IOException {
                    chain.doFilter(request, response);
                }
            };
        }
    }

    @Autowired  private MockMvc mockMvc;

    @MockBean private AnalyticsService  analyticsService;
    @MockBean private JwtService        jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private SecurityUtils     securityUtils;

    // ── Fixtures ──────────────────────────────────────────────────────────────

    private static final UUID DEPT_ID = UUID.randomUUID();
    private static final UUID EMP_ID  = UUID.randomUUID();

    private static AnalyticsSummaryResponse summaryFixture() {
        return new AnalyticsSummaryResponse(
                100L, 90L, 10L, 5L, 3L,
                0.87, 2400L, 200L, 50L, 150L,
                80L, 12L, 55L, 13L,
                95L, 60L, 25L, 10L, 0.63,
                78.5, 42L, 3L,
                List.of(new AnalyticsSummaryResponse.TrendPoint("2024-06-01", 0.87)),
                List.of(new AnalyticsSummaryResponse.TrendPoint("2024-06-01", 78.5))
        );
    }

    private static AnalyticsAttendanceResponse attendanceFixture() {
        return new AnalyticsAttendanceResponse(
                2800L, 2400L, 200L, 50L, 100L, 50L, 0.87,
                List.of(new AnalyticsAttendanceResponse.DailyAttendancePoint(
                        "2024-06-01", 95L, 5L, 100L, 0.95))
        );
    }

    private static AnalyticsLeavesResponse leavesFixture() {
        return new AnalyticsLeavesResponse(
                80L, 12L, 55L, 8L, 5L, 0.69,
                List.of(new AnalyticsLeavesResponse.LeaveTypeBreakdown("ANNUAL", 30L)),
                List.of(new AnalyticsLeavesResponse.MonthlyLeaveTrend("2024-06", 15L, 10L))
        );
    }

    private static AnalyticsTasksResponse tasksFixture() {
        return new AnalyticsTasksResponse(
                95L, 60L, 10L, 12L, 3L, 7L, 3L, 0.63,
                List.of(new AnalyticsTasksResponse.TaskStatusBreakdown("COMPLETED", 60L))
        );
    }

    private static AnalyticsPerformanceResponse performanceFixture() {
        return new AnalyticsPerformanceResponse(
                78.5, 74.2, 42L, 3L, 1L,
                List.of(new AnalyticsPerformanceResponse.ScoreTrendPoint("2024-06-01", 82.3, 8L))
        );
    }

    private static AnalyticsDepartmentsResponse deptsFixture() {
        return new AnalyticsDepartmentsResponse(
                8L,
                List.of(new AnalyticsDepartmentsResponse.DepartmentStat(
                        DEPT_ID.toString(), "Engineering", "ENG", 25L, 23L, 2L))
        );
    }

    // ── Unauthenticated access → 401 ─────────────────────────────────────────

    @Nested
    @DisplayName("Unauthenticated access → 401")
    class Unauthenticated {

        @Test @WithAnonymousUser
        @DisplayName("GET /analytics/summary without token → 401")
        void summary_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/analytics/summary"))
                    .andExpect(status().isUnauthorized());
        }

        @Test @WithAnonymousUser
        @DisplayName("GET /analytics/attendance without token → 401")
        void attendance_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/analytics/attendance"))
                    .andExpect(status().isUnauthorized());
        }

        @Test @WithAnonymousUser
        @DisplayName("GET /analytics/performance without token → 401")
        void performance_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/analytics/performance"))
                    .andExpect(status().isUnauthorized());
        }

        @Test @WithAnonymousUser
        @DisplayName("GET /analytics/departments without token → 401")
        void departments_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/analytics/departments"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── EMPLOYEE restricted from performance + departments ───────────────────

    @Nested
    @DisplayName("EMPLOYEE restrictions")
    class EmployeeRestrictions {

        @Test @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE cannot access /analytics/performance → 403")
        void performance_employee_returns403() throws Exception {
            mockMvc.perform(get("/analytics/performance"))
                    .andExpect(status().isForbidden());
        }

        @Test @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE cannot access /analytics/departments → 403")
        void departments_employee_returns403() throws Exception {
            mockMvc.perform(get("/analytics/departments"))
                    .andExpect(status().isForbidden());
        }

        @Test @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE accessing summary receives their own scoped data")
        void summary_employee_receivesOwnData() throws Exception {
            final Employee emp = new Employee();
            emp.setId(EMP_ID);
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(emp));
            when(analyticsService.getSummary(any(), any(), isNull(), eq(EMP_ID)))
                    .thenReturn(summaryFixture());

            mockMvc.perform(get("/analytics/summary"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalEmployees").value(100));
        }

        @Test @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE passing a different employeeId → 403")
        void summary_employee_passOtherEmployeeId_returns403() throws Exception {
            final Employee emp = new Employee();
            emp.setId(EMP_ID);
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(emp));

            final UUID otherId = UUID.randomUUID();
            mockMvc.perform(get("/analytics/summary")
                            .param("employeeId", otherId.toString()))
                    .andExpect(status().isForbidden());
        }

        @Test @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE with no linked employee record → 403")
        void summary_employee_noLinkedRecord_returns403() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.empty());

            mockMvc.perform(get("/analytics/summary"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── ADMIN access ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ADMIN access")
    class AdminAccess {

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /analytics/summary → 200 with correct JSON")
        void summary_admin_returns200() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getSummary(any(), any(), isNull(), isNull()))
                    .thenReturn(summaryFixture());

            mockMvc.perform(get("/analytics/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEmployees").value(100))
                    .andExpect(jsonPath("$.attendanceRate").value(0.87))
                    .andExpect(jsonPath("$.totalTasks").value(95))
                    .andExpect(jsonPath("$.avgAiScore").value(78.5))
                    .andExpect(jsonPath("$.attendanceTrend").isArray())
                    .andExpect(jsonPath("$.aiScoreTrend").isArray());
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /analytics/attendance → 200 with trend array")
        void attendance_admin_returns200() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getAttendance(any(), any(), isNull(), isNull()))
                    .thenReturn(attendanceFixture());

            mockMvc.perform(get("/analytics/attendance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.presentCount").value(2400))
                    .andExpect(jsonPath("$.trend").isArray())
                    .andExpect(jsonPath("$.trend[0].date").value("2024-06-01"));
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /analytics/leaves → 200 with type breakdown")
        void leaves_admin_returns200() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getLeaves(any(), any(), isNull(), isNull()))
                    .thenReturn(leavesFixture());

            mockMvc.perform(get("/analytics/leaves"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalRequests").value(80))
                    .andExpect(jsonPath("$.byType").isArray())
                    .andExpect(jsonPath("$.trend").isArray());
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /analytics/tasks → 200 with status breakdown")
        void tasks_admin_returns200() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getTasks(isNull(), isNull()))
                    .thenReturn(tasksFixture());

            mockMvc.perform(get("/analytics/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalTasks").value(95))
                    .andExpect(jsonPath("$.completionRate").value(0.63))
                    .andExpect(jsonPath("$.statusBreakdown").isArray());
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /analytics/performance → 200 with score data")
        void performance_admin_returns200() throws Exception {
            when(analyticsService.getPerformance(any(), any()))
                    .thenReturn(performanceFixture());

            mockMvc.perform(get("/analytics/performance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.avgCompletionScore").value(78.5))
                    .andExpect(jsonPath("$.completedEvaluations").value(42))
                    .andExpect(jsonPath("$.scoreTrend").isArray());
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /analytics/departments → 200 with department list")
        void departments_admin_returns200() throws Exception {
            when(analyticsService.getDepartments()).thenReturn(deptsFixture());

            mockMvc.perform(get("/analytics/departments"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalDepartments").value(8))
                    .andExpect(jsonPath("$.departments").isArray())
                    .andExpect(jsonPath("$.departments[0].departmentName").value("Engineering"))
                    .andExpect(jsonPath("$.departments[0].headcount").value(25));
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("Department filter is passed through for ADMIN")
        void summary_admin_withDeptFilter_passesThrough() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getSummary(any(), any(), eq(DEPT_ID), isNull()))
                    .thenReturn(summaryFixture());

            mockMvc.perform(get("/analytics/summary")
                            .param("departmentId", DEPT_ID.toString()))
                    .andExpect(status().isOk());
        }
    }

    // ── HR access ─────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("HR access")
    class HrAccess {

        @Test @WithMockUser(roles = "HR")
        @DisplayName("HR can access performance endpoint")
        void performance_hr_returns200() throws Exception {
            when(analyticsService.getPerformance(any(), any()))
                    .thenReturn(performanceFixture());

            mockMvc.perform(get("/analytics/performance"))
                    .andExpect(status().isOk());
        }

        @Test @WithMockUser(roles = "HR")
        @DisplayName("HR can access departments endpoint")
        void departments_hr_returns200() throws Exception {
            when(analyticsService.getDepartments()).thenReturn(deptsFixture());

            mockMvc.perform(get("/analytics/departments"))
                    .andExpect(status().isOk());
        }
    }

    // ── MANAGER access ────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MANAGER access")
    class ManagerAccess {

        @Test @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER can access performance endpoint")
        void performance_manager_returns200() throws Exception {
            when(analyticsService.getPerformance(any(), any()))
                    .thenReturn(performanceFixture());

            mockMvc.perform(get("/analytics/performance"))
                    .andExpect(status().isOk());
        }

        @Test @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER can access summary endpoint")
        void summary_manager_returns200() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getSummary(any(), any(), isNull(), isNull()))
                    .thenReturn(summaryFixture());

            mockMvc.perform(get("/analytics/summary"))
                    .andExpect(status().isOk());
        }
    }

    // ── Date filter handling ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Date filter handling")
    class DateFilters {

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("Custom date range is accepted for attendance")
        void attendance_customDateRange_accepted() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getAttendance(any(), any(), isNull(), isNull()))
                    .thenReturn(attendanceFixture());

            mockMvc.perform(get("/analytics/attendance")
                            .param("from", "2024-01-01")
                            .param("to", "2024-01-31"))
                    .andExpect(status().isOk());
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("Custom date range is accepted for leaves")
        void leaves_customDateRange_accepted() throws Exception {
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getLeaves(any(), any(), isNull(), isNull()))
                    .thenReturn(leavesFixture());

            mockMvc.perform(get("/analytics/leaves")
                            .param("from", "2024-01-01")
                            .param("to", "2024-06-30"))
                    .andExpect(status().isOk());
        }
    }

    // ── Empty dataset handling ────────────────────────────────────────────────

    @Nested
    @DisplayName("Empty dataset responses")
    class EmptyDatasets {

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("Empty summary is returned as valid JSON with zero values")
        void summary_emptyData_returnsZeroValues() throws Exception {
            final AnalyticsSummaryResponse empty = new AnalyticsSummaryResponse(
                    0L, 0L, 0L, 0L, 0L,
                    0.0, 0L, 0L, 0L, 0L,
                    0L, 0L, 0L, 0L,
                    0L, 0L, 0L, 0L, 0.0,
                    -1.0, 0L, 0L,
                    List.of(), List.of()
            );
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getSummary(any(), any(), isNull(), isNull()))
                    .thenReturn(empty);

            mockMvc.perform(get("/analytics/summary"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalEmployees").value(0))
                    .andExpect(jsonPath("$.attendanceTrend").isArray())
                    .andExpect(jsonPath("$.attendanceTrend").isEmpty());
        }

        @Test @WithMockUser(roles = "ADMIN")
        @DisplayName("Empty attendance trend is returned as empty array")
        void attendance_emptyTrend_returnsEmptyArray() throws Exception {
            final AnalyticsAttendanceResponse empty = new AnalyticsAttendanceResponse(
                    0L, 0L, 0L, 0L, 0L, 0L, 0.0, List.of());
            when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
            when(analyticsService.getAttendance(any(), any(), isNull(), isNull()))
                    .thenReturn(empty);

            mockMvc.perform(get("/analytics/attendance"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.trend").isArray())
                    .andExpect(jsonPath("$.trend").isEmpty());
        }
    }
}
