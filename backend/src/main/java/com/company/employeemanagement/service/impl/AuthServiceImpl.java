package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.config.JwtProperties;
import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.DuplicateResourceException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.service.AuthService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link AuthService} that handles user registration and
 * JWT-based login.
 *
 * <p>Registration assigns the default {@code ROLE_EMPLOYEE} role to every new
 * account. A JWT access token is generated immediately on successful registration
 * so that the client does not need to make a separate login call.
 *
 * <p>When registering a {@code ROLE_EMPLOYEE} account, a linked {@link Employee}
 * record is automatically created so that leave requests, attendance, and reviews
 * work immediately after registration without requiring HR to manually link the
 * accounts.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);

    private static final String DEFAULT_ROLE    = "ROLE_EMPLOYEE";
    private static final String ALLOWED_ROLE_HR = "ROLE_HR";

    /** Counter used to generate unique employee codes for self-registered employees. */
    private static final AtomicLong EMP_CODE_COUNTER = new AtomicLong(
            System.currentTimeMillis() % 100_000L);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param userRepository        repository for user persistence
     * @param roleRepository        repository for role lookups
     * @param employeeRepository    repository for employee record persistence
     * @param departmentRepository  repository for department lookups (default dept for new employees)
     * @param passwordEncoder       BCrypt encoder for password hashing
     * @param jwtService            service for JWT generation
     * @param authenticationManager Spring Security authentication manager
     * @param userDetailsService    service for loading UserDetails by email
     * @param jwtProperties         JWT configuration properties
     */
    public AuthServiceImpl(final UserRepository userRepository,
                            final RoleRepository roleRepository,
                            final EmployeeRepository employeeRepository,
                            final DepartmentRepository departmentRepository,
                            final PasswordEncoder passwordEncoder,
                            final JwtService jwtService,
                            final AuthenticationManager authenticationManager,
                            final UserDetailsService userDetailsService,
                            final JwtProperties jwtProperties) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
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
     *   <li>Validate the requested role — only {@code ROLE_HR} and
     *       {@code ROLE_EMPLOYEE} are permitted; anything else triggers a
     *       {@code 400 Bad Request}.</li>
     *   <li>Hash the password with BCrypt (strength 12).</li>
     *   <li>Resolve the target role.</li>
     *   <li>Persist the new {@link User}.</li>
     *   <li>If the role is {@code ROLE_EMPLOYEE}, automatically create and link
     *       an {@link Employee} record so leave/attendance/review features work
     *       immediately after registration.</li>
     *   <li>Generate and return a JWT token.</li>
     * </ol>
     */
    @Override
    @Transactional
    public AuthResponse register(final RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }

        // Determine the role to assign. Only ROLE_EMPLOYEE and ROLE_HR are
        // accepted from the public registration endpoint. Any other value
        // (including ROLE_ADMIN and ROLE_MANAGER) is rejected with 400.
        String requestedRole = request.role();
        final String roleName;
        if (requestedRole == null || requestedRole.isBlank()) {
            roleName = DEFAULT_ROLE;
        } else if (ALLOWED_ROLE_HR.equals(requestedRole) || DEFAULT_ROLE.equals(requestedRole)) {
            roleName = requestedRole;
        } else {
            throw new IllegalArgumentException(
                    "Role '" + requestedRole + "' cannot be assigned via registration. "
                    + "Allowed values: ROLE_EMPLOYEE, ROLE_HR.");
        }

        Role defaultRole = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        // Use a mutable HashSet so that JPA and AdminService can later modify the roles
        // collection without triggering UnsupportedOperationException.
        java.util.Set<Role> roles = new HashSet<>();
        roles.add(defaultRole);

        User user = User.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .firstName(request.firstName())
                .lastName(request.lastName())
                .isEnabled(true)
                .isLocked(false)
                .roles(roles)
                .build();

        User savedUser = userRepository.save(user);
        log.info("Registration: created user id={} email={} role={}",
                savedUser.getId(), savedUser.getEmail(), roleName);

        // ── Auto-create Employee record for ROLE_EMPLOYEE accounts ──────────────
        // This ensures JWT → User → Employee resolution works immediately after
        // registration without requiring HR to manually link the accounts.
        if (DEFAULT_ROLE.equals(roleName)) {
            createEmployeeRecord(savedUser);
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(savedUser.getEmail());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, savedUser, userDetails);
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
        // authenticationManager.authenticate throws BadCredentialsException on wrong credentials.
        // GlobalExceptionHandler maps this to HTTP 401 with {"detail":"Invalid email or password."}.
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));

        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));

        log.info("Login: user id={} email={} roles={}",
                user.getId(), user.getEmail(),
                user.getRoles().stream().map(r -> r.getName()).toList());

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.email());
        String token = jwtService.generateToken(userDetails);

        return buildAuthResponse(token, user, userDetails);
    }

    // ── private helpers ────────────────────────────────────────────────────────

    /**
     * Automatically creates an {@link Employee} record linked to the given user.
     *
     * <p>Uses any available department as the employee's department (preferring
     * the "General" / "GEN" department seeded by Flyway). If no department
     * exists at all the employee record is still created without a department —
     * this is a fallback that should not occur in a properly initialised environment.
     *
     * @param user the newly registered user to link
     */
    private void createEmployeeRecord(final User user) {
        // Skip if an employee record already exists for this user (idempotent)
        if (employeeRepository.findByUserId(user.getId()).isPresent()) {
            log.debug("Employee record already exists for user id={}", user.getId());
            return;
        }

        // Resolve the default department (prefer "GEN" / "General")
        // If no department exists at all, create a "General" department on-the-fly so that
        // the employee record can always be persisted (department_id is NOT NULL in DB schema).
        Department department = departmentRepository.findByCode("GEN")
                .or(() -> departmentRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    log.info("Registration: no department found — auto-creating 'General' dept for user id={}", user.getId());
                    Department newDept = Department.builder()
                            .name("General")
                            .code("GEN")
                            .build();
                    return departmentRepository.save(newDept);
                });

        // Generate a unique employee code. Pattern: REG-<10-digit-timestamp-suffix>
        String employeeCode = generateUniqueEmployeeCode();

        Employee employee = Employee.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .employeeCode(employeeCode)
                .department(department)
                .jobTitle("Employee")
                .dateOfJoining(LocalDate.now())
                .salary(BigDecimal.ZERO)
                .status(EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(employee);
        log.info("Registration: auto-created Employee id={} code={} for user id={}",
                saved.getId(), saved.getEmployeeCode(), user.getId());
    }

    /**
     * Generates a unique employee code of the form {@code REG-XXXXXXXXXX}.
     * Retries with a different suffix if the generated code already exists.
     *
     * @return a unique employee code
     */
    private String generateUniqueEmployeeCode() {
        for (int attempt = 0; attempt < 20; attempt++) {
            String code = String.format("REG-%010d", EMP_CODE_COUNTER.incrementAndGet());
            if (!employeeRepository.existsByEmployeeCode(code)) {
                return code;
            }
        }
        // Fallback: use timestamp + random suffix
        return String.format("REG-%010d", System.currentTimeMillis() % 10_000_000_000L);
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
