package com.company.employeemanagement.controller;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.config.SecurityConfig;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.security.JwtAuthenticationFilter;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.SettingsService;
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
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * WebMvc tests for {@link SettingsController}.
 *
 * @author Employee Management Portal Team
 */
@WebMvcTest(controllers = SettingsController.class)
@Import({SecurityConfig.class, GlobalExceptionHandler.class, SettingsControllerTest.TestSecurityBeans.class})
@TestPropertySource(properties = {
        "app.jwt.secret=ThisIsAVeryLongSecretKeyForJWTSigningThatIsAtLeast256BitsLong!!",
        "app.jwt.expiration-ms=86400000",
        "app.jwt.refresh-expiration-ms=604800000"
})
@DisplayName("SettingsController")
class SettingsControllerTest {

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

    @MockBean private SettingsService settingsService;
    @MockBean private JwtService jwtService;
    @MockBean private UserDetailsService userDetailsService;
    @MockBean private SecurityUtils securityUtils;

    private static final String CHANGE_PW_URL = "/settings/change-password";

    private String validBody() {
        return """
                {"currentPassword":"OldP@ss1","newPassword":"NewP@ss1!","confirmPassword":"NewP@ss1!"}
                """;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Unauthenticated
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("401 — unauthenticated")
    class Unauthenticated {

        @Test
        @WithAnonymousUser
        @DisplayName("POST /settings/change-password returns 401 without token")
        void returns401() throws Exception {
            mockMvc.perform(post(CHANGE_PW_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Success
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("204 — success")
    class Success {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 204 when password changed successfully")
        void returns204() throws Exception {
            doNothing().when(settingsService).changePassword(any(), any(), any());

            mockMvc.perform(post(CHANGE_PW_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isNoContent());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Wrong current password
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("401 — wrong current password")
    class WrongPassword {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 401 when BadCredentialsException thrown")
        void returns401OnBadCredentials() throws Exception {
            doThrow(new BadCredentialsException("Current password is incorrect."))
                    .when(settingsService).changePassword(any(), any(), any());

            mockMvc.perform(post(CHANGE_PW_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content()
                            .contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Validation errors
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("400 — validation failures")
    class ValidationErrors {

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 400 when currentPassword is blank")
        void blankCurrentPassword() throws Exception {
            String body = """
                    {"currentPassword":"","newPassword":"NewP@ss1!","confirmPassword":"NewP@ss1!"}
                    """;
            mockMvc.perform(post(CHANGE_PW_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.currentPassword").exists());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 400 when newPassword is too short")
        void shortNewPassword() throws Exception {
            String body = """
                    {"currentPassword":"OldP@ss1","newPassword":"short","confirmPassword":"short"}
                    """;
            mockMvc.perform(post(CHANGE_PW_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.violations.newPassword").exists());
        }

        @Test
        @WithMockUser(roles = "EMPLOYEE")
        @DisplayName("returns 400 when service throws IllegalArgumentException (passwords don't match)")
        void passwordMismatch() throws Exception {
            doThrow(new IllegalArgumentException("New password and confirmation do not match."))
                    .when(settingsService).changePassword(any(), any(), any());

            mockMvc.perform(post(CHANGE_PW_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(validBody()))
                    .andExpect(status().isBadRequest());
        }
    }
}
