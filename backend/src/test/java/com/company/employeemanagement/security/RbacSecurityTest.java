package com.company.employeemanagement.security;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.controller.DepartmentController;
import com.company.employeemanagement.controller.EmployeeController;
import com.company.employeemanagement.controller.LeaveController;
import com.company.employeemanagement.dto.request.CreateLeaveRequest;
import com.company.employeemanagement.dto.response.DepartmentResponse;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.service.DepartmentService;
import com.company.employeemanagement.service.EmployeeService;
import com.company.employeemanagement.service.LeaveRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Role-Based Access Control (RBAC) security tests for all REST endpoints.
 *
 * <p>Uses {@link WebMvcTest} with {@link WithMockUser} to verify:
 * <ul>
 *   <li>Unauthenticated requests receive 401.</li>
 *   <li>Authenticated requests with insufficient roles receive 403.</li>
 *   <li>Authenticated requests with correct roles are permitted (2xx).</li>
 *   <li>EMPLOYEE ownership checks are enforced at the service layer (403 from AccessDeniedException).</li>
 * </ul>
 *
 * <p>Service/repository dependencies are mocked. The JWT filter is replaced
 * by a no-op so that {@code @WithMockUser} authentication flows through cleanly.
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(controllers = {
        EmployeeController.class,
        DepartmentController.class,
        LeaveController.class
})
@Import({SecurityConfig.class, GlobalExceptionHandler.class, RbacSecurityTest.TestSecurityBeans.class})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000"
})
@DisplayName("RBAC Security Tests")
class RbacSecurityTest {

    // ── Test infrastructure beans ──────────────────────────────────────────────

