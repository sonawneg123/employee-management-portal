package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.UpdateProfileRequest;
import com.company.employeemanagement.dto.response.ProfileResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
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

    private final SecurityUtils securityUtils;
    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Constructs the controller with required dependencies.
     *
     * @param securityUtils      helper for current-principal inspection
     * @param userRepository     repository for user account lookups and saves
     * @param employeeRepository repository for employee record lookups and saves
     */
    public ProfileController(final SecurityUtils securityUtils,
                              final UserRepository userRepository,
                              final EmployeeRepository employeeRepository) {
        this.securityUtils = securityUtils;
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
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
    @Transactional(readOnly = true)
    public ResponseEntity<ProfileResponse> getProfile() {
        User user = resolveCurrentUser();
        Employee employee = employeeRepository.findByUserId(user.getId()).orElse(null);
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

        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
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
