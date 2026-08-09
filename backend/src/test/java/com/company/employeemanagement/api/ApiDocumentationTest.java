package com.company.employeemanagement.api;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.OpenApiConfig;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.controller.AuthController;
import com.company.employeemanagement.controller.DepartmentController;
import com.company.employeemanagement.controller.EmployeeController;
import com.company.employeemanagement.controller.LeaveController;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.security.JwtAuthenticationFilter;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.AuthService;
import com.company.employeemanagement.service.DepartmentService;
import com.company.employeemanagement.service.EmployeeService;
import com.company.employeemanagement.service.LeaveRequestService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for API documentation availability and structured error-response consistency.
 *
 * <p>Uses {@link WebMvcTest} to boot the Spring MVC slice with the real
 * {@link SecurityConfig} and {@link GlobalExceptionHandler}, verifying that:
 * <ul>
 *   <li>The OpenAPI JSON descriptor ({@code /v3/api-docs}) is publicly reachable
 *       (SpringDoc registers its own MVC controller, auto-configured in the slice).</li>
 *   <li>Unauthenticated requests to protected endpoints return a structured JSON 401.</li>
 *   <li>Insufficient-role requests return a structured JSON 403.</li>
 *   <li>Not-found resources return a structured JSON 404.</li>
 *   <li>Bean-Validation failures return a structured JSON 400 with a violations map.</li>
 * </ul>
 *
 * <p>The JWT filter is replaced by a no-op so that {@code @WithMockUser}
 * authentication flows through without token parsing.
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(controllers = {
        AuthController.class,
        EmployeeController.class,
        DepartmentController.class,
        LeaveController.class
})
@Import({
        SecurityConfig.class,
        OpenApiConfig.class,
        GlobalExceptionHandler.class,
        ApiDocumentationTest.TestSecurityBeans.class
})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000",
        "springdoc.api-docs.enabled=true",
        "springdoc.swagger-ui.enabled=true"
})
@DisplayName("API Documentation & Error Response Consistency")
class ApiDocumentationTest {

    // ── No-op JWT filter so @WithMockUser is honoured ───────────────────────

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

    @Autowired private MockMvc mockMvc;

    @MockBean private AuthService         authService;
    @MockBean private EmployeeService     employeeService;
    @MockBean private DepartmentService   departmentService;
    @MockBean private LeaveRequestService leaveRequestService;
    @MockBean private JwtService          jwtService;
    @MockBean private UserDetailsService  userDetailsService;
    @MockBean private SecurityUtils       securityUtils;

    private static final UUID ANY_UUID = UUID.randomUUID();

