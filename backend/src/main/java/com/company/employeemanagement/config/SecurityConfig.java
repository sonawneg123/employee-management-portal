package com.company.employeemanagement.config;

import com.company.employeemanagement.security.JwtAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
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
 * <p>Fine-grained resource-ownership checks (e.g., EMPLOYEE accessing only their
 * own record) are enforced via {@code @PreAuthorize} at the controller layer and
 * ownership logic in the service layer. URL-level rules here restrict by broad role
 * only.
 *
 * @author Employee Management Portal Team
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@EnableConfigurationProperties({JwtProperties.class,
        com.company.employeemanagement.config.FileStorageProperties.class})
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
                // Return 401/403 JSON, not a redirect
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(unauthorizedEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler())
                )
                .authorizeHttpRequests(auth -> auth
                        // ── Public endpoints ────────────────────────────
                        .requestMatchers(HttpMethod.POST,
                                "/auth/register", "/auth/login",
                                "/auth/forgot-password", "/auth/verify-otp", "/auth/reset-password"
                        ).permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**",
                                "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        // ── Admin-only management endpoints ──────────────
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // ── Settings endpoints ───────────────────────────
                        .requestMatchers(HttpMethod.POST, "/settings/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── Reviews endpoints ────────────────────────────
                        .requestMatchers(HttpMethod.GET, "/reviews/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/reviews/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/reviews/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/reviews/**")
                                .hasRole("ADMIN")
                        // ── Dashboard endpoints (all authenticated roles) ──
                        .requestMatchers(HttpMethod.GET, "/dashboard/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
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
                        // ── Leave approval / rejection — ADMIN, HR, MANAGER ─
                        .requestMatchers(HttpMethod.POST, "/leaves/*/approve", "/leaves/*/reject")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // ── Leave request endpoints (create/update/cancel by any auth user) ──
                        .requestMatchers(HttpMethod.GET, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PUT, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.DELETE, "/leaves/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── Attendance — self-service (my/checkin/checkout) for all roles ──────────
                        .requestMatchers(HttpMethod.GET, "/attendance/my")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Employee self-service check-in / check-out — must be listed BEFORE the
                        // more general POST /attendance/** rule (Spring evaluates rules in order).
                        .requestMatchers(HttpMethod.POST, "/attendance/checkin", "/attendance/checkout")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.GET, "/attendance/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.POST, "/attendance/**")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.PUT, "/attendance/**")
                                .hasAnyRole("ADMIN", "HR")
                        // ── Profile — all authenticated roles ────────────────
                        .requestMatchers("/profile/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── AI chat — any authenticated user ─────────────────
                        .requestMatchers(HttpMethod.POST, "/ai/chat")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── AI Copilot (Phase 7E) — any authenticated user ────
                        .requestMatchers(HttpMethod.POST, "/ai/agent/chat")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── RAG document management — ADMIN or HR only ────────
                        .requestMatchers(HttpMethod.POST, "/ai/rag/documents")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.GET, "/ai/rag/documents/**")
                                .hasAnyRole("ADMIN", "HR")
                        .requestMatchers(HttpMethod.DELETE, "/ai/rag/documents/**")
                                .hasRole("ADMIN")
                        // ── RAG search — any authenticated user ───────────────
                        .requestMatchers(HttpMethod.POST, "/ai/rag/search")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── Task management endpoints ─────────────────────────
                        // Status update — all authenticated roles (employee scoped + limited transitions)
                        .requestMatchers(HttpMethod.PATCH, "/tasks/*/status")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Task reassign — privileged roles only
                        .requestMatchers(HttpMethod.POST, "/tasks/*/reassign")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // Task submission (employee submits work) — all authenticated roles
                        .requestMatchers(HttpMethod.POST, "/tasks/*/submissions")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Task submission queries — all authenticated roles
                        .requestMatchers(HttpMethod.GET, "/tasks/*/submissions/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Task comments — all authenticated roles (IDOR enforced in service)
                        .requestMatchers(HttpMethod.GET, "/tasks/*/comments")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.POST, "/tasks/*/comments")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Task activity timeline — all authenticated roles (IDOR enforced in service)
                        .requestMatchers(HttpMethod.GET, "/tasks/*/activities")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Task attachments — upload/delete privileged only; download/list for all
                        .requestMatchers(HttpMethod.POST, "/tasks/*/attachments")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.DELETE, "/tasks/*/attachments/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/tasks/*/attachments/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Dashboard / workload stats — privileged roles only
                        .requestMatchers(HttpMethod.GET, "/tasks/dashboard-stats",
                                "/tasks/workload-summary", "/tasks/workload/**",
                                "/tasks/employee-availability")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // Task queries — all authenticated roles (employees scoped server-side)
                        .requestMatchers(HttpMethod.GET, "/tasks/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Task creation / full update — privileged roles only
                        .requestMatchers(HttpMethod.POST, "/tasks/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.PUT, "/tasks/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // Task deletion — privileged roles only
                        .requestMatchers(HttpMethod.DELETE, "/tasks/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // ── Task submission review endpoints ─────────────────────
                        // Resubmit — employee (own submission only, enforced in service)
                        .requestMatchers(HttpMethod.PUT, "/task-submissions/*/resubmit")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Approve / request-changes — privileged only (enforced at controller + service)
                        .requestMatchers(HttpMethod.POST, "/task-submissions/*/approve",
                                "/task-submissions/*/request-changes")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // Attachment download — all authenticated roles (ownership enforced in service)
                        .requestMatchers(HttpMethod.GET, "/task-submissions/*/attachment")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // ── AI Task Review — Phase 7A — privileged roles only ──────────────
                        .requestMatchers(HttpMethod.POST, "/task-submissions/*/ai-review")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/task-submissions/*/ai-review",
                                "/task-submissions/*/ai-reviews")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/task-ai-reviews/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // ── AI Feedback & Insights — Phase 7D ─────────────────────────────
                        // Employee-safe feedback and history — all authenticated roles (IDOR in service)
                        .requestMatchers(HttpMethod.GET, "/task-submissions/*/ai-feedback",
                                "/task-submissions/*/ai-history")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        // Manager-only AI analytics
                        .requestMatchers(HttpMethod.GET, "/tasks/*/ai-trend",
                                "/tasks/*/ai-insights")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        .requestMatchers(HttpMethod.GET, "/ai/dashboard-summary")
                                .hasAnyRole("ADMIN", "HR", "MANAGER")
                        // ── Notification endpoints — all authenticated roles (self-scoped) ──
                        .requestMatchers(HttpMethod.GET, "/notifications/**")
                                .hasAnyRole("ADMIN", "HR", "MANAGER", "EMPLOYEE")
                        .requestMatchers(HttpMethod.PATCH, "/notifications/**")
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
     * Returns a custom {@link AuthenticationEntryPoint} that writes a JSON
     * {@code 401 Unauthorized} response instead of the default HTML redirect.
     *
     * <p>This prevents missing/invalid tokens from being swallowed as 302
     * redirects on REST clients.
     *
     * @return the entry point
     */
    @Bean
    public AuthenticationEntryPoint unauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":401,"title":"Unauthorized","detail":"Authentication is required to access this resource."}
                    """);
        };
    }

    /**
     * Returns a custom {@link AccessDeniedHandler} that writes a JSON
     * {@code 403 Forbidden} response instead of the default empty response.
     *
     * @return the access-denied handler
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                    {"status":403,"title":"Access Denied","detail":"You do not have permission to perform this action."}
                    """);
        };
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
