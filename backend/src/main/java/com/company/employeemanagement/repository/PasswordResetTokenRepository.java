package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PasswordResetToken}.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /**
     * Finds the most recent active (not used, not expired, not exceeded) token
     * for the given email address.
     *
     * @param email the email address
     * @return the latest active token, if present
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.email = :email
              AND t.used = false
              AND t.verified = false
              AND t.expiresAt > :now
              AND t.attemptCount < :maxAttempts
            ORDER BY t.createdAt DESC
            """)
    Optional<PasswordResetToken> findActiveTokenByEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now,
            @Param("maxAttempts") int maxAttempts);

    /**
     * Finds the most recent verified-but-not-yet-used token for the given email.
     * Used by the reset-password step to confirm OTP was properly verified.
     *
     * @param email the email address
     * @return the verified token, if present
     */
    @Query("""
            SELECT t FROM PasswordResetToken t
            WHERE t.email = :email
              AND t.used = false
              AND t.verified = true
              AND t.expiresAt > :now
            ORDER BY t.createdAt DESC
            """)
    Optional<PasswordResetToken> findVerifiedTokenByEmail(
            @Param("email") String email,
            @Param("now") LocalDateTime now);

    /**
     * Invalidates all pending (not used) tokens for the given email address.
     * Called when a new OTP request is made to prevent old tokens from remaining active.
     *
     * @param email the email address
     */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.used = true WHERE t.email = :email AND t.used = false")
    void invalidateAllPendingTokens(@Param("email") String email);
}
