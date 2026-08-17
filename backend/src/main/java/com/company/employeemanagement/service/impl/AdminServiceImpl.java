package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UserListResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.AdminService;
import com.company.employeemanagement.service.NotificationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Implementation of {@link AdminService} for ADMIN user-management operations.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AdminServiceImpl implements AdminService {

    private static final Logger log = LoggerFactory.getLogger(AdminServiceImpl.class);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;

    /**
     * Constructs the service with required repository dependencies.
     *
     * @param userRepository      repository for user persistence
     * @param roleRepository      repository for role lookups
     * @param employeeRepository  repository for employee lookups (to include in user list)
     * @param notificationService service for sending in-app notifications
     */
    public AdminServiceImpl(final UserRepository userRepository,
                             final RoleRepository roleRepository,
                             final EmployeeRepository employeeRepository,
                             final NotificationService notificationService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserListResponse> findAllUsers(final int page, final int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<User> users = userRepository.findAll(pageable);
        Page<UserListResponse> mapped = users.map(this::toResponse);
        return PageResponse.from(mapped);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Replaces the user's existing roles with the single specified role.
     *
     * <p>Uses a mutable {@link HashSet} for the roles collection to avoid
     * {@link UnsupportedOperationException} when JPA clears the join table rows.
     * The {@code user.getRoles()} call returns the Hibernate-managed collection
     * (which is mutable at runtime), so {@code clear()} works correctly after
     * the initial load. This comment explains the defensive re-assignment below.
     */
    @Override
    @Transactional
    public UserListResponse updateUserRole(final UUID userId, final String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));

        log.info("Admin: changing role for user id={} email={} to {}",
                user.getId(), user.getEmail(), roleName);

        // Capture the old role name before replacing
        String oldRoleName = user.getRoles().stream()
                .map(Role::getName)
                .findFirst()
                .orElse(null);

        // Use a mutable HashSet — Set.of() returns an immutable set which causes
        // UnsupportedOperationException when JPA tries to clear the join table rows.
        // For users created via DataInitializer (Set.of()), we must replace the
        // collection reference; for JPA-loaded users the collection is already mutable.
        Set<Role> mutableRoles = new HashSet<>(user.getRoles());
        mutableRoles.clear();
        mutableRoles.add(role);
        user.setRoles(mutableRoles);

        User saved = userRepository.save(user);
        log.info("Admin: role updated for user id={} email={} → {}",
                saved.getId(), saved.getEmail(), roleName);

        // Only send notification when the role actually changed
        boolean roleChanged = !roleName.equals(oldRoleName);
        if (roleChanged) {
            employeeRepository.findByUserId(saved.getId()).ifPresent(employee -> {
                String oldLabel = humanRole(oldRoleName);
                String newLabel = humanRole(roleName);
                try {
                    notificationService.createNotification(
                            employee,
                            NotificationType.ROLE_UPDATED,
                            "Role Updated",
                            "Your role has been updated from " + oldLabel + " to " + newLabel + ".",
                            null
                    );
                } catch (Exception e) {
                    log.warn("Admin: failed to send ROLE_UPDATED notification for user id={}: {}",
                            saved.getId(), e.getMessage());
                }
            });
        }

        return toResponse(saved);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public UserListResponse setUserEnabled(final UUID userId, final boolean enabled) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        user.setEnabled(enabled);
        log.info("Admin: set enabled={} for user id={} email={}", enabled, user.getId(), user.getEmail());
        User saved = userRepository.save(user);

        // Synchronise the linked Employee record status:
        // When a user is disabled → mark employee as DISABLED
        // When a user is re-enabled → restore employee to ACTIVE (only if currently DISABLED)
        employeeRepository.findByUserId(saved.getId()).ifPresent(emp -> {
            if (!enabled && emp.getStatus() == EmployeeStatus.ACTIVE) {
                emp.setStatus(EmployeeStatus.DISABLED);
                employeeRepository.save(emp);
                log.info("Admin: employee id={} status set to DISABLED", emp.getId());
            } else if (enabled && emp.getStatus() == EmployeeStatus.DISABLED) {
                emp.setStatus(EmployeeStatus.ACTIVE);
                employeeRepository.save(emp);
                log.info("Admin: employee id={} status restored to ACTIVE", emp.getId());
            }
        });

        return toResponse(saved);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    /**
     * Returns a human-readable label for a role name string.
     *
     * @param roleName e.g. "ROLE_EMPLOYEE" or null
     * @return a short label, e.g. "EMPLOYEE"
     */
    private static String humanRole(final String roleName) {
        if (roleName == null) return "Unknown";
        return roleName.startsWith("ROLE_") ? roleName.substring(5) : roleName;
    }

    /**
     * Converts a {@link User} to a {@link UserListResponse}, including any linked
     * {@link Employee} record summary.
     *
     * @param user the user entity to convert
     * @return the populated response DTO
     */
    private UserListResponse toResponse(final User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();

        // Look up the linked employee record — present for ROLE_EMPLOYEE accounts
        // (and potentially for accounts that were changed to another role but retain
        // their employee record).
        UserListResponse.EmployeeSummary employeeSummary = employeeRepository
                .findByUserId(user.getId())
                .map(this::toEmployeeSummary)
                .orElse(null);

        return new UserListResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                user.isEnabled(),
                user.isLocked(),
                user.getCreatedAt(),
                employeeSummary
        );
    }

    /**
     * Converts an {@link Employee} entity to an {@link UserListResponse.EmployeeSummary}.
     *
     * @param emp the employee entity
     * @return the summary DTO
     */
    private UserListResponse.EmployeeSummary toEmployeeSummary(final Employee emp) {
        return new UserListResponse.EmployeeSummary(
                emp.getId(),
                emp.getEmployeeCode(),
                emp.getDepartment() != null ? emp.getDepartment().getId() : null,
                emp.getDepartment() != null ? emp.getDepartment().getName() : null,
                emp.getJobTitle(),
                emp.getStatus() != null ? emp.getStatus().name() : null
        );
    }
}
