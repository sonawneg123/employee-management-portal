package com.company.employeemanagement.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that intercepts every incoming HTTP request and extracts
 * a JWT Bearer token from the {@code Authorization} header.
 *
 * <p>If a valid, non-expired token is found and no authentication is already
 * present in the {@link SecurityContextHolder}, the filter:
 * <ol>
 *   <li>Extracts the username (email) from the token.</li>
 *   <li>Loads the {@link UserDetails} from the database.</li>
 *   <li>Validates the token against the loaded {@link UserDetails}.</li>
 *   <li>Registers a {@link UsernamePasswordAuthenticationToken} in the
 *       security context so that downstream components see an authenticated
 *       principal.</li>
 * </ol>
 *
 * <p>Extends {@link OncePerRequestFilter} to guarantee single execution
 * per request regardless of the servlet container's dispatch type.
 *
 * @author Employee Management Portal Team
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

    /** HTTP header carrying the Bearer token. */
    private static final String AUTHORIZATION_HEADER = "Authorization";

    /** Required prefix for the Bearer token value. */
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;

    /**
     * Constructs the filter with the required JWT service and user details service.
     *
     * @param jwtService         service for parsing and validating JWTs
     * @param userDetailsService Spring Security service for loading user details
     */
    public JwtAuthenticationFilter(final JwtService jwtService,
                                    final UserDetailsService userDetailsService) {
        this.jwtService = jwtService;
        this.userDetailsService = userDetailsService;
    }

    /**
     * Core filter logic executed once per request.
     *
     * <p>Extracts the JWT, validates it, and sets the security context
     * if the token is valid. Passes the request to the next filter in the
     * chain regardless of the outcome.
     *
     * @param request     the incoming HTTP request
     * @param response    the outgoing HTTP response
     * @param filterChain the remaining filter chain
     * @throws ServletException if a servlet error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doFilterInternal(
            @NonNull final HttpServletRequest request,
            @NonNull final HttpServletResponse response,
            @NonNull final FilterChain filterChain) throws ServletException, IOException {

        final String token = extractTokenFromRequest(request);

        if (token == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (!jwtService.validateToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        final String username = jwtService.extractUsername(token);

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            if (jwtService.isTokenValid(token, userDetails)) {
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
                log.debug("Authenticated user '{}' via JWT", username);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extracts the JWT string from the {@code Authorization} header.
     *
     * @param request the incoming HTTP request
     * @return the raw JWT string, or {@code null} if the header is absent or
     *         does not start with {@code "Bearer "}
     */
    private String extractTokenFromRequest(final HttpServletRequest request) {
        final String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        if (StringUtils.hasText(authHeader) && authHeader.startsWith(BEARER_PREFIX)) {
            return authHeader.substring(BEARER_PREFIX.length());
        }
        return null;
    }
}
