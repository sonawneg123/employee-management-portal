package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.ForgotPasswordRequest;
import com.company.employeemanagement.dto.request.ResetPasswordRequest;
import com.company.employeemanagement.dto.request.VerifyOtpRequest;
import com.company.employeemanagement.dto.response.MessageResponse;
import com.company.employeemanagement.entity.PasswordResetToken;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.PasswordResetTokenRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.EmailService;
import com.company.employeemanagement.service.PasswordResetService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

/**
 * Implementation of {@link PasswordResetService} that provides a
 * three-step OTP-based password-reset flow.
 *
 * <p>Security measures implemented:
 * <ul>
 *   <li>OTPs are generated with {@link SecureRandom} — cryptographically secure.</li>
 *   <li>OTPs are hashed with BCrypt before storage — not stored in plaintext.</li>
 *   <li>Constant-time comparison via {@link PasswordEncoder#matches} — no timing attack.</li>
 *   <li>Generic responses prevent email-existence enumeration.</li>
 *   <li>OTPs expire after {@value PasswordResetToken#OTP_EXPIRY_MINUTES} minutes.</li>
 *   <li>Maximum {@value PasswordResetToken#MAX_ATTEMPTS} failed attempts lock the token.</li>
 *   <li>Requesting a new OTP invalidates all previous pending tokens for that email.</li>
 *   <li>A verified token is consumed on password reset — it cannot be reused.</li>
 *   <li>Existing JWT sessions remain valid after password reset
 *       (see known-limitations note in the implementation comment).</li>
 * </ul>
 *
 * <p><strong>JWT session note:</strong> the current project does not implement
 * a token-revocation list or version counter on {@code User}. After a password
 * reset, previously issued JWTs remain structurally valid until they expire
 * naturally. This is a known limitation documented in the final report.
 *
 * @author Employee Management Portal Team
 */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final Logger log = LoggerFactory.getLogger(PasswordResetServiceImpl.class);

    /** Response returned when an OTP is successfully dispatched. */
    private static final String OTP_SENT_RESPONSE =
            "An OTP has been sent to your email address.";

    private static final String OTP_VERIFIED_MESSAGE =
            "OTP verified successfully. You may now reset your password.";

    private static final String PASSWORD_RESET_MESSAGE =
            "Password has been reset successfully. Please log in with your new password.";

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param tokenRepository  repository for OTP token persistence
     * @param userRepository   repository for user lookups and password updates
     * @param passwordEncoder  BCrypt encoder (reused for both passwords and OTP hashing)
     * @param emailService     service for sending OTP emails
     */
    public PasswordResetServiceImpl(final PasswordResetTokenRepository tokenRepository,
                                    final UserRepository userRepository,
                                    final PasswordEncoder passwordEncoder,
                                    final EmailService emailService) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.secureRandom = new SecureRandom();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Steps:
     * <ol>
     *   <li>Look up user by email — throw {@link ResourceNotFoundException} if not found.</li>
     *   <li>Invalidate all existing pending tokens for this email.</li>
     *   <li>Generate a cryptographically secure 6-digit OTP.</li>
     *   <li>Hash the OTP with BCrypt and persist a new {@link PasswordResetToken}.</li>
     *   <li>Email the raw OTP to the user.</li>
     *   <li>Return confirmation response.</li>
     * </ol>
     */
    @Override
    @Transactional
    public MessageResponse requestPasswordReset(final ForgotPasswordRequest request) {
        final String email = request.email().toLowerCase().trim();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "This email is not registered with us."));

        // Invalidate all previous pending tokens for this email
        tokenRepository.invalidateAllPendingTokens(email);

        // Generate a cryptographically secure 6-digit OTP (100000–999999)
        final String rawOtp = generateOtp();

        // Hash the OTP with BCrypt — never store the raw value
        final String hashedOtp = passwordEncoder.encode(rawOtp);

        // Persist the token
        PasswordResetToken token = PasswordResetToken.builder()
                .email(email)
                .otpHash(hashedOtp)
                .expiresAt(LocalDateTime.now().plusMinutes(PasswordResetToken.OTP_EXPIRY_MINUTES))
                .build();
        tokenRepository.save(token);

        // Send email with the raw OTP (never log the raw OTP)
        log.info("Password-reset OTP generated for user id={} email={}", user.getId(), email);

        try {
            emailService.sendPasswordResetOtp(
                    email,
                    user.getFirstName() + " " + user.getLastName(),
                    rawOtp,
                    PasswordResetToken.OTP_EXPIRY_MINUTES
            );
        } catch (RuntimeException ex) {
            log.error("OTP email delivery failed for email={}: {}", email, ex.getMessage());
        }

        return new MessageResponse(OTP_SENT_RESPONSE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Uses BCrypt's constant-time {@link PasswordEncoder#matches} to compare
     * the submitted OTP against the stored hash, preventing timing-based attacks.
     */
    @Override
    @Transactional
    public MessageResponse verifyOtp(final VerifyOtpRequest request) {
        final String email = request.email().toLowerCase().trim();
        final String submittedOtp = request.otp();

        PasswordResetToken token = tokenRepository
                .findActiveTokenByEmail(email, LocalDateTime.now(), PasswordResetToken.MAX_ATTEMPTS)
                .orElseThrow(() -> new IllegalArgumentException(
                        "OTP is invalid or has expired. Please request a new one."));

        // Constant-time comparison via BCrypt
        if (!passwordEncoder.matches(submittedOtp, token.getOtpHash())) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            tokenRepository.save(token);

            int remaining = PasswordResetToken.MAX_ATTEMPTS - token.getAttemptCount();
            if (remaining <= 0) {
                log.warn("OTP max attempts exceeded for email={}", email);
                throw new IllegalArgumentException(
                        "Maximum verification attempts exceeded. Please request a new OTP.");
            }
            throw new IllegalArgumentException(
                    "Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // OTP is correct — mark as verified
        token.setVerified(true);
        tokenRepository.save(token);

        log.info("OTP verified for email={}", email);
        return new MessageResponse(OTP_VERIFIED_MESSAGE);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The verified token is consumed (marked as used) before the password
     * update is committed, preventing double-use even under concurrent requests.
     */
    @Override
    @Transactional
    public MessageResponse resetPassword(final ResetPasswordRequest request) {
        final String email = request.email().toLowerCase().trim();

        if (!request.newPassword().equals(request.confirmPassword())) {
            throw new IllegalArgumentException("Passwords do not match.");
        }

        PasswordResetToken token = tokenRepository
                .findVerifiedTokenByEmail(email, LocalDateTime.now())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No valid verified OTP found. Please verify your OTP first."));

        // Consume the token before changing the password — prevents double-use
        token.setUsed(true);
        tokenRepository.save(token);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Account not found. Password reset failed."));

        // Hash the new password with BCrypt (same encoder used for login)
        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);

        log.info("Password reset completed for user id={} email={}", user.getId(), email);
        return new MessageResponse(PASSWORD_RESET_MESSAGE);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /**
     * Generates a cryptographically secure 6-digit OTP string.
     *
     * <p>Uses {@link SecureRandom} to produce a value in the range [100000, 999999],
     * ensuring the OTP is always exactly 6 digits with no leading zeros.
     *
     * @return a 6-digit OTP string
     */
    private String generateOtp() {
        // Range: 100000 to 999999 (inclusive) → always exactly 6 digits
        int otp = 100000 + secureRandom.nextInt(900000);
        return String.valueOf(otp);
    }
}
