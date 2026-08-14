package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.service.AuthService;
import com.company.employeemanagement.service.PasswordResetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AuthController} covering the wrong-credentials login flow.
 *
 * <p>Verifies that every combination of bad credentials (wrong password for existing
 * user, nonexistent email, admin/HR/employee wrong password) produces:
 * <ul>
 *   <li>HTTP 401 Unauthorized</li>
 *   <li>RFC-7807 ProblemDetail body with {@code "Invalid email or password."}</li>
 *   <li>No internal exception details leaked to the response</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — wrong credentials")
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private MockMvc     mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private String jsonBody(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new LoginRequest(email, password));
    }

    private AuthResponse dummyAuthResponse(String email, String role) {
        return new AuthResponse(
                "jwt.token.here", "Bearer", 86400L,
                UUID.randomUUID(), email,
                "Test", "User", List.of(role)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Wrong credentials → 401
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Wrong password for existing user")
    class WrongPassword {

        @Test
        @DisplayName("existing admin + wrong password returns 401")
        void adminWrongPasswordReturns401() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("admin@company.com", "wrongpassword")))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        @DisplayName("existing admin + wrong password returns 'Invalid email or password.' detail")
        void adminWrongPasswordReturnsGenericDetail() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("admin@company.com", "wrongpassword")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password."))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"));
        }

        @Test
        @DisplayName("existing HR user + wrong password returns 401")
        void hrWrongPasswordReturns401() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("hr@company.com", "wrongpassword")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password."));
        }

        @Test
        @DisplayName("existing employee + wrong password returns 401")
        void employeeWrongPasswordReturns401() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("employee@company.com", "wrongpassword")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Nonexistent email
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Nonexistent email")
    class NonexistentEmail {

        @Test
        @DisplayName("nonexistent email + any password returns 401 (same message)")
        void nonexistentEmailReturns401() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("doesnotexist@example.com", "anything")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password."))
                    .andExpect(jsonPath("$.title").value("Authentication Failed"));
        }

        @Test
        @DisplayName("response for nonexistent email is identical to wrong-password message (no user enumeration)")
        void noUserEnumerationLeak() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("nobody@nowhere.com", "pass")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail").value("Invalid email or password."));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // No internal exception details leaked
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Security: no internal details leaked")
    class NoInternalDetailsLeaked {

        @Test
        @DisplayName("response body does not contain BadCredentialsException class name")
        void doesNotLeakExceptionClassName() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("test@example.com", "wrong")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail", not(containsString("BadCredentialsException"))))
                    .andExpect(jsonPath("$.detail", not(containsString("Bad credentials"))));
        }

        @Test
        @DisplayName("response body does not expose 'User not found' or email existence")
        void doesNotLeakEmailExistence() throws Exception {
            when(authService.login(any())).thenThrow(new BadCredentialsException("Bad credentials"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("ghost@example.com", "pass")))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.detail", not(containsString("not found"))))
                    .andExpect(jsonPath("$.detail", not(containsString("User"))));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Successful login (valid credentials still work)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful login")
    class SuccessfulLogin {

        @Test
        @DisplayName("returns 200 with JWT for valid admin credentials")
        void validAdminLoginReturns200() throws Exception {
            when(authService.login(any())).thenReturn(dummyAuthResponse("admin@company.com", "ROLE_ADMIN"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("admin@company.com", "Admin@1234!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists())
                    .andExpect(jsonPath("$.tokenType").value("Bearer"));
        }

        @Test
        @DisplayName("returns 200 with JWT for valid HR credentials")
        void validHrLoginReturns200() throws Exception {
            when(authService.login(any())).thenReturn(dummyAuthResponse("hr@company.com", "ROLE_HR"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("hr@company.com", "HR@1234!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        @DisplayName("returns 200 with JWT for valid employee credentials")
        void validEmployeeLoginReturns200() throws Exception {
            when(authService.login(any())).thenReturn(dummyAuthResponse("emp@company.com", "ROLE_EMPLOYEE"));

            mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(jsonBody("emp@company.com", "Emp@1234!")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.accessToken").exists());
        }
    }
}
