package com.company.employeemanagement.dto.request;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for an admin updating a user's role.
 *
 * @param roleName the exact role name to assign (e.g. {@code "ROLE_ADMIN"})
 *
 * @author Employee Management Portal Team
 */
public record UpdateUserRoleRequest(

        @NotBlank(message = "Role name must not be blank")
        String roleName
) {
}
