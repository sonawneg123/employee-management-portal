package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UserListResponse;

import java.util.UUID;

/**
 * Service for ADMIN user-management operations.
 *
 * <p>These operations are restricted to the ROLE_ADMIN role.
 *
 * @author Employee Management Portal Team
 */
public interface AdminService {

    /**
     * Returns a paginated list of all user accounts.
     *
     * @param page zero-based page number
     * @param size page size
     * @return paginated user list
     */
    PageResponse<UserListResponse> findAllUsers(int page, int size);

    /**
     * Assigns a single role to a user, replacing any previously assigned roles.
     *
     * @param userId   UUID of the user to update
     * @param roleName the role name to assign (e.g. {@code "ROLE_ADMIN"})
     * @return the updated user
     */
    UserListResponse updateUserRole(UUID userId, String roleName);

    /**
     * Enables or disables a user account.
     *
     * @param userId  UUID of the user to update
     * @param enabled {@code true} to enable, {@code false} to disable
     * @return the updated user
     */
    UserListResponse setUserEnabled(UUID userId, boolean enabled);
}
