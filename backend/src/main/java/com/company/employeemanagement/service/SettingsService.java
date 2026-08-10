package com.company.employeemanagement.service;

/**
 * Service contract for user settings operations.
 *
 * <p>All methods operate on the <em>currently authenticated</em> principal —
 * no ID parameter is needed or accepted.
 *
 * @author Employee Management Portal Team
 */
public interface SettingsService {

    /**
     * Changes the password of the currently authenticated user.
     *
     * <p>Steps:
     * <ol>
     *   <li>Verify the {@code currentPassword} against the stored hash.</li>
     *   <li>Validate that {@code newPassword} and {@code confirmPassword} match.</li>
     *   <li>Encode the new password with BCrypt and persist it.</li>
     * </ol>
     *
     * @param currentPassword the caller's current password (plaintext, for verification)
     * @param newPassword     the desired new password (plaintext, will be hashed)
     * @param confirmPassword must equal {@code newPassword}
     * @throws org.springframework.security.authentication.BadCredentialsException
     *         if {@code currentPassword} does not match the stored hash
     * @throws IllegalArgumentException if {@code newPassword} and {@code confirmPassword}
     *         do not match, or if the new password equals the current password
     */
    void changePassword(String currentPassword, String newPassword, String confirmPassword);
}