    /**
     * Provides infrastructure beans needed by {@link SecurityConfig} in
     * {@link WebMvcTest} slice context.
     */
    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityBeans {

        /**
         * No-op JWT filter — bypassed in tests so that {@code @WithMockUser}
         * authentication is not overwritten by JWT parsing logic.
         */
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

    @Autowired
    private MockMvc mockMvc;

    @MockBean private EmployeeService       employeeService;
    @MockBean private DepartmentService     departmentService;
    @MockBean private LeaveRequestService   leaveRequestService;
    @MockBean private JwtService            jwtService;
    @MockBean private UserDetailsService    userDetailsService;
    @MockBean private SecurityUtils         securityUtils;

    private ObjectMapper objectMapper;

    private static final UUID EMPLOYEE_ID = UUID.randomUUID();
    private static final UUID DEPT_ID     = UUID.randomUUID();
    private static final UUID LEAVE_ID    = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private EmployeeResponse stubEmployeeResponse() {
        return new EmployeeResponse(
                EMPLOYEE_ID, "EMP-001", DEPT_ID, "Engineering",
                null, "John", "Doe", "john@example.com",
                "Software Engineer", null, null,
                LocalDate.of(2024, 1, 15),
                new BigDecimal("75000.00"), EmployeeStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(),
                null, null
        );
    }

    private LeaveRequestResponse stubLeaveResponse() {
        return new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "EMP-001", "John Doe", "Engineering",
                LeaveType.ANNUAL,
                LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), 5L,
                "vacation", LeaveStatus.PENDING, null, null,
                LocalDateTime.now(), LocalDateTime.now(),
                null, null
        );
    }

    private PageResponse<EmployeeResponse> stubEmployeePage() {
        return new PageResponse<>(
                List.of(stubEmployeeResponse()), 0, 20, 1L, 1, true, LocalDateTime.now());
    }

    private PageResponse<LeaveRequestResponse> stubLeavePage() {
        return new PageResponse<>(
                List.of(stubLeaveResponse()), 0, 20, 1L, 1, true, LocalDateTime.now());
    }

    private PageResponse<DepartmentResponse> stubDepartmentPage() {
        return new PageResponse<>(List.of(), 0, 20, 0L, 0, true, LocalDateTime.now());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Unauthenticated requests — must return 401
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unauthenticated — 401")
    class Unauthenticated {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /employees returns 401 when no token present")
        void getEmployees_noToken_returns401() throws Exception {
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /departments returns 401 when no token present")
        void getDepartments_noToken_returns401() throws Exception {
            mockMvc.perform(get("/departments"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /leaves returns 401 when no token present")
        void getLeaves_noToken_returns401() throws Exception {
            mockMvc.perform(get("/leaves"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /employees returns 401 when no token present")
        void postEmployee_noToken_returns401() throws Exception {
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("DELETE /departments/{id} returns 401 when no token present")
        void deleteDepartment_noToken_returns401() throws Exception {
            mockMvc.perform(delete("/departments/{id}", DEPT_ID))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN — full access
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("ADMIN — full access")
    class AdminAccess {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /employees returns 200")
        void admin_getEmployees_returns200() throws Exception {
            when(employeeService.findAll(any(), any())).thenReturn(stubEmployeePage());
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /employees/{id} returns 204")
        void admin_deleteEmployee_returns204() throws Exception {
            mockMvc.perform(delete("/employees/{id}", EMPLOYEE_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /departments/{id} returns 204")
        void admin_deleteDepartment_returns204() throws Exception {
            mockMvc.perform(delete("/departments/{id}", DEPT_ID))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /leaves/{id}/approve returns 200")
        void admin_approveLeave_returns200() throws Exception {
            when(leaveRequestService.approve(eq(LEAVE_ID), any())).thenReturn(stubLeaveResponse());
            mockMvc.perform(post("/leaves/{id}/approve", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /leaves/{id}/reject returns 200")
        void admin_rejectLeave_returns200() throws Exception {
            when(leaveRequestService.reject(eq(LEAVE_ID), any())).thenReturn(stubLeaveResponse());
            mockMvc.perform(post("/leaves/{id}/reject", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // HR — can create/update employees and departments, approve/reject leave
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("HR — permitted operations")
    class HrAccess {

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("GET /employees returns 200")
        void hr_getEmployees_returns200() throws Exception {
            when(employeeService.findAll(any(), any())).thenReturn(stubEmployeePage());
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("POST /leaves/{id}/approve returns 200")
        void hr_approveLeave_returns200() throws Exception {
            when(leaveRequestService.approve(eq(LEAVE_ID), any())).thenReturn(stubLeaveResponse());
            mockMvc.perform(post("/leaves/{id}/approve", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("DELETE /employees/{id} returns 403 — HR cannot delete employees")
        void hr_deleteEmployee_returns403() throws Exception {
            mockMvc.perform(delete("/employees/{id}", EMPLOYEE_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("DELETE /departments/{id} returns 403 — HR cannot delete departments")
        void hr_deleteDepartment_returns403() throws Exception {
            mockMvc.perform(delete("/departments/{id}", DEPT_ID))
                    .andExpect(status().isForbidden());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MANAGER — read-only employee/department, approve/reject leave
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("MANAGER — permitted operations")
    class ManagerAccess {

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("GET /employees returns 200")
        void manager_getEmployees_returns200() throws Exception {
            when(employeeService.findAll(any(), any())).thenReturn(stubEmployeePage());
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("GET /departments returns 200")
        void manager_getDepartments_returns200() throws Exception {
            when(departmentService.findAllPaged(any(), any())).thenReturn(stubDepartmentPage());
            mockMvc.perform(get("/departments"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("POST /leaves/{id}/approve returns 200")
        void manager_approveLeave_returns200() throws Exception {
            when(leaveRequestService.approve(eq(LEAVE_ID), any())).thenReturn(stubLeaveResponse());
            mockMvc.perform(post("/leaves/{id}/approve", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("POST /leaves/{id}/reject returns 200")
        void manager_rejectLeave_returns200() throws Exception {
            when(leaveRequestService.reject(eq(LEAVE_ID), any())).thenReturn(stubLeaveResponse());
            mockMvc.perform(post("/leaves/{id}/reject", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("POST /employees returns 403 — MANAGER cannot create employees")
        void manager_createEmployee_returns403() throws Exception {
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("DELETE /employees/{id} returns 403 — MANAGER cannot delete employees")
        void manager_deleteEmployee_returns403() throws Exception {
            mockMvc.perform(delete("/employees/{id}", EMPLOYEE_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("POST /departments returns 403 — MANAGER cannot create departments")
        void manager_createDepartment_returns403() throws Exception {
            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("DELETE /departments/{id} returns 403 — MANAGER cannot delete departments")
        void manager_deleteDepartment_returns403() throws Exception {
            mockMvc.perform(delete("/departments/{id}", DEPT_ID))
                    .andExpect(status().isForbidden());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // EMPLOYEE — own resources only
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("EMPLOYEE — own resources")
    class EmployeeAccess {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /employees returns 200 (service may filter by ownership)")
        void employee_getEmployees_returns200() throws Exception {
            when(employeeService.findAll(any(), any())).thenReturn(stubEmployeePage());
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /employees/{id} — own record returns 200")
        void employee_getOwnEmployee_returns200() throws Exception {
            when(employeeService.findById(EMPLOYEE_ID)).thenReturn(stubEmployeeResponse());
            mockMvc.perform(get("/employees/{id}", EMPLOYEE_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /employees/{id} — another employee's record returns 403")
        void employee_getOtherEmployee_returns403() throws Exception {
            UUID otherId = UUID.randomUUID();
            when(employeeService.findById(otherId))
                    .thenThrow(new AccessDeniedException(
                            "You may only access your own employee record."));
            mockMvc.perform(get("/employees/{id}", otherId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /employees returns 403 — EMPLOYEE cannot create employees")
        void employee_createEmployee_returns403() throws Exception {
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /employees/{id} returns 403 — EMPLOYEE cannot delete employees")
        void employee_deleteEmployee_returns403() throws Exception {
            mockMvc.perform(delete("/employees/{id}", EMPLOYEE_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /leaves returns 200 (service scopes to own leaves)")
        void employee_getLeaves_returns200() throws Exception {
            when(leaveRequestService.findAll(any(), any())).thenReturn(stubLeavePage());
            mockMvc.perform(get("/leaves"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /leaves/{id} — own leave returns 200")
        void employee_getOwnLeave_returns200() throws Exception {
            when(leaveRequestService.findById(LEAVE_ID)).thenReturn(stubLeaveResponse());
            mockMvc.perform(get("/leaves/{id}", LEAVE_ID))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /leaves/{id} — another user's leave returns 403")
        void employee_getOtherLeave_returns403() throws Exception {
            UUID otherId = UUID.randomUUID();
            when(leaveRequestService.findById(otherId))
                    .thenThrow(new AccessDeniedException(
                            "You may only access your own leave requests."));
            mockMvc.perform(get("/leaves/{id}", otherId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /leaves/{id}/approve returns 403 — EMPLOYEE cannot approve leave")
        void employee_approveLeave_returns403() throws Exception {
            mockMvc.perform(post("/leaves/{id}/approve", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /leaves/{id}/reject returns 403 — EMPLOYEE cannot reject leave")
        void employee_rejectLeave_returns403() throws Exception {
            mockMvc.perform(post("/leaves/{id}/reject", LEAVE_ID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /leaves — own employee ID returns 201; service enforces ownership")
        void employee_createOwnLeave_returns201() throws Exception {
            CreateLeaveRequest req = new CreateLeaveRequest(
                    EMPLOYEE_ID, LeaveType.ANNUAL,
                    LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), "vacation");
            when(leaveRequestService.create(any())).thenReturn(stubLeaveResponse());
            mockMvc.perform(post("/leaves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /leaves — for another employee returns 403 (service throws AccessDeniedException)")
        void employee_createLeaveForOther_returns403() throws Exception {
            UUID otherId = UUID.randomUUID();
            CreateLeaveRequest req = new CreateLeaveRequest(
                    otherId, LeaveType.ANNUAL,
                    LocalDate.of(2025, 8, 1), LocalDate.of(2025, 8, 5), "vacation");
            when(leaveRequestService.create(any()))
                    .thenThrow(new AccessDeniedException(
                            "You may only submit leave requests for your own employee record."));
            mockMvc.perform(post("/leaves")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(req)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /leaves/{id} — another user's leave returns 403 (service throws)")
        void employee_cancelOtherLeave_returns403() throws Exception {
            UUID otherId = UUID.randomUUID();
            doThrow(new AccessDeniedException(
                    "You may only access your own leave requests."))
                    .when(leaveRequestService).cancel(otherId);
            mockMvc.perform(delete("/leaves/{id}", otherId))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /departments/{id} returns 403 — EMPLOYEE cannot delete departments")
        void employee_deleteDepartment_returns403() throws Exception {
            mockMvc.perform(delete("/departments/{id}", DEPT_ID))
                    .andExpect(status().isForbidden());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Unauthorized / unknown role
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unauthorized role — 403")
    class UnauthorizedRole {

        @Test
        @WithMockUser(roles = "UNKNOWN_ROLE")
        @DisplayName("GET /employees with unknown role returns 403")
        void unknownRole_getEmployees_returns403() throws Exception {
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "UNKNOWN_ROLE")
        @DisplayName("POST /departments with unknown role returns 403")
        void unknownRole_createDepartment_returns403() throws Exception {
            mockMvc.perform(post("/departments")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden());
        }
    }
}
