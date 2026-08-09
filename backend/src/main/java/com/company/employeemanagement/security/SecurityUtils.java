package com.company.employeemanagement.security;

import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

/**
 * Utility component that provides helpers for inspecting the currently
 * authenticated principal within the security context.
 *
 * <p>All methods are safe to call even when no authentication is present —
 * they return empty optionals or {@code false} rather than throwing.
 *
 * @author Employee Management Portal Team
 */
@Component
public class SecurityUtils {

    private final UserRepository userRepository;
    private final EmployeeRepository employeeRepository;

    /**
     * Constructs the utility with required repository dependencies.
     *
     * @param userRepository       repository for user lookups
     * @param employeeRepository   repository for employee lookups
     */
    public SecurityUtils(final UserRepository userRepository,
                         final EmployeeRepository employeeRepository) {
        this.userRepository = userRepository;
        this.employeeRepository = employeeRepository;
    }

    /**
     * Returns the email (username) of the currently authenticated user.
     *
     * @return the email address, or {@code null} if not authenticated
     */
    public String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        return auth.getName();
    }

    /**
     * Returns the UUID of the currently authenticated user by looking up their
     * email in the database.
     *
     * @return an {@link Optional} containing the user UUID, or empty if not found
     */
    public Optional<UUID> getCurrentUserId() {
        String email = getCurrentUsername();
        if (email == null) {
            return Optional.empty();
        }
        return userRepository.findByEmail(email).map(u -> u.getId());
    }

    /**
     * Returns the {@link Employee} record linked to the currently authenticated user.
     *
     * <p>Returns empty when the user has no linked employee record (e.g., admin-only accounts).
     *
     * @return an {@link Optional} containing the current user's employee record
     */
    public Optional<Employee> getCurrentEmployee() {
        Optional<UUID> userId = getCurrentUserId();
        if (userId.isEmpty()) {
            return Optional.empty();
        }
        return employeeRepository.findByUserId(userId.get());
    }

    /**
     * Returns whether the currently authenticated user holds the given role.
     *
     * @param role the Spring Security role string (e.g., {@code "ROLE_ADMIN"})
     * @return {@code true} if the current principal has the specified role
     */
    public boolean hasRole(final String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return false;
        }
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    /**
     * Returns whether the currently authenticated user is an ADMIN or HR — i.e.,
     * a privileged role that bypasses employee-level ownership restrictions.
     *
     * @return {@code true} if the current user has ADMIN, HR, or MANAGER role
     */
    public boolean isPrivileged() {
        return hasRole("ROLE_ADMIN") || hasRole("ROLE_HR") || hasRole("ROLE_MANAGER");
    }
}
