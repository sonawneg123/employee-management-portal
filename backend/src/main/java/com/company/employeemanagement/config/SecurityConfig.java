package com.company.employeemanagement.config;

import com.company.employeemanagement.security.JwtAuthenticationFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

// Note: JwtProperties is NOT injected here — it is consumed by JwtService directly.
// SecurityConfig only needs UserDetailsService and JwtAuthenticationFilter.

/**
 * Central Spring Security configuration for the Employee Management Portal.
 *
 * <p>Security model:
 * <ul>
 *   <li><strong>Stateless</strong> — no HTTP sessions; every request is
 *       authenticated via a JWT Bearer token.</li>
 *   <li><strong>CSRF disabled</strong> — safe for stateless REST APIs that
 *       do not use cookie-based session authentication.</li>
 *   <li><strong>CORS</strong> — configured via a {@link CorsConfigurationSource}
 *       bean; allowed origins are read from {@code app.cors.allowed-origins}.</li>
 *   <li><strong>Method security</strong> enabled via
 *       {@link EnableMethodSecurity} — allows {@code @PreAuthorize} annotations
 *       on service/controller methods.</li>
 * </ul>
 *
 * <p>Endpoint authorisation rules:
 * <ul>
 *   <li>{@code POST /auth/**} — public (login, register)</li>
 *   <li>{@code GET /v3/api-docs/**}, {@code /swagger-ui/**} — public</li>
 *   <li>{@code GET /actuator/health} — public</li>
 *   <li>All other endpoints require authentication.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties(JwtProperties.class)   // registers JwtProperties bean for JwtService
public class SecurityConfig {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    /**
     * Constructs the security configuration with all required dependencies.
     *
     * @param userDetailsService       service for loading user credentials from the DB
     * @param jwtAuthenticationFilter  filter that validates Bearer tokens on each request
     */
    public SecurityConfig(final UserDetailsService userDetailsService,
                           final JwtAuthenticationFilter jwtAuthenticationFilter) {
        this.userDetailsService = userDetailsService;
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    }

    /**
     * Configures the main {@link SecurityFilterChain}.
     *
     * @param http the {@link HttpSecurity} builder
     * @return the fully configured filter chain
     * @throws Exception if an error occurs during configuration
     */
    @Bean
    public SecurityFilterChain securityFilterChain(final HttpSecurity http) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // ── Public endpoints ────────────────────────────
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // ── Employee endpoints ───────────────────────────
                        .requestMatchers(HttpMethod.GET, "/employees/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/employees/**")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/employees/**")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.DELETE, "/employees/**")
                                .hasRole("ADMIN")
                        // ── Department endpoints ─────────────────────────
                        .requestMatchers(HttpMethod.GET, "/departments/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/departments/**")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/departments/**")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.DELETE, "/departments/**")
                                .hasRole("ADMIN")
                        // ── Leave request endpoints ──────────────────────
                        .requestMatchers(HttpMethod.GET, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── All other requests require authentication ────
                        .anyRequest().authenticated()
                )
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    /**
     * Creates a {@link DaoAuthenticationProvider} that uses the custom
     * {@link UserDetailsService} and BCrypt password encoder.
     *
     * @return the configured {@link AuthenticationProvider}
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    /**
     * Exposes the {@link AuthenticationManager} from the
     * {@link AuthenticationConfiguration} as a Spring bean so that it can
     * be injected into the authentication service.
     *
     * @param config the authentication configuration provided by Spring Boot
     * @return the default {@link AuthenticationManager}
     * @throws Exception if the manager cannot be resolved
     */
    @Bean
    public AuthenticationManager authenticationManager(
            final AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * Registers a {@link BCryptPasswordEncoder} with cost factor 12 as
     * the application-wide {@link PasswordEncoder}.
     *
     * <p>BCrypt with strength 12 is the recommended minimum for production
     * systems as of 2024.
     *
     * @return the configured {@link PasswordEncoder}
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * Configures CORS to accept requests from the origins listed in
     * {@code app.cors.allowed-origins}.
     *
     * <p>All standard HTTP methods and headers are permitted.
     * {@code allowCredentials} is set to {@code true} only alongside explicit
     * origin patterns (never with a bare {@code "*"} wildcard, which is
     * forbidden by the CORS specification when credentials are enabled).
     *
     * @return the CORS configuration source
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Explicit pattern required — wildcard "*" is invalid with allowCredentials=true
        config.setAllowedOriginPatterns(List.of("http://localhost:5173", "http://localhost:3000",
                "http://localhost:80", "https://*.company.com"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of("Authorization"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
