package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.service.AuthService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

/**
 * Implementation of {@link AuthService} that handles user registration and
 * JWT-based login.
 *
 * <p>Registration assigns the default {@code ROLE_EMPLOYEE} role to every new
 * account. A JWT access token is generated immediately on successful registration
 * so that the client does not need to make a separate login call.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final String DEFAULT_ROLE = "ROLE_EMPLOYEE";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param userRepository       repository for user persistence
     * @param roleRepository       repository for role lookups
     * @param passwordEncoder      BCrypt encoder for password hashing
     * @param jwtService           service for JWT generation
     * @param authenticationManager Spring Security authentication manager
     * @param userDetailsService   service for loading UserDetails by email
     * @param jwtProperties        JWT configuration properties
     */
    public AuthServiceImpl(final UserRepository userRepository,
                            final RoleRepository roleRepository,
                            final PasswordEncoder passwordEncoder,
                            final JwtService jwtService,
                            final AuthenticationManager authenticationManager,
                            final UserDetailsService userDetailsService,
                            final JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtProperties = jwtProperties;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Steps:
     * <ol>
     *   <li>Guard against duplicate email.</li>
     *   <li>Hash the password with BCrypt (strength 12).</li>
     *   <li>Resolve the default {@code ROLE_EMPLOYEE} role.</li>
     *   <li>Persist the new {@link User}.</li>
     *   <li>Generate and return a JWT token.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthResponse register(final RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        Role defaultRole = roleRepository.findByName(DEFAULT_ROLE)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", DEFAULT_ROLE));

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .isEnabled(true)
                .isLocked(false)
                .roles(Set.of(defaultRole))
                .build();

        userRepository.save(user);

        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user, userDetails);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates credential verification to Spring Security's
     * {@link AuthenticationManager}. On success, loads the fresh
     * {@link UserDetails} and issues a JWT.
     */
    @Override
    @Transactional(readOnly = true)
    public AuthResponse login(final LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user, userDetails);
    }

    /**
     * Assembles an {@link AuthResponse} from a generated token, the user entity,
     * and the loaded {@link UserDetails}.
     *
     * @param token       the signed JWT access token
     * @param user        the persisted user entity
     * @param userDetails the Spring Security user details
     * @return a fully populated {@link AuthResponse}
     */
    private AuthResponse buildAuthResponse(final String token,
                                            final User user,
                                            final UserDetails userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        return new AuthResponse(
                token,
                "Bearer",
                jwtProperties.expirationMs() / 1000L,
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles
        );
    }
}
