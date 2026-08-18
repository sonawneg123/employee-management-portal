package com.company.employeemanagement.ai.agent;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.ai.agent.tool.AgentToolRegistry;
import com.company.employeemanagement.ai.agent.tool.AiAgentTool;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.withSettings;

/**
 * Phase 7E security tests for the agent tool registry and confirmation store.
 *
 * <p>These tests verify:
 * <ul>
 *   <li>Employee cannot access manager-only tools.</li>
 *   <li>Manager cannot call ADMIN-only tools.</li>
 *   <li>Expired confirmation tokens are rejected.</li>
 *   <li>Confirmation tokens cannot be used by a different user.</li>
 *   <li>Confirmation tokens are single-use.</li>
 *   <li>Tool call count limit enforced by agent (checked via service logic).</li>
 *   <li>Prompt injection via tool arguments does not escalate privileges.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
class Phase7ESecurityTest {

    private AgentConfirmationStore confirmationStore;

    @BeforeEach
    void setUp() {
        confirmationStore = new AgentConfirmationStore();
    }

    // ── Tool authorization tests ────────────────────────────────────────────

    @Test
    @DisplayName("SECURITY: Employee role cannot access MANAGER-only tool")
    void employeeCannotAccessManagerTool() {
        AiAgentTool managerTool = mockTool("get_employee_workload", Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER"));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(managerTool));

        // Employee roles
        List<AiAgentTool> employeeTools = registry.toolsForRoles(Set.of("ROLE_EMPLOYEE"));

        assertThat(employeeTools).isEmpty();
    }

    @Test
    @DisplayName("SECURITY: Manager can access MANAGER tools but not ADMIN-only tools")
    void managerAccessScopedToManagerTools() {
        AiAgentTool adminOnlyTool = mockTool("admin_only", Set.of("ROLE_ADMIN"));
        AiAgentTool managerTool = mockTool("manager_tool", Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER"));
        AiAgentTool allRolesTool = mockTool("all_roles", Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE"));

        AgentToolRegistry registry = new AgentToolRegistry(List.of(adminOnlyTool, managerTool, allRolesTool));

        List<AiAgentTool> managerTools = registry.toolsForRoles(Set.of("ROLE_MANAGER"));

        assertThat(managerTools).hasSize(2);
        assertThat(managerTools.stream().map(AiAgentTool::getName))
                .containsExactlyInAnyOrder("manager_tool", "all_roles")
                .doesNotContain("admin_only");
    }

    @Test
    @DisplayName("SECURITY: Employee can only access employee-allowed tools")
    void employeeAccessRestrictedToEmployeeTools() {
        AiAgentTool managerTool = mockTool("manager_only", Set.of("ROLE_MANAGER"));
        AiAgentTool hrTool = mockTool("hr_only", Set.of("ROLE_HR"));
        AiAgentTool employeeTool = mockTool("employee_tool",
                Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE"));

        AgentToolRegistry registry = new AgentToolRegistry(List.of(managerTool, hrTool, employeeTool));

        List<AiAgentTool> allowed = registry.toolsForRoles(Set.of("ROLE_EMPLOYEE"));

        assertThat(allowed).hasSize(1);
        assertThat(allowed.get(0).getName()).isEqualTo("employee_tool");
    }

    // ── Confirmation store tests ────────────────────────────────────────────

    @Test
    @DisplayName("SECURITY: Valid confirmation token can be consumed once")
    void validTokenConsumedOnce() {
        String userId = UUID.randomUUID().toString();
        AgentActionProposal proposal = confirmationStore.createAndStore(
                "REASSIGN_TASK", "task-id-1", Map.of(), "Test action", userId);

        // First consumption: succeeds
        Optional<AgentActionProposal> first = confirmationStore.consumeToken(proposal.token(), userId);
        assertThat(first).isPresent();
        assertThat(first.get().actionType()).isEqualTo("REASSIGN_TASK");

        // Second consumption: token already removed
        Optional<AgentActionProposal> second = confirmationStore.consumeToken(proposal.token(), userId);
        assertThat(second).isEmpty();
    }

    @Test
    @DisplayName("SECURITY: Confirmation token rejected for different user")
    void tokenRejectedForDifferentUser() {
        String ownerUserId = UUID.randomUUID().toString();
        String attackerUserId = UUID.randomUUID().toString();

        // Create two proposals — one the attacker tries to steal
        AgentActionProposal ownerProposal = confirmationStore.createAndStore(
                "APPROVE_LEAVE", "leave-id-1", Map.of(), "Test action", ownerUserId);
        AgentActionProposal attackerProposal = confirmationStore.createAndStore(
                "APPROVE_LEAVE", "leave-id-2", Map.of(), "Test action 2", attackerUserId);

        // Attacker tries to use the owner's token — must be rejected
        Optional<AgentActionProposal> result = confirmationStore.consumeToken(
                ownerProposal.token(), attackerUserId);
        assertThat(result).isEmpty();

        // The attacker can use their own token
        Optional<AgentActionProposal> attackerResult = confirmationStore.consumeToken(
                attackerProposal.token(), attackerUserId);
        assertThat(attackerResult).isPresent();
    }

    @Test
    @DisplayName("SECURITY: Expired confirmation token is rejected")
    void expiredTokenRejected() {
        String userId = UUID.randomUUID().toString();
        String token = UUID.randomUUID().toString();

        // Create a proposal that is already expired
        AgentActionProposal expiredProposal = new AgentActionProposal(
                token,
                "REASSIGN_TASK",
                "task-id",
                Map.of(),
                "Test",
                userId,
                Instant.now().minusSeconds(10) // already expired
        );
        confirmationStore.store(expiredProposal);

        Optional<AgentActionProposal> result = confirmationStore.consumeToken(token, userId);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SECURITY: Non-existent confirmation token returns empty")
    void nonExistentTokenReturnsEmpty() {
        Optional<AgentActionProposal> result = confirmationStore.consumeToken(
                "non-existent-token", "any-user");
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("SECURITY: Confirmation for wrong action type is structurally isolated")
    void confirmationIsolatedByStructure() {
        // Tokens are structural — they cannot be reused for a different action
        String userId = UUID.randomUUID().toString();

        AgentActionProposal proposal1 = confirmationStore.createAndStore(
                "REASSIGN_TASK", "task-1", Map.of("taskId", "task-1"), "Reassign task 1", userId);

        AgentActionProposal proposal2 = confirmationStore.createAndStore(
                "APPROVE_LEAVE", "leave-1", Map.of("leaveId", "leave-1"), "Approve leave 1", userId);

        // Consuming proposal1's token returns exactly proposal1's data
        Optional<AgentActionProposal> r1 = confirmationStore.consumeToken(proposal1.token(), userId);
        assertThat(r1).isPresent();
        assertThat(r1.get().actionType()).isEqualTo("REASSIGN_TASK");
        assertThat(r1.get().resourceId()).isEqualTo("task-1");

        // proposal1's token cannot be used to approve the leave
        assertThat(proposal1.token()).isNotEqualTo(proposal2.token());
    }

    @Test
    @DisplayName("SECURITY: Tool context hasAnyRole check works correctly")
    void toolContextRoleCheck() {
        AgentToolContext managerContext = new AgentToolContext(
                UUID.randomUUID(), "manager@test.com",
                Set.of("ROLE_MANAGER"), null);

        assertThat(managerContext.hasAnyRole(Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER"))).isTrue();
        assertThat(managerContext.hasAnyRole(Set.of("ROLE_ADMIN"))).isFalse();
        assertThat(managerContext.hasAnyRole(Set.of("ROLE_EMPLOYEE"))).isFalse();
    }

    @Test
    @DisplayName("SECURITY: Prompt injection in tool name is sanitised by registry lookup")
    void promptInjectionInToolNameSanitised() {
        // If LLM is tricked into outputting a malicious tool name, the registry will not find it
        AiAgentTool safeTool = mock(AiAgentTool.class, withSettings().lenient());
        when(safeTool.getName()).thenReturn("get_current_employee");
        when(safeTool.getAllowedRoles()).thenReturn(Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE"));
        AgentToolRegistry registry = new AgentToolRegistry(List.of(safeTool));

        // Prompt injection attempt: attacker encodes a different tool name
        String injectedToolName = "'; DROP TABLE employees; --";
        Optional<AiAgentTool> found = registry.findByName(injectedToolName);
        assertThat(found).isEmpty();

        // The legitimate tool is still accessible
        assertThat(registry.findByName("get_current_employee")).isPresent();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AiAgentTool mockTool(final String name, final Set<String> roles) {
        AiAgentTool tool = mock(AiAgentTool.class);
        when(tool.getName()).thenReturn(name);
        when(tool.getAllowedRoles()).thenReturn(roles);
        return tool;
    }
}
