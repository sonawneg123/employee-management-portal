package com.company.employeemanagement.security;

import com.company.employeemanagement.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Service responsible for generating, parsing, and validating JSON Web Tokens.
 *
 * <p>Uses HMAC-SHA256 (HS256) as the signing algorithm. The signing key is
 * derived from the {@code app.jwt.secret} property and must be at least
 * 256 bits long.
 *
 * <p>Tokens carry the following standard claims:
 * <ul>
 *   <li>{@code sub} — the authenticated user's email address</li>
 *   <li>{@code iat} — issued-at timestamp</li>
 *   <li>{@code exp} — expiration timestamp</li>
 * </ul>
 * and the following private claim:
 * <ul>
 *   <li>{@code roles} — list of Spring Security role strings</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    /** Claim key under which the user's roles are stored in the JWT payload. */
    private static final String CLAIM_ROLES = "roles";

    private final JwtProperties jwtProperties;

    /**
     * Constructs a {@link JwtService} with the required JWT configuration.
     *
     * @param jwtProperties JWT configuration properties bound from
     *                      {@code application.properties}
     */
    public JwtService(final JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    /**
     * Generates a signed JWT access token for the authenticated user.
     *
     * @param userDetails the Spring Security {@link UserDetails} for which to
     *                    generate the token
     * @return a compact, URL-safe JWT string
     */
    public String generateToken(final UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put(CLAIM_ROLES, roles);
        return buildToken(extraClaims, userDetails, jwtProperties.expirationMs());
    }

    /**
     * Extracts the subject (email address) from a JWT.
     *
     * @param token the compact JWT string
     * @return the subject claim value
     */
    public String extractUsername(final String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Validates a JWT against the supplied {@link UserDetails}.
     *
     * <p>Validation checks:
     * <ol>
     *   <li>The token's subject matches the {@code UserDetails} username.</li>
     *   <li>The token has not expired.</li>
     * </ol>
     *
     * @param token       the JWT to validate
     * @param userDetails the user against whom to validate the token
     * @return {@code true} if the token is valid
     */
    public boolean isTokenValid(final String token, final UserDetails userDetails) {
        final String username = extractUsername(token);
        return username.equals(userDetails.getUsername()) && !isTokenExpired(token);
    }

    /**
     * Parses and validates a JWT string, returning {@code true} if the signature
     * is valid and the token is not structurally malformed.
     *
     * <p>This method is used by {@link JwtAuthenticationFilter} before loading
     * the {@link UserDetails}, so it does not check token expiry.
     *
     * @param token the JWT to validate
     * @return {@code true} if the token can be parsed and the signature is valid
     */
    public boolean validateToken(final String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException ex) {
            log.warn("Invalid JWT signature: {}", ex.getMessage());
        } catch (MalformedJwtException ex) {
            log.warn("Invalid JWT token: {}", ex.getMessage());
        } catch (ExpiredJwtException ex) {
            log.warn("JWT token is expired: {}", ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            log.warn("JWT token is unsupported: {}", ex.getMessage());
        } catch (IllegalArgumentException ex) {
            log.warn("JWT claims string is empty: {}", ex.getMessage());
        }
        return false;
    }

    /**
     * Extracts a single claim from the token using the provided resolver function.
     *
     * @param <T>            the return type of the claim
     * @param token          the JWT string
     * @param claimsResolver function that extracts the desired value from {@link Claims}
     * @return the extracted claim value
     */
    public <T> T extractClaim(final String token, final Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    // ──────────────────── private helpers ─────────────────────────────────────

    /**
     * Builds and signs a JWT with the given extra claims and expiration.
     *
     * @param extraClaims  additional payload claims
     * @param userDetails  the principal
     * @param expirationMs token lifetime in milliseconds
     * @return the signed compact JWT string
     */
    private String buildToken(final Map<String, Object> extraClaims,
                               final UserDetails userDetails,
                               final long expirationMs) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(now))
                .expiration(new Date(now + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Parses all claims from the given JWT.
     *
     * @param token the compact JWT string
     * @return all parsed {@link Claims}
     */
    private Claims extractAllClaims(final String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Returns whether the token's expiration date is before the current time.
     *
     * @param token the JWT to check
     * @return {@code true} if the token is expired
     */
    private boolean isTokenExpired(final String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    /**
     * Derives the HMAC-SHA signing key from the configured secret string.
     *
     * @return the {@link SecretKey} used for signing and verification
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtProperties.secret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
