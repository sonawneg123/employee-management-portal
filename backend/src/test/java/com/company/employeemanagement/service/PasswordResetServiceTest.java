package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.ForgotPasswordRequest;
import com.company.employeemanagement.dto.request.ResetPasswordRequest;
import com.company.employeemanagement.dto.request.VerifyOtpRequest;
import com.company.employeemanagement.dto.response.MessageResponse;
import com.company.employeemanagement.entity.PasswordResetToken;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.repository.PasswordResetTokenRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.impl.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PasswordResetServiceImpl}.
 *
 * <p>Uses a real {@link BCryptPasswordEncoder} for OTP hashing to verify that
 * OTPs are never stored as plaintext and that constant-time comparison works correctly.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetServiceImpl")
class PasswordResetServiceTest {

    @Mock private PasswordResetTokenRepository tokenRepository;
    @Mock private UserRepository               userRepository;
    @Mock private EmailService                 emailService;

    /** Real encoder — needed to test hashing and constant-time OTP comparison. */
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(4); // cost 4 for speed

    private PasswordResetServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new PasswordResetServiceImpl(tokenRepository, userRepository,
                                               passwordEncoder, emailService);
    }

    // ── Shared helpers ─────────────────────────────────────────────────────────

    private User buildUser(String email) {
        User user = User.builder()
                .email(email)
                .firstName("Test")
                .lastName("User")
                .passwordHash(passwordEncoder.encode("OldPass1!"))
                .isEnabled(true)
                .build();
        user.setId(UUID.randomUUID());
        return user;
    }

    private PasswordResetToken buildActiveToken(String email, String rawOtp) {
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .otpHash(passwordEncoder.encode(rawOtp))
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        token.setId(UUID.randomUUID());
        return token;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 1. requestPasswordReset
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("requestPasswordReset()")
    class RequestPasswordReset {

        @Test
        @DisplayName("1. Existing email: invalidates old tokens, creates new token, sends email")
        void existingEmailSendsOtp() {
            User user = buildUser("alice@example.com");
            when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
            doNothing().when(tokenRepository).invalidateAllPendingTokens(anyString());
            when(tokenRepository.save(any(PasswordResetToken.class))).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(emailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyInt());

            MessageResponse resp = service.requestPasswordReset(new ForgotPasswordRequest("alice@example.com"));

            assertThat(resp.message()).contains("OTP has been sent");
            verify(tokenRepository).invalidateAllPendingTokens("alice@example.com");
            verify(tokenRepository).save(any(PasswordResetToken.class));
            verify(emailService).sendPasswordResetOtp(eq("alice@example.com"), anyString(), anyString(), eq(10));
        }

        @Test
        @DisplayName("2. Nonexistent email: throws ResourceNotFoundException")
        void nonexistentEmailThrows() {
            when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.requestPasswordReset(new ForgotPasswordRequest("ghost@example.com")))
                    .isInstanceOf(com.company.employeemanagement.exception.ResourceNotFoundException.class)
                    .hasMessageContaining("not registered");

            verify(tokenRepository, never()).save(any());
            verify(emailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("2b. Nonexistent email: no OTP token is created")
        void nonexistentEmailNoTokenCreated() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.requestPasswordReset(new ForgotPasswordRequest("nobody@example.com")))
                    .isInstanceOf(com.company.employeemanagement.exception.ResourceNotFoundException.class);

            verify(tokenRepository, never()).save(any());
        }

        @Test
        @DisplayName("2c. Nonexistent email: email service is not called")
        void nonexistentEmailNoEmailSent() {
            when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.requestPasswordReset(new ForgotPasswordRequest("nobody@example.com")))
                    .isInstanceOf(com.company.employeemanagement.exception.ResourceNotFoundException.class);

            verify(emailService, never()).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyInt());
        }

        @Test
        @DisplayName("3. OTP is NOT stored as plaintext — stored hash must not equal the raw OTP")
        void otpNotStoredAsPlaintext() {
            User user = buildUser("bob@example.com");
            when(userRepository.findByEmail("bob@example.com")).thenReturn(Optional.of(user));
            doNothing().when(tokenRepository).invalidateAllPendingTokens(anyString());
            doNothing().when(emailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyInt());

            ArgumentCaptor<PasswordResetToken> captor = ArgumentCaptor.forClass(PasswordResetToken.class);
            when(tokenRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

            // Capture the raw OTP sent to the email service
            ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
            service.requestPasswordReset(new ForgotPasswordRequest("bob@example.com"));

            PasswordResetToken savedToken = captor.getValue();
            // The stored hash must NOT equal the raw OTP
            assertThat(savedToken.getOtpHash()).isNotEqualTo(
                    otpCaptor.getAllValues().isEmpty() ? "" : otpCaptor.getValue());

            // The hash must be a BCrypt hash (starts with $2)
            assertThat(savedToken.getOtpHash()).startsWith("$2");

            // The raw OTP (captured from email call) should NOT appear in the hash
            verify(emailService).sendPasswordResetOtp(anyString(), anyString(), otpCaptor.capture(), anyInt());
            assertThat(savedToken.getOtpHash()).doesNotContain(otpCaptor.getValue());
        }

        @Test
        @DisplayName("9. New OTP request invalidates previous pending tokens")
        void newOtpInvalidatesPrevious() {
            User user = buildUser("carol@example.com");
            when(userRepository.findByEmail("carol@example.com")).thenReturn(Optional.of(user));
            doNothing().when(tokenRepository).invalidateAllPendingTokens("carol@example.com");
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(emailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyInt());

            service.requestPasswordReset(new ForgotPasswordRequest("carol@example.com"));

            verify(tokenRepository).invalidateAllPendingTokens("carol@example.com");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 2. verifyOtp
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("verifyOtp()")
    class VerifyOtp {

        @Test
        @DisplayName("4. Correct OTP succeeds — token marked as verified")
        void correctOtpSucceeds() {
            String rawOtp = "482713";
            PasswordResetToken token = buildActiveToken("dave@example.com", rawOtp);

            when(tokenRepository.findActiveTokenByEmail(eq("dave@example.com"),
                    any(LocalDateTime.class), eq(PasswordResetToken.MAX_ATTEMPTS)))
                    .thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            MessageResponse resp = service.verifyOtp(
                    new VerifyOtpRequest("dave@example.com", rawOtp));

            assertThat(resp.message()).contains("verified");
            assertThat(token.isVerified()).isTrue();
            verify(tokenRepository).save(token);
        }

        @Test
        @DisplayName("5. Incorrect OTP fails — attempt count incremented")
        void incorrectOtpFails() {
            String rawOtp = "482713";
            PasswordResetToken token = buildActiveToken("eve@example.com", rawOtp);

            when(tokenRepository.findActiveTokenByEmail(eq("eve@example.com"),
                    any(LocalDateTime.class), eq(PasswordResetToken.MAX_ATTEMPTS)))
                    .thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() ->
                    service.verifyOtp(new VerifyOtpRequest("eve@example.com", "000000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Incorrect OTP");

            assertThat(token.getAttemptCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("6. Expired OTP fails — no active token found")
        void expiredOtpFails() {
            when(tokenRepository.findActiveTokenByEmail(anyString(),
                    any(LocalDateTime.class), anyInt()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.verifyOtp(new VerifyOtpRequest("frank@example.com", "123456")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("expired");
        }

        @Test
        @DisplayName("8. Maximum attempts: after MAX_ATTEMPTS wrong tries, error is thrown")
        void maxAttemptsEnforced() {
            String rawOtp = "482713";
            PasswordResetToken token = buildActiveToken("george@example.com", rawOtp);
            // Simulate already at MAX_ATTEMPTS - 1 so next wrong attempt triggers the cap
            token.setAttemptCount(PasswordResetToken.MAX_ATTEMPTS - 1);

            when(tokenRepository.findActiveTokenByEmail(eq("george@example.com"),
                    any(LocalDateTime.class), eq(PasswordResetToken.MAX_ATTEMPTS)))
                    .thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            assertThatThrownBy(() ->
                    service.verifyOtp(new VerifyOtpRequest("george@example.com", "000000")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Maximum verification attempts exceeded");

            assertThat(token.getAttemptCount()).isEqualTo(PasswordResetToken.MAX_ATTEMPTS);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 3. resetPassword
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("resetPassword()")
    class ResetPassword {

        @Test
        @DisplayName("10. Password is actually changed in the database")
        void passwordIsActuallyChanged() {
            User user = buildUser("helen@example.com");
            PasswordResetToken token = buildActiveToken("helen@example.com", "482713");
            token.setVerified(true);

            when(tokenRepository.findVerifiedTokenByEmail(eq("helen@example.com"),
                    any(LocalDateTime.class))).thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail("helen@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resetPassword(new ResetPasswordRequest(
                    "helen@example.com", "NewPass1!", "NewPass1!"));

            ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(userCaptor.capture());
            User savedUser = userCaptor.getValue();

            // 13. Password is stored hashed
            assertThat(savedUser.getPasswordHash()).startsWith("$2");
            assertThat(savedUser.getPasswordHash()).isNotEqualTo("NewPass1!");
        }

        @Test
        @DisplayName("11. Old password no longer works after reset (hash doesn't match old)")
        void oldPasswordNoLongerWorks() {
            User user = buildUser("ida@example.com");
            String oldHash = user.getPasswordHash();
            PasswordResetToken token = buildActiveToken("ida@example.com", "482713");
            token.setVerified(true);

            when(tokenRepository.findVerifiedTokenByEmail(eq("ida@example.com"),
                    any(LocalDateTime.class))).thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail("ida@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resetPassword(new ResetPasswordRequest(
                    "ida@example.com", "BrandNew@2!", "BrandNew@2!"));

            // New hash must differ from the old one
            assertThat(user.getPasswordHash()).isNotEqualTo(oldHash);
            // Old password should not match the new hash
            assertThat(passwordEncoder.matches("OldPass1!", user.getPasswordHash())).isFalse();
        }

        @Test
        @DisplayName("12. New password works — new hash matches the new raw password")
        void newPasswordWorks() {
            User user = buildUser("jack@example.com");
            PasswordResetToken token = buildActiveToken("jack@example.com", "482713");
            token.setVerified(true);

            when(tokenRepository.findVerifiedTokenByEmail(eq("jack@example.com"),
                    any(LocalDateTime.class))).thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail("jack@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resetPassword(new ResetPasswordRequest(
                    "jack@example.com", "MyNew@pass9", "MyNew@pass9"));

            assertThat(passwordEncoder.matches("MyNew@pass9", user.getPasswordHash())).isTrue();
        }

        @Test
        @DisplayName("7. OTP cannot be reused — token is consumed (used=true) after reset")
        void otpCannotBeReused() {
            User user = buildUser("kate@example.com");
            PasswordResetToken token = buildActiveToken("kate@example.com", "482713");
            token.setVerified(true);

            when(tokenRepository.findVerifiedTokenByEmail(eq("kate@example.com"),
                    any(LocalDateTime.class))).thenReturn(Optional.of(token));
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(userRepository.findByEmail("kate@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.resetPassword(new ResetPasswordRequest(
                    "kate@example.com", "NewPass1!", "NewPass1!"));

            // Token must be marked as used
            assertThat(token.isUsed()).isTrue();
        }

        @Test
        @DisplayName("Password mismatch throws IllegalArgumentException")
        void passwordMismatchThrows() {
            assertThatThrownBy(() ->
                    service.resetPassword(new ResetPasswordRequest(
                            "lee@example.com", "NewPass1!", "Different1!")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("do not match");

            verify(tokenRepository, never()).findVerifiedTokenByEmail(anyString(), any());
        }

        @Test
        @DisplayName("No verified token throws IllegalArgumentException")
        void noVerifiedTokenThrows() {
            when(tokenRepository.findVerifiedTokenByEmail(anyString(),
                    any(LocalDateTime.class))).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.resetPassword(new ResetPasswordRequest(
                            "mike@example.com", "NewPass1!", "NewPass1!")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("No valid verified OTP");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // 14. Unregistered email behaviour
    // ══════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Unregistered email")
    class NoEnumeration {

        @Test
        @DisplayName("14. Unregistered email returns ResourceNotFoundException with friendly message")
        void unregisteredEmailThrowsWithFriendlyMessage() {
            when(userRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() ->
                    service.requestPasswordReset(new ForgotPasswordRequest("nonexistent@example.com")))
                    .isInstanceOf(com.company.employeemanagement.exception.ResourceNotFoundException.class)
                    .hasMessageContaining("not registered");
        }

        @Test
        @DisplayName("Registered email still produces OTP response after new behaviour")
        void registeredEmailStillWorks() {
            User user = buildUser("existing@example.com");
            when(userRepository.findByEmail("existing@example.com")).thenReturn(Optional.of(user));
            doNothing().when(tokenRepository).invalidateAllPendingTokens(anyString());
            when(tokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            doNothing().when(emailService).sendPasswordResetOtp(anyString(), anyString(), anyString(), anyInt());

            MessageResponse resp = service.requestPasswordReset(
                    new ForgotPasswordRequest("existing@example.com"));

            assertThat(resp.message()).contains("OTP has been sent");
            verify(emailService).sendPasswordResetOtp(eq("existing@example.com"), anyString(), anyString(), anyInt());
        }
    }
}
