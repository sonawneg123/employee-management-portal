package com.company.employeemanagement.security;

import com.company.employeemanagement.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Unit tests for {@link JwtService}.
 *
 * <p>Uses a fixed test secret (≥256 bits) so that signing and verification
 * are deterministic without involving a real application context.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("JwtService")
class JwtServiceTest {

    private static final String TEST_SECRET =
            "ThisIsA256BitTestSecretKeyForJwtUnitTestsDoNotUseInProduction!!";
    private static final long EXPIRY_MS         = 3_600_000L;   // 1 hour
    private static final long REFRESH_EXPIRY_MS = 86_400_000L;  // 24 hours

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties(TEST_SECRET, EXPIRY_MS, REFRESH_EXPIRY_MS);
        jwtService = new JwtService(props);
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private UserDetails buildUser(final String email, final String... roles) {
        List<SimpleGrantedAuthority> authorities =
                List.of(roles).stream()
                        .map(SimpleGrantedAuthority::new)
                        .toList();
        return User.withUsername(email)
                .password("hashed-password")
                .authorities(authorities)
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Token generation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("generateToken()")
    class GenerateToken {

        @Test
        @DisplayName("generates a non-blank JWT string")
        void generatesNonBlankToken() {
            UserDetails user = buildUser("alice@example.com", "ROLE_EMPLOYEE");
            String token = jwtService.generateToken(user);
            assertThat(token).isNotBlank();
        }

        @Test
        @DisplayName("embeds the correct subject (email) in the token")
        void embedsCorrectSubject() {
            UserDetails user = buildUser("bob@example.com", "ROLE_ADMIN");
            String token = jwtService.generateToken(user);
            assertThat(jwtService.extractUsername(token)).isEqualTo("bob@example.com");
        }
        @Test
@DisplayName("embeds the roles claim in the token")
void embedsRolesClaim() {
    UserDetails user = buildUser("carol@example.com", "ROLE_HR", "ROLE_EMPLOYEE");
    String token = jwtService.generateToken(user);

   @SuppressWarnings("unchecked")
List<String> roles = (List<String>) jwtService.extractClaim(
        token,
        claims -> claims.get("roles", List.class)
);

    assertThat(roles)
            .hasSize(2)
            .contains("ROLE_HR", "ROLE_EMPLOYEE");
}
        @Test
        @DisplayName("generates distinct tokens for the same user on successive calls")
        void generatesDifferentTokensSuccessively() throws InterruptedException {
            UserDetails user = buildUser("dave@example.com", "ROLE_EMPLOYEE");
            String token1 = jwtService.generateToken(user);
            Thread.sleep(1_001); // ensure different iat second
            String token2 = jwtService.generateToken(user);
            assertThat(token1).isNotEqualTo(token2);
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Token validation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("isTokenValid()")
    class IsTokenValid {

        @Test
        @DisplayName("returns true for a freshly generated token matching the user")
        void returnsTrueForValidToken() {
            UserDetails user = buildUser("eve@example.com", "ROLE_EMPLOYEE");
            String token = jwtService.generateToken(user);
            assertThat(jwtService.isTokenValid(token, user)).isTrue();
        }

        @Test
        @DisplayName("returns false when the username in the token does not match the UserDetails")
        void returnsFalseForSubjectMismatch() {
            UserDetails alice = buildUser("alice@example.com", "ROLE_EMPLOYEE");
            UserDetails mallory = buildUser("mallory@example.com", "ROLE_EMPLOYEE");
            String token = jwtService.generateToken(alice);
            assertThat(jwtService.isTokenValid(token, mallory)).isFalse();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Structural token validation
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("validateToken()")
    class ValidateToken {

        @Test
        @DisplayName("returns true for a well-formed, unexpired token")
        void returnsTrueForWellFormedToken() {
            UserDetails user = buildUser("frank@example.com", "ROLE_EMPLOYEE");
            String token = jwtService.generateToken(user);
            assertThat(jwtService.validateToken(token)).isTrue();
        }

        @Test
        @DisplayName("returns false for a completely invalid string")
        void returnsFalseForGarbage() {
            assertThat(jwtService.validateToken("this.is.not.a.jwt")).isFalse();
        }

        @Test
        @DisplayName("returns false for a blank string")
        void returnsFalseForBlank() {
            assertThat(jwtService.validateToken("")).isFalse();
        }

        @Test
        @DisplayName("returns false for a token signed with a different secret")
        void returnsFalseForWrongSecret() {
            JwtProperties differentProps = new JwtProperties(
                    "TotallyDifferentSecretKeyThatIs256BitsLongButNotTheSame!!", EXPIRY_MS, REFRESH_EXPIRY_MS);
            JwtService otherService = new JwtService(differentProps);
            UserDetails user = buildUser("grace@example.com", "ROLE_EMPLOYEE");
            String foreignToken = otherService.generateToken(user);
            assertThat(jwtService.validateToken(foreignToken)).isFalse();
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Claim extraction
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("extractClaim()")
    class ExtractClaim {

        @Test
        @DisplayName("extracts the expiration claim as a Date")
        void extractsExpiration() {
            UserDetails user = buildUser("heidi@example.com", "ROLE_EMPLOYEE");
            String token = jwtService.generateToken(user);
            java.util.Date expiration = jwtService.extractClaim(token, Claims::getExpiration);
            assertThat(expiration).isAfter(new java.util.Date());
        }

        @Test
        @DisplayName("extracting a claim from a valid token does not throw")
        void noExceptionForValidToken() {
            UserDetails user = buildUser("ivan@example.com", "ROLE_EMPLOYEE");
            String token = jwtService.generateToken(user);
            assertThatCode(() -> jwtService.extractClaim(token, Claims::getSubject))
                    .doesNotThrowAnyException();
        }
    }
}
