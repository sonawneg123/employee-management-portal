package com.company.employeemanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Strongly-typed configuration properties for JWT generation and validation.
 *
 * <p>Values are bound from the {@code app.jwt.*} namespace in
 * {@code application.properties}:
 * <ul>
 *   <li>{@code app.jwt.secret} — HMAC-SHA256 signing key (minimum 256 bits)</li>
 *   <li>{@code app.jwt.expiration-ms} — access token TTL in milliseconds</li>
 *   <li>{@code app.jwt.refresh-expiration-ms} — refresh token TTL in milliseconds</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(

        /**
         * Base64-encoded or plaintext HMAC-SHA256 signing secret.
         * Must be at least 32 bytes (256 bits) long.
         */
        String secret,

        /**
         * Access token validity period in milliseconds.
         * Default: 86 400 000 ms (24 hours).
         */
        long expirationMs,

        /**
         * Refresh token validity period in milliseconds.
         * Default: 604 800 000 ms (7 days).
         */
        long refreshExpirationMs
) {
}
