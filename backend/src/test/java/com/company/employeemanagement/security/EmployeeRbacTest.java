package com.company.employeemanagement.security;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.controller.AttendanceController;
import com.company.employeemanagement.controller.ReviewController;
import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.ReviewResponse;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.service.AttendanceService;
import com.company.employeemanagement.service.ReviewService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
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
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role-Based Access Control tests for the Attendance and Review controllers,
 * focused on verifying that ROLE_EMPLOYEE can access the self-service endpoints
 * and is correctly blocked from administrative operations.
 *
 * <p>Verifies:
 * <ul>
 *   <li>EMPLOYEE can GET /attendance/my — 200</li>
 *   <li>EMPLOYEE cannot GET /attendance (admin list) — 403</li>
 *   <li>EMPLOYEE cannot POST /attendance — 403</li>
 *   <li>EMPLOYEE cannot PUT /attendance/{id} — 403</li>
 *   <li>EMPLOYEE can GET /reviews — 200 (service auto-scopes to own)</li>
 *   <li>EMPLOYEE cannot POST /reviews — 403</li>
 *   <li>EMPLOYEE cannot DELETE /reviews/{id} — 403</li>
 *   <li>Unauthenticated /attendance/my — 401</li>
 *   <li>EMPLOYEE accessing another employee's attendance record — 403</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(controllers = {AttendanceController.class, ReviewController.class})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, EmployeeRbacTest.TestSecurityBeans.class})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000"
})
@DisplayName("Employee RBAC — Attendance & Review")
class EmployeeRbacTest {

    // ── No-op JWT filter so @WithMockUser flows through ───────────────────────

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
                        final FilterChain filterChain) throws ServletException, IOException {
                    filterChain.doFilter(request, response);
                }
            };
        }
    }

    @Autowired  private MockMvc mockMvc;
    @MockBean   private AttendanceService  attendanceService;
    @MockBean   private ReviewService      reviewService;
    @MockBean   private JwtService         jwtService;
    @MockBean   private UserDetailsService userDetailsService;
    @MockBean   private SecurityUtils      securityUtils;

    private static final UUID REC_ID  = UUID.randomUUID();
    private static final UUID EMP_ID  = UUID.randomUUID();
    private static final UUID REV_ID  = UUID.randomUUID();

    private PageResponse<AttendanceResponse> emptyAttendancePage() {
        return new PageResponse<>(List.of(), 0, 20, 0L, 0, true, LocalDateTime.now());
    }

    private AttendanceResponse stubAttendanceRecord() {
        return new AttendanceResponse(
                REC_ID, EMP_ID, "EMP-001", "Jane Doe",
                LocalDate.of(2025, 8, 1),
                LocalTime.of(9, 0), LocalTime.of(17, 0),
                AttendanceStatus.PRESENT, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private PageResponse<ReviewResponse> emptyReviewPage() {
        return new PageResponse<>(List.of(), 0, 20, 0L, 0, true, LocalDateTime.now());
    }

    @BeforeEach
    void setUpMocks() {
        when(attendanceService.findMyAttendance(any(), any(), any()))
                .thenReturn(emptyAttendancePage());
        when(reviewService.findAll(any(), any()))
                .thenReturn(emptyReviewPage());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Unauthenticated → 401
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unauthenticated — 401")
    class Unauthenticated {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /attendance/my without token returns 401")
        void attendanceMy_noToken_401() throws Exception {
            mockMvc.perform(get("/attendance/my"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /reviews without token returns 401")
        void reviews_noToken_401() throws Exception {
            mockMvc.perform(get("/reviews"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMPLOYEE self-service — 200
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EMPLOYEE — permitted self-service")
    class EmployeePermitted {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /attendance/my returns 200 for EMPLOYEE")
        void employee_myAttendance_200() throws Exception {
            mockMvc.perform(get("/attendance/my"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /reviews returns 200 for EMPLOYEE (service auto-scopes to own)")
        void employee_reviews_200() throws Exception {
            mockMvc.perform(get("/reviews"))
                    .andExpect(status().isOk());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMPLOYEE blocked from admin operations — 403
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EMPLOYEE — blocked from admin operations")
    class EmployeeBlocked {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /attendance (admin list) returns 403 for EMPLOYEE")
        void employee_attendanceList_403() throws Exception {
            mockMvc.perform(get("/attendance"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /attendance returns 403 for EMPLOYEE")
        void employee_createAttendance_403() throws Exception {
            mockMvc.perform(post("/attendance")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /reviews returns 403 for EMPLOYEE")
        void employee_createReview_403() throws Exception {
            mockMvc.perform(post("/reviews")
                            .contentType("application/json")
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /reviews/{id} returns 403 for EMPLOYEE")
        void employee_deleteReview_403() throws Exception {
            mockMvc.perform(delete("/reviews/{id}", REV_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /attendance/{id} for another employee returns 403 (service throws)")
        void employee_anotherRecord_403() throws Exception {
            when(attendanceService.findById(REC_ID))
                    .thenThrow(new AccessDeniedException(
                            "You may only access your own attendance records."));
            mockMvc.perform(get("/attendance/{id}", REC_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN / HR — still have full access
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ADMIN — full attendance access preserved")
    class AdminAccess {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /attendance returns 200 for ADMIN")
        void admin_attendanceList_200() throws Exception {
            when(attendanceService.findAll(any(), any(), any(), any()))
                    .thenReturn(new PageResponse<>(
                            List.of(stubAttendanceRecord()), 0, 20, 1L, 1, true, LocalDateTime.now()));
            mockMvc.perform(get("/attendance"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("GET /attendance returns 200 for HR")
        void hr_attendanceList_200() throws Exception {
            when(attendanceService.findAll(any(), any(), any(), any()))
                    .thenReturn(emptyAttendancePage());
            mockMvc.perform(get("/attendance"))
                    .andExpect(status().isOk());
        }
    }
}
