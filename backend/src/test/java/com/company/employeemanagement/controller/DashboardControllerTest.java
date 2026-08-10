package com.company.employeemanagement.controller;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.dto.response.ActivityItemResponse;
import com.company.employeemanagement.dto.response.DashboardChartsResponse;
import com.company.employeemanagement.dto.response.DashboardSummaryResponse;
import com.company.employeemanagement.security.JwtAuthenticationFilter;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.DashboardService;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Web-layer tests for {@link DashboardController}.
 *
 * <p>Reproduces the original failure scenario (missing controller → 500) and
 * verifies all three happy-path endpoints plus unauthenticated access control.
 *
 * <p>No database, Flyway migration, or Testcontainers instance is required —
 * the service layer is mocked.
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(DashboardController.class)
@Import({
        SecurityConfig.class,
        DashboardControllerTest.TestSecurityBeans.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000",
        "server.servlet.context-path="
})
@DisplayName("DashboardController — web-layer tests")
class DashboardControllerTest {

    // ── No-op JWT filter (honours @WithMockUser / @WithAnonymousUser) ──────────

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
                        final FilterChain chain)
                        throws ServletException, IOException {
                    chain.doFilter(request, response);
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean private DashboardService dashboardService;
    @MockBean private JwtService       jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private SecurityUtils    securityUtils;

    // ── Fixtures ───────────────────────────────────────────────────────────────

    private static final DashboardSummaryResponse SUMMARY_FIXTURE =
            new DashboardSummaryResponse(
                    42L,   // totalEmployees
                    5L,    // totalDepartments
                    3L,    // pendingLeaves
                    40L,   // activeEmployees
                    38L,   // presentToday
                    2L,    // onLeaveToday
                    4L,    // newThisMonth
                    2L,    // trendEmployees
                    -1L,   // trendLeaves
                    0.05,  // trendAttendance
                    0.90   // attendanceRate
            );

    private static final DashboardChartsResponse CHARTS_FIXTURE =
            new DashboardChartsResponse(
                    List.of(
                            new DashboardChartsResponse.DepartmentDistribution("Engineering", "ENG", 20L),
                            new DashboardChartsResponse.DepartmentDistribution("HR",          "HR",  5L)
                    ),
                    List.of(
                            new DashboardChartsResponse.EmployeeStatusCount("ACTIVE",   40L),
                            new DashboardChartsResponse.EmployeeStatusCount("INACTIVE",  2L)
                    ),
                    List.of(
                            new DashboardChartsResponse.AttendanceTrendPoint("2024-01-15", 38L, 4L)
                    )
            );

    private static final List<ActivityItemResponse> ACTIVITY_FIXTURE = List.of(
            new ActivityItemResponse(
                    "act-1",
                    "LEAVE_REQUESTED",
                    "Alice Smith requested annual leave",
                    "2024-01-15T10:00:00",
                    "Alice Smith",
                    null
            )
    );

    // ── Unauthenticated access ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Unauthenticated access → 401")
    class Unauthenticated {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /dashboard/summary without token returns 401")
        void summary_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /dashboard/charts without token returns 401")
        void charts_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/dashboard/charts"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /dashboard/activity without token returns 401")
        void activity_unauthenticated_returns401() throws Exception {
            mockMvc.perform(get("/dashboard/activity"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ── GET /dashboard/summary ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /dashboard/summary")
    class Summary {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 200 with correct JSON structure")
        void summary_admin_returns200WithJson() throws Exception {
            when(dashboardService.getSummary()).thenReturn(SUMMARY_FIXTURE);

            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalEmployees").value(42))
                    .andExpect(jsonPath("$.totalDepartments").value(5))
                    .andExpect(jsonPath("$.pendingLeaves").value(3))
                    .andExpect(jsonPath("$.activeEmployees").value(40))
                    .andExpect(jsonPath("$.presentToday").value(38))
                    .andExpect(jsonPath("$.onLeaveToday").value(2))
                    .andExpect(jsonPath("$.newThisMonth").value(4))
                    .andExpect(jsonPath("$.trendEmployees").value(2))
                    .andExpect(jsonPath("$.trendLeaves").value(-1))
                    .andExpect(jsonPath("$.attendanceRate").value(0.90));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("is accessible by EMPLOYEE role")
        void summary_employee_returns200() throws Exception {
            when(dashboardService.getSummary()).thenReturn(SUMMARY_FIXTURE);
            mockMvc.perform(get("/dashboard/summary"))
                    .andExpect(status().isOk());
        }
    }

    // ── GET /dashboard/charts ──────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /dashboard/charts")
    class Charts {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 200 with department distribution and status breakdown")
        void charts_admin_returns200WithJson() throws Exception {
            when(dashboardService.getCharts()).thenReturn(CHARTS_FIXTURE);

            mockMvc.perform(get("/dashboard/charts"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.departmentDistribution").isArray())
                    .andExpect(jsonPath("$.departmentDistribution[0].name").value("Engineering"))
                    .andExpect(jsonPath("$.departmentDistribution[0].code").value("ENG"))
                    .andExpect(jsonPath("$.departmentDistribution[0].count").value(20))
                    .andExpect(jsonPath("$.employeeStatusBreakdown").isArray())
                    .andExpect(jsonPath("$.employeeStatusBreakdown[0].status").value("ACTIVE"))
                    .andExpect(jsonPath("$.employeeStatusBreakdown[0].count").value(40))
                    .andExpect(jsonPath("$.attendanceTrend").isArray())
                    .andExpect(jsonPath("$.attendanceTrend[0].date").value("2024-01-15"))
                    .andExpect(jsonPath("$.attendanceTrend[0].present").value(38))
                    .andExpect(jsonPath("$.attendanceTrend[0].absent").value(4));
        }
    }

    // ── GET /dashboard/activity ────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /dashboard/activity")
    class Activity {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("returns 200 with activity item list")
        void activity_admin_returns200WithList() throws Exception {
            when(dashboardService.getActivity(10)).thenReturn(ACTIVITY_FIXTURE);

            mockMvc.perform(get("/dashboard/activity"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$").isArray())
                    .andExpect(jsonPath("$[0].id").value("act-1"))
                    .andExpect(jsonPath("$[0].type").value("LEAVE_REQUESTED"))
                    .andExpect(jsonPath("$[0].actorName").value("Alice Smith"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("accepts a custom limit parameter")
        void activity_customLimit_passedToService() throws Exception {
            when(dashboardService.getActivity(5)).thenReturn(List.of());

            mockMvc.perform(get("/dashboard/activity").param("limit", "5"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());
        }
    }
}
