package com.company.employeemanagement.controller;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.ReviewResponse;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.security.JwtAuthenticationFilter;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.security.SecurityUtils;
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
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
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
 * WebMvc tests for {@link ReviewController}.
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(controllers = ReviewController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, ReviewControllerTest.TestSecurityBeans.class})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000"
})
@DisplayName("ReviewController")
class ReviewControllerTest {

    @TestConfiguration
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestSecurityBeans {
        @Bean
        public JwtAuthenticationFilter jwtAuthenticationFilter(
                final JwtService jwtService, final UserDetailsService uds) {
            return new JwtAuthenticationFilter(jwtService, uds) {
                @Override
                protected void doFilterInternal(
                        final HttpServletRequest req, final HttpServletResponse res,
                        final FilterChain chain) throws ServletException, IOException {
                    chain.doFilter(req, res);
                }
            };
        }
    }

    @Autowired private MockMvc mockMvc;
    @MockBean  private ReviewService reviewService;
    @MockBean  private JwtService jwtService;
    @MockBean  private UserDetailsService userDetailsService;
    @MockBean  private SecurityUtils securityUtils;

    private static final UUID REVIEW_ID   = UUID.randomUUID();
    private static final UUID EMPLOYEE_ID = UUID.randomUUID();

    private ReviewResponse stubResponse() {
        return new ReviewResponse(
                REVIEW_ID, EMPLOYEE_ID, "EMP-001", "Jane Smith", "Engineering",
                UUID.randomUUID(), "John Manager",
                "Q1 2025", 4, "Good",
                LocalDate.of(2025, 3, 31), "Great work", "Improve coverage",
                LocalDateTime.now(), LocalDateTime.now(),
                "manager@example.com", "manager@example.com");
    }

    private PageResponse<ReviewResponse> stubPage() {
        return new PageResponse<>(List.of(stubResponse()), 0, 20, 1L, 1, true, LocalDateTime.now());
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 401 — unauthenticated
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("401 — unauthenticated")
    class Unauthenticated {

        @Test
        @WithAnonymousUser
        @DisplayName("GET /reviews returns 401 without token")
        void getReturns401() throws Exception {
            mockMvc.perform(get("/reviews")).andExpect(status().isUnauthorized());
        }

        @Test
        @WithAnonymousUser
        @DisplayName("POST /reviews returns 401 without token")
        void postReturns401() throws Exception {
            mockMvc.perform(post("/reviews")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 403 — insufficient role
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("403 — insufficient role")
    class Forbidden {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE cannot create a review — 403")
        void employeeCannotCreate() throws Exception {
            mockMvc.perform(post("/reviews")
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE cannot update a review — 403")
        void employeeCannotUpdate() throws Exception {
            mockMvc.perform(put("/reviews/{id}", REVIEW_ID)
                    .contentType(MediaType.APPLICATION_JSON).content("{}"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE cannot delete a review — 403")
        void employeeCannotDelete() throws Exception {
            mockMvc.perform(delete("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "HR")
        @DisplayName("HR cannot delete a review — 403")
        void hrCannotDelete() throws Exception {
            mockMvc.perform(delete("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "MANAGER")
        @DisplayName("MANAGER cannot delete a review — 403")
        void managerCannotDelete() throws Exception {
            mockMvc.perform(delete("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("EMPLOYEE accessing another's review returns 403 (service throws)")
        void employeeAccessOtherReview403() throws Exception {
            when(reviewService.findById(eq(REVIEW_ID)))
                    .thenThrow(new AccessDeniedException("You may only access your own performance reviews."));

            mockMvc.perform(get("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.title").value("Access Denied"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 200 / 201 — success
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("success responses")
    class SuccessResponses {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /reviews returns 200 with page")
        void adminGetReviews() throws Exception {
            when(reviewService.findAll(any(), any())).thenReturn(stubPage());
            mockMvc.perform(get("/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].reviewPeriod").value("Q1 2025"));
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("GET /reviews returns 200 for EMPLOYEE (service scopes to own)")
        void employeeGetOwnReviews() throws Exception {
            when(reviewService.findAll(any(), any())).thenReturn(stubPage());
            mockMvc.perform(get("/reviews"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /reviews/{id} returns 200")
        void getById() throws Exception {
            when(reviewService.findById(any(UUID.class))).thenReturn(stubResponse());
            mockMvc.perform(get("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.rating").value(4))
                    .andExpect(jsonPath("$.ratingLabel").value("Good"));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /reviews returns 201 with new review")
        void create201() throws Exception {
            when(reviewService.create(any())).thenReturn(stubResponse());
            String body = """
                    {
                      "employeeId":"%s",
                      "reviewPeriod":"Q1 2025",
                      "rating":4,
                      "reviewDate":"2025-03-31",
                      "comments":"Great work",
                      "goals":"Improve coverage"
                    }
                    """.formatted(EMPLOYEE_ID);
            mockMvc.perform(post("/reviews")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.rating").value(4));
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("DELETE /reviews/{id} returns 204")
        void delete204() throws Exception {
            mockMvc.perform(delete("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isNoContent());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 400 — validation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("400 — validation failures")
    class ValidationErrors {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /reviews with missing employeeId returns 400")
        void missingEmployeeId() throws Exception {
            String body = """
                    {"reviewPeriod":"Q1 2025","rating":4,"reviewDate":"2025-03-31"}
                    """;
            mockMvc.perform(post("/reviews")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.employeeId").exists());
        }

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("POST /reviews with rating=6 returns 400")
        void ratingOutOfRange() throws Exception {
            String body = """
                    {"employeeId":"%s","reviewPeriod":"Q1 2025","rating":6,"reviewDate":"2025-03-31"}
                    """.formatted(EMPLOYEE_ID);
            mockMvc.perform(post("/reviews")
                    .contentType(MediaType.APPLICATION_JSON).content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.rating").exists());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // 404 — not found
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("404 — not found")
    class NotFound {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("GET /reviews/{id} returns 404 for unknown ID")
        void unknownId404() throws Exception {
            when(reviewService.findById(REVIEW_ID))
                    .thenThrow(new ResourceNotFoundException("PerformanceReview", REVIEW_ID));
            mockMvc.perform(get("/reviews/{id}", REVIEW_ID))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }
}
