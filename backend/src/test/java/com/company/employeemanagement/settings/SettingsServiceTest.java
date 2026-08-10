package com.company.employeemanagement.settings;

import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.impl.SettingsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SettingsServiceImpl}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SettingsServiceImpl")
class SettingsServiceTest {

    @Mock private SecurityUtils securityUtils;
    @Mock private UserRepository userRepository;

    private PasswordEncoder passwordEncoder;
    private SettingsServiceImpl settingsService;

    @BeforeEach
    void setUp() {
        passwordEncoder = new BCryptPasswordEncoder(4); // low cost for tests
        settingsService = new SettingsServiceImpl(securityUtils, userRepository, passwordEncoder);
    }

    private User buildUser(final String email, final String plaintextPassword) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(plaintextPassword));
        return user;
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Successful password change
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("successful password change")
    class Success {

        @Test
        @DisplayName("encodes and saves new password when all validations pass")
        void savesEncodedNewPassword() {
            User user = buildUser("user@example.com", "OldP@ss1");
            when(securityUtils.getCurrentUsername()).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            settingsService.changePassword("OldP@ss1", "NewP@ss1!", "NewP@ss1!");

            ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(captor.capture());
            User saved = captor.getValue();
            assertThat(passwordEncoder.matches("NewP@ss1!", saved.getPasswordHash())).isTrue();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Wrong current password
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("wrong current password")
    class WrongCurrent {

        @Test
        @DisplayName("throws BadCredentialsException when current password is wrong")
        void throwsBadCredentials() {
            User user = buildUser("user@example.com", "RealPassword1");
            when(securityUtils.getCurrentUsername()).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    settingsService.changePassword("WrongPassword!", "NewP@ss1!", "NewP@ss1!"))
                    .isInstanceOf(BadCredentialsException.class)
                    .hasMessageContaining("Current password is incorrect");

            verify(userRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Passwords don't match
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("new password confirmation mismatch")
    class ConfirmMismatch {

        @Test
        @DisplayName("throws IllegalArgumentException when newPassword != confirmPassword")
        void throwsIllegalArgument() {
            User user = buildUser("user@example.com", "OldP@ss1");
            when(securityUtils.getCurrentUsername()).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    settingsService.changePassword("OldP@ss1", "NewP@ss1!", "DifferentPass!"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("do not match");

            verify(userRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // New password same as current
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("new password same as current")
    class SameAsCurrentPassword {

        @Test
        @DisplayName("throws IllegalArgumentException when new == current")
        void throwsIllegalArgument() {
            User user = buildUser("user@example.com", "OldP@ss1");
            when(securityUtils.getCurrentUsername()).thenReturn("user@example.com");
            when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() ->
                    settingsService.changePassword("OldP@ss1", "OldP@ss1", "OldP@ss1"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("different from the current password");

            verify(userRepository, never()).save(any());
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Unauthenticated request
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("unauthenticated")
    class Unauthenticated {

        @Test
        @DisplayName("throws AccessDeniedException when no authentication context")
        void throwsAccessDenied() {
            when(securityUtils.getCurrentUsername()).thenReturn(null);

            assertThatThrownBy(() ->
                    settingsService.changePassword("any", "NewP@ss1!", "NewP@ss1!"))
                    .isInstanceOf(AccessDeniedException.class);

            verify(userRepository, never()).findByEmail(any());
            verify(userRepository, never()).save(any());
        }
    }
}
