package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.ForgotPasswordRequest;
import com.company.employeemanagement.dto.request.ResetPasswordRequest;
import com.company.employeemanagement.dto.request.VerifyOtpRequest;
import com.company.employeemanagement.dto.response.MessageResponse;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for the password-reset endpoints in {@link AuthController}.
 *
 * <p>Covers the happy path and the key security/error scenarios for all three endpoints:
 * <ul>
 *   <li>{@code POST /auth/forgot-password}</li>
 *   <li>{@code POST /auth/verify-otp}</li>
 *   <li>{@code POST /auth/reset-password}</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthController — password-reset endpoints")
class PasswordResetControllerTest {

    @Mock private AuthService          authService;
    @Mock private PasswordResetService passwordResetService;

    @InjectMocks
    private AuthController authController;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    private static final String OTP_SENT_MSG =
            "An OTP has been sent to your email address.";

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(authController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/forgot-password
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/forgot-password")
    class ForgotPassword {

        @Test
        @DisplayName("returns 200 with OTP sent message for existing email")
        void existingEmailReturns200() throws Exception {
            when(passwordResetService.requestPasswordReset(any(ForgotPasswordRequest.class)))
                    .thenReturn(new MessageResponse(OTP_SENT_MSG));

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ForgotPasswordRequest("alice@example.com"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(OTP_SENT_MSG));
        }

        @Test
        @DisplayName("returns 404 with friendly message for nonexistent email")
        void nonexistentEmailReturns404() throws Exception {
            when(passwordResetService.requestPasswordReset(any(ForgotPasswordRequest.class)))
                    .thenThrow(new com.company.employeemanagement.exception.ResourceNotFoundException(
                            "This email is not registered with us."));

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ForgotPasswordRequest("ghost@example.com"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail").value(containsString("not registered")));
        }

        @Test
        @DisplayName("returns 400 for invalid email format")
        void invalidEmailReturns400() throws Exception {
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"not-an-email\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when email is blank")
        void blankEmailReturns400() throws Exception {
            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"email\":\"\"}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("404 detail message does not expose raw DB/user-not-found language")
        void doesNotExposeRawDbLanguage() throws Exception {
            when(passwordResetService.requestPasswordReset(any()))
                    .thenThrow(new com.company.employeemanagement.exception.ResourceNotFoundException(
                            "This email is not registered with us."));

            mockMvc.perform(post("/auth/forgot-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ForgotPasswordRequest("nobody@example.com"))))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.detail", not(containsString("User not found"))))
                    .andExpect(jsonPath("$.detail", not(containsString("No row"))));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/verify-otp
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/verify-otp")
    class VerifyOtp {

        @Test
        @DisplayName("returns 200 on correct OTP")
        void correctOtpReturns200() throws Exception {
            when(passwordResetService.verifyOtp(any(VerifyOtpRequest.class)))
                    .thenReturn(new MessageResponse("OTP verified successfully. You may now reset your password."));

            mockMvc.perform(post("/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyOtpRequest("alice@example.com", "482713"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("verified")));
        }

        @Test
        @DisplayName("returns 400 on incorrect OTP (service throws IllegalArgumentException)")
        void incorrectOtpReturns400() throws Exception {
            when(passwordResetService.verifyOtp(any()))
                    .thenThrow(new IllegalArgumentException("Incorrect OTP. 4 attempt(s) remaining."));

            mockMvc.perform(post("/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyOtpRequest("alice@example.com", "000000"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("Incorrect OTP")));
        }

        @Test
        @DisplayName("returns 400 on expired OTP")
        void expiredOtpReturns400() throws Exception {
            when(passwordResetService.verifyOtp(any()))
                    .thenThrow(new IllegalArgumentException("OTP is invalid or has expired. Please request a new one."));

            mockMvc.perform(post("/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyOtpRequest("alice@example.com", "123456"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("expired")));
        }

        @Test
        @DisplayName("returns 400 when OTP is not exactly 6 digits")
        void shortOtpReturns400() throws Exception {
            mockMvc.perform(post("/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyOtpRequest("alice@example.com", "12345"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when max attempts exceeded")
        void maxAttemptsExceededReturns400() throws Exception {
            when(passwordResetService.verifyOtp(any()))
                    .thenThrow(new IllegalArgumentException(
                            "Maximum verification attempts exceeded. Please request a new OTP."));

            mockMvc.perform(post("/auth/verify-otp")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new VerifyOtpRequest("alice@example.com", "000000"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("Maximum")));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /auth/reset-password
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /auth/reset-password")
    class ResetPassword {

        @Test
        @DisplayName("returns 200 on successful password reset")
        void successfulResetReturns200() throws Exception {
            when(passwordResetService.resetPassword(any(ResetPasswordRequest.class)))
                    .thenReturn(new MessageResponse(
                            "Password has been reset successfully. Please log in with your new password."));

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ResetPasswordRequest("alice@example.com", "NewPass1!", "NewPass1!"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value(containsString("reset successfully")));
        }

        @Test
        @DisplayName("returns 400 when passwords do not match")
        void passwordMismatchReturns400() throws Exception {
            when(passwordResetService.resetPassword(any()))
                    .thenThrow(new IllegalArgumentException("Passwords do not match."));

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ResetPasswordRequest("alice@example.com", "NewPass1!", "Different1!"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.detail").value(containsString("do not match")));
        }

        @Test
        @DisplayName("returns 400 when no valid verified token exists")
        void noVerifiedTokenReturns400() throws Exception {
            when(passwordResetService.resetPassword(any()))
                    .thenThrow(new IllegalArgumentException("No valid verified OTP found."));

            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ResetPasswordRequest("alice@example.com", "NewPass1!", "NewPass1!"))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when new password is too short")
        void shortPasswordReturns400() throws Exception {
            mockMvc.perform(post("/auth/reset-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new ResetPasswordRequest("alice@example.com", "short", "short"))))
                    .andExpect(status().isBadRequest());
        }
    }
}
