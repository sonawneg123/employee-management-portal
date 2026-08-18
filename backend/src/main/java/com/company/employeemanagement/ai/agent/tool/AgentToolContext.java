package com.company.employeemanagement.ai.agent.tool;

import com.company.employeemanagement.entity.Employee;

import java.util.Set;
import java.util.UUID;

/**
 * Immutable execution context passed to every {@link AiAgentTool#execute} call.
 *
 * <p>Carries the minimum identity information required for authorization and
 * audit logging without exposing raw security credentials.
 *
 * @author Employee Management Portal Team
 */
public record AgentToolContext(

        /**
         * The authenticated user's UUID (from the User entity).
         */
        UUID userId,

        /**
         * The authenticated user's email address (username).
         */
        String username,

        /**
         * The Spring Security role strings held by the authenticated user.
         * E.g. {@code ["ROLE_MANAGER"]}.
         */
        Set<String> roles,

        /**
         * The Employee record linked to the authenticated user,
         * or {@code null} if this user has no employee record (e.g. admin-only accounts).
         */
        Employee currentEmployee

) {

    /**
     * Convenience method: returns {@code true} if the user holds any of the given roles.
     *
     * @param requiredRoles the roles to test (Spring Security format: {@code "ROLE_XYZ"})
     * @return {@code true} if the intersection is non-empty
     */
    public boolean hasAnyRole(final Set<String> requiredRoles) {
        return roles.stream().anyMatch(requiredRoles::contains);
    }

    /**
     * Returns the employee's UUID, or {@code null} if no employee record is linked.
     *
     * @return the employee UUID, or {@code null}
     */
    public UUID currentEmployeeId() {
        return currentEmployee != null ? currentEmployee.getId() : null;
    }

    /**
     * Returns the primary role string for log/display purposes.
     *
     * @return the first role in the set, or {@code "UNKNOWN"}
     */
    public String primaryRole() {
        return roles.stream().findFirst().orElse("UNKNOWN");
    }
}