    // ══════════════════════════════════════════════════════════════════════════
    // OpenAPI / Swagger UI endpoint availability
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("OpenAPI descriptor availability")
    class OpenApiAvailability {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /v3/api-docs returns 200 with JSON content")
        void apiDocs_publiclyReachable_returns200() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /v3/api-docs contains API title")
        void apiDocs_containsTitle() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.title")
                            .value("Employee Management Portal API"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /v3/api-docs contains API version")
        void apiDocs_containsVersion() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.info.version").value("1.0.0"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /v3/api-docs defines BearerAuth security scheme")
        void apiDocs_definesBearerAuthScheme() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth").exists())
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.type")
                            .value("http"))
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.scheme")
                            .value("bearer"))
                    .andExpect(jsonPath("$.components.securitySchemes.BearerAuth.bearerFormat")
                            .value("JWT"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /v3/api-docs exposes Authentication tag")
        void apiDocs_exposesAuthenticationTag() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags[?(@.name == 'Authentication')]").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /v3/api-docs exposes Employees tag")
        void apiDocs_exposesEmployeesTag() throws Exception {
            mockMvc.perform(get("/v3/api-docs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.tags[?(@.name == 'Employees')]").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /swagger-ui.html is publicly accessible (redirect or 200)")
        void swaggerUi_publiclyAccessible() throws Exception {
            mockMvc.perform(get("/swagger-ui.html"))
                    .andExpect(status().is(anyOf(equalTo(200), equalTo(302))));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Structured 401 — unauthenticated requests
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Structured 401 — unauthenticated requests")
    class Structured401 {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /employees without token returns JSON 401 with status field")
        void getEmployees_unauthenticated_returns401Json() throws Exception {
            mockMvc.perform(get("/employees"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(401));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /departments without token returns JSON 401 with title field")
        void getDepartments_unauthenticated_returns401JsonWithTitle() throws Exception {
            mockMvc.perform(get("/departments"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value("Unauthorized"));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("GET /leaves without token returns JSON 401 with detail field")
        void getLeaves_unauthenticated_returns401JsonWithDetail() throws Exception {
            mockMvc.perform(get("/leaves"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.detail").value(not(blankOrNullString())));
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /employees without token returns JSON 401, not HTML")
        void postEmployee_unauthenticated_returnsJsonNotHtml() throws Exception {
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Structured 403 — insufficient role
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Structured 403 — insufficient role")
    class Structured403 {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /employees/{id} by EMPLOYEE returns JSON 403 with status field")
        void deleteEmployee_byEmployee_returns403Json() throws Exception {
            mockMvc.perform(delete("/employees/{id}", ANY_UUID))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("DELETE /departments/{id} by EMPLOYEE returns JSON 403 with title field")
        void deleteDepartment_byEmployee_returns403WithTitle() throws Exception {
            mockMvc.perform(delete("/departments/{id}", ANY_UUID))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("POST /employees by MANAGER returns JSON 403")
        void createEmployee_byManager_returns403Json() throws Exception {
            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("POST /leaves/{id}/approve by EMPLOYEE returns JSON 403")
        void approveLeave_byEmployee_returns403Json() throws Exception {
            mockMvc.perform(post("/leaves/{id}/approve", ANY_UUID)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isForbidden())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(403));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Structured 400 — Bean Validation failures
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Structured 400 — validation failures")
    class Structured400 {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /auth/register with invalid email returns JSON 400 with violations map")
        void register_invalidEmail_returns400WithViolations() throws Exception {
            String body = """
                    {"email":"not-an-email","password":"SecureP@ss1","firstName":"John","lastName":"Doe"}
                    """;
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.title").value("Validation Failed"))
                    .andExpect(jsonPath("$.properties.violations.email").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /auth/register with short password returns JSON 400 with violations map")
        void register_shortPassword_returns400WithViolations() throws Exception {
            String body = """
                    {"email":"valid@example.com","password":"short","firstName":"John","lastName":"Doe"}
                    """;
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.properties.violations.password").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /auth/login with blank email returns JSON 400 with violations map")
        void login_blankEmail_returns400WithViolations() throws Exception {
            String body = """
                    {"email":"","password":"somepass"}
                    """;
            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.properties.violations.email").exists());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /auth/register with missing body fields returns JSON 400 with violations")
        void register_missingFields_returns400() throws Exception {
            String body = "{}";
            mockMvc.perform(post("/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(400))
                    .andExpect(jsonPath("$.properties.violations").isMap());
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // Structured 404 — resource not found via GlobalExceptionHandler
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Structured 404 — resource not found")
    class Structured404 {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /employees/{unknownId} returns JSON 404 with title field")
        void getEmployee_unknownId_returns404Json() throws Exception {
            org.mockito.Mockito.when(employeeService.findById(ANY_UUID))
                    .thenThrow(new com.company.employeemanagement.exception.ResourceNotFoundException(
                            "Employee", ANY_UUID));

            mockMvc.perform(get("/employees/{id}", ANY_UUID))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Resource Not Found"))
                    .andExpect(jsonPath("$.detail").value(not(blankOrNullString())));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /departments/{unknownId} returns JSON 404 with status and title")
        void getDepartment_unknownId_returns404Json() throws Exception {
            org.mockito.Mockito.when(departmentService.findById(ANY_UUID))
                    .thenThrow(new com.company.employeemanagement.exception.ResourceNotFoundException(
                            "Department", ANY_UUID));

            mockMvc.perform(get("/departments/{id}", ANY_UUID))
                    .andExpect(status().isNotFound())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }
}
