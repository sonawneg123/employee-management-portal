package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.SettingsService;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link SettingsService}.
 *
 * <p>All operations are scoped to the currently authenticated user —
 * no user ID is accepted or exposed. The password is never returned.
 *
 * @author Employee Management Portal Team
 */
@Service
public class SettingsServiceImpl implements SettingsService {

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Constructs the service with required dependencies.
     *
     * @param securityUtils  helper for current-principal inspection
     * @param userRepository repository for user persistence
     * @param passwordEncoder BCrypt encoder for password hashing and verification
     */
    public SettingsServiceImpl(final SecurityUtils securityUtils,
                                final UserRepository userRepository,
                                final PasswordEncoder passwordEncoder) {
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Steps:
     * <ol>
     *   <li>Resolve the currently authenticated user from the security context.</li>
     *   <li>Verify {@code currentPassword} against the stored BCrypt hash.</li>
     *   <li>Validate that {@code newPassword} equals {@code confirmPassword}.</li>
     *   <li>Reject if the new password is identical to the current password.</li>
     *   <li>Encode the new password and save.</li>
     * </ol>
     */
    @Override
    @Transactional
    public void changePassword(final String currentPassword,
                                final String newPassword,
                                final String confirmPassword) {
        String email = securityUtils.getCurrentUsername();
        if (email == null) {
            throw new AccessDeniedException("Not authenticated.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        // Verify current password
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new BadCredentialsException("Current password is incorrect.");
        }

        // Confirm new passwords match
        if (!newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("New password and confirmation do not match.");
        }

        // Reject if new == current
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException(
                    "New password must be different from the current password.");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
