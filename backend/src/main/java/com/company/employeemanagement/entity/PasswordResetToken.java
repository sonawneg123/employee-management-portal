package com.company.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Stores a hashed OTP (One-Time Password) for the password-reset flow.
 *
 * <p>Security properties:
 * <ul>
 *   <li>The raw OTP is <strong>never</strong> stored — only its BCrypt hash.</li>
 *   <li>Tokens expire {@code 10 minutes} after creation ({@code expiresAt}).</li>
 *   <li>After {@code MAX_ATTEMPTS} failed verifications the token is locked.</li>
 *   <li>Once verified, {@code verified = true} and a subsequent
 *       {@code POST /auth/reset-password} call sets {@code used = true},
 *       preventing any re-use.</li>
 *   <li>Requesting a new OTP for the same email invalidates (sets {@code used = true})
 *       all previous pending tokens for that email.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(
        name = "password_reset_tokens",
        indexes = {
                @Index(name = "idx_prt_email", columnList = "email"),
                @Index(name = "idx_prt_email_active", columnList = "email, used, verified")
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetToken extends BaseEntity {

    /**
     * The maximum number of failed OTP verification attempts before the token
     * is permanently locked.
     */
    public static final int MAX_ATTEMPTS = 5;

    /** OTP validity window in minutes. */
    public static final int OTP_EXPIRY_MINUTES = 10;

    /**
     * The email address for which the OTP was requested.
     * This is the lookup key — it is not a foreign key so that
     * the generic response can be returned regardless of whether
     * the email is registered.
     */
    @Column(name = "email", nullable = false, length = 150)
    private String email;

    /**
     * BCrypt hash of the raw 6-digit OTP.
     * The raw OTP is never persisted.
     */
    @Column(name = "otp_hash", nullable = false, length = 255)
    private String otpHash;

    /**
     * Timestamp after which the OTP is no longer valid.
     * Set to {@code now + 10 minutes} at creation time.
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * Count of consecutive failed verification attempts.
     * Incremented on every wrong OTP submission. When this reaches
     * {@link #MAX_ATTEMPTS} the token is permanently unusable.
     */
    @Builder.Default
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount = 0;

    /**
     * {@code true} after the OTP has been successfully verified.
     * A verified token may be exchanged once for a password reset.
     * Set to {@code true} by the verify-OTP endpoint.
     */
    @Builder.Default
    @Column(name = "verified", nullable = false)
    private boolean verified = false;

    /**
     * {@code true} after the reset has been consumed.
     * Prevents the same verified token from being used more than once.
     * Also set to {@code true} on any previous tokens when a new OTP
     * is requested, invalidating them.
     */
    @Builder.Default
    @Column(name = "used", nullable = false)
    private boolean used = false;

    // ── Convenience helpers ────────────────────────────────────────────────────

    /**
     * Returns {@code true} if this token has expired (wall-clock check).
     *
     * @return {@code true} when {@code expiresAt} is before now
     */
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    /**
     * Returns {@code true} if the maximum number of failed attempts has been reached.
     *
     * @return {@code true} when {@code attemptCount >= MAX_ATTEMPTS}
     */
    public boolean isMaxAttemptsExceeded() {
        return attemptCount >= MAX_ATTEMPTS;
    }

    /**
     * Returns {@code true} if this token is active — not used, not expired,
     * and not exceeding max attempts.
     *
     * @return {@code true} when the token can still be used
     */
    public boolean isActive() {
        return !used && !isExpired() && !isMaxAttemptsExceeded();
    }
}
