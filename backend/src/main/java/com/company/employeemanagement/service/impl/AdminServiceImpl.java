package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UserListResponse;
import com.company.employeemanagement.entity.Role;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.RoleRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.service.AdminService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    /**
     * Constructs the service with required repository dependencies.
     *
     * @param userRepository  repository for user persistence
     * @param roleRepository  repository for role lookups
     */
    public AdminServiceImpl(final UserRepository userRepository,
                             final RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
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
     */
    @Override
    @Transactional
    public UserListResponse updateUserRole(final UUID userId, final String roleName) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", userId));
        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role", "name", roleName));
        user.setRoles(Set.of(role));
        return toResponse(userRepository.save(user));
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
        return toResponse(userRepository.save(user));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private UserListResponse toResponse(final User user) {
        List<String> roles = user.getRoles().stream()
                .map(Role::getName)
                .sorted()
                .toList();
        return new UserListResponse(
                user.getId(),
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                roles,
                user.isEnabled(),
                user.isLocked(),
                user.getCreatedAt()
        );
    }
}
