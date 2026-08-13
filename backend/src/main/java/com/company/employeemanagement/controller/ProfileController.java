package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.UpdateProfileRequest;
import com.company.employeemanagement.dto.response.ProfileResponse;
import com.company.employeemanagement.entity.Department;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * REST controller exposing the authenticated user's own profile.
 *
 * <p>Base path: {@code /api/profile}
 *
 * <p>All endpoints require a valid JWT Bearer token. The profile is always
 * scoped to the currently authenticated principal — no ID parameter is needed.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/profile")
@Tag(name = "Profile", description = "Authenticated user's own profile management")
@SecurityRequirement(name = "BearerAuth")
public class ProfileController {

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(ProfileController.class);

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;
    private final DepartmentRepository departmentRepository;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param securityUtils       helper for current-principal inspection
     * @param userRepository      repository for user account lookups and saves
     * @param employeeRepository  repository for employee record lookups and saves
     * @param departmentRepository repository for department lookups (auto-create Employee)
     */
    public ProfileController(final SecurityUtils securityUtils,
                              final UserRepository userRepository,
                              final EmployeeRepository employeeRepository,
                              final DepartmentRepository departmentRepository) {
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
        this.departmentRepository = departmentRepository;
    }

    /**
     * Returns the authenticated user's own profile, combining user account data
     * with the linked employee record (if present).
     *
     * @return the caller's {@link ProfileResponse}
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get my profile",
               description = "Returns the authenticated user's account and employee information.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Transactional
    public ResponseEntity<ProfileResponse> getProfile() {
        User user = resolveCurrentUser();
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);

        // Safety net: if the user has ROLE_EMPLOYEE but no Employee record yet,
        // create one now so leave submission and other employee features work immediately.
        if (employee == null && user.getRoles().stream()
                .anyMatch(r -> "ROLE_EMPLOYEE".equals(r.getName()))) {
            employee = ensureEmployeeRecord(user);
        }

        return ResponseEntity.ok(buildProfileResponse(user, employee));
    }

    /**
     * Updates the authenticated user's own personal information (name, phone, address).
     *
     * <p>Updates the {@link User} entity's {@code firstName} and {@code lastName},
     * and — if the user has a linked employee record — also updates the employee's
     * {@code phone} and {@code address}.
     *
     * @param request the update payload
     * @return the updated {@link ProfileResponse}
     */
    @PutMapping(value = "/personal",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Update my personal info",
               description = "Updates the authenticated user's first name, last name, phone, and address.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Profile updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ProfileResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Transactional
    public ResponseEntity<ProfileResponse> updatePersonal(
            @Valid @RequestBody final UpdateProfileRequest request) {
        User user = resolveCurrentUser();

        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }
        User saved = userRepository.save(user);

        Employee employee = employeeRepository.findByUserId(saved.getId()).orElse(null);
        if (employee != null) {
            employee.setPhone(request.phone());
            employee.setAddress(request.address());
            employee = employeeRepository.save(employee);
        }

        return ResponseEntity.ok(buildProfileResponse(saved, employee));
    }

    // ───────────────────────── private helpers ─────────────────────────────────

    /**
     * Creates an {@link Employee} record for the given user if one does not already exist.
     * Uses the "GEN" department (or first available, or creates one) to satisfy the
     * NOT NULL constraint on {@code department_id}.
     *
     * @param user the user to link
     * @return the newly created (or existing) employee record, or {@code null} if creation failed
     */
    private Employee ensureEmployeeRecord(final User user) {
        // Double-check inside the transaction (defensive)
        java.util.Optional<Employee> existing = employeeRepository.findByUserId(user.getId());
        if (existing.isPresent()) {
            return existing.get();
        }

        Department department = departmentRepository.findByCode("GEN")
                .or(() -> departmentRepository.findAll().stream().findFirst())
                .orElseGet(() -> {
                    log.info("ProfileController.ensureEmployee: creating 'General' department for user id={}", user.getId());
                    return departmentRepository.save(
                            Department.builder().name("General").code("GEN").build());
                });

        // Generate a collision-resistant employee code
        String code = generateCode();

        Employee emp = Employee.builder()
                .user(user)
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .employeeCode(code)
                .department(department)
                .jobTitle("Employee")
                .dateOfJoining(java.time.LocalDate.now())
                .salary(java.math.BigDecimal.ZERO)
                .status(EmployeeStatus.ACTIVE)
                .build();

        Employee saved = employeeRepository.save(emp);
        log.info("ProfileController: auto-created Employee id={} code={} for user id={}",
                saved.getId(), saved.getEmployeeCode(), user.getId());
        return saved;
    }

    private String generateCode() {
        for (int i = 0; i < 20; i++) {
            String code = String.format("REG-%010d", System.currentTimeMillis() % 10_000_000_000L + i);
            if (!employeeRepository.existsByEmployeeCode(code)) {
                return code;
            }
        }
        return "REG-" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 10);
    }


    /**
     * Resolves the currently authenticated principal to a {@link User} entity.
     *
     * @return the authenticated user
     * @throws AccessDeniedException      if no authentication context is present
     * @throws ResourceNotFoundException  if the user cannot be found in the database
     */
    private User resolveCurrentUser() {
        String email = securityUtils.getCurrentUsername();
        if (email == null) {
            throw new AccessDeniedException("Not authenticated.");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    /**
     * Constructs a {@link ProfileResponse} from a {@link User} and an optional
     * {@link Employee}.
     *
     * @param user     the authenticated user entity
     * @param employee the linked employee record, or {@code null}
     * @return populated profile response
     */
    private ProfileResponse buildProfileResponse(final User user, final Employee employee) {
        String roles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.joining(", "));

        if (employee == null) {
            return new ProfileResponse(
                    user.getId(), user.getEmail(),
                    user.getFirstName(), user.getLastName(),
                    roles,
                    null, null, null, null, null, null, null, null, null, null);
        }

        return new ProfileResponse(
                user.getId(), user.getEmail(),
                user.getFirstName(), user.getLastName(),
                roles,
                employee.getId(),
                employee.getEmployeeCode(),
                employee.getDepartment() != null ? employee.getDepartment().getId() : null,
                employee.getDepartment() != null ? employee.getDepartment().getName() : null,
                employee.getJobTitle(),
                employee.getPhone(),
                employee.getAddress(),
                employee.getDateOfJoining(),
                employee.getSalary(),
                employee.getStatus());
    }
}
