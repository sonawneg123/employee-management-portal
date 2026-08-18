package com.company.employeemanagement.ai.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistent audit record for every AI agent execution.
 *
 * <p>Sensitive data is NEVER stored in this record:
 * no passwords, JWTs, API keys, or raw authentication headers.
 * Salary and other sensitive employee fields are excluded from tool results.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "ai_agent_audit_logs")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "id", nullable = false, updatable = false, length = 36)
    private UUID id;

    /** UUID of the authenticated user who made the request. */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "user_id", nullable = false, length = 36)
    private UUID userId;

    /** Email of the authenticated user (for readability in audit queries). */
    @Column(name = "username", nullable = false, length = 255)
    private String username;

    /** Primary role held by the user (e.g. ROLE_MANAGER). */
    @Column(name = "user_role", nullable = false, length = 50)
    private String userRole;

    /** The original user request (truncated to 2000 chars). */
    @Column(name = "user_request", nullable = false, length = 2000)
    private String userRequest;

    /** Comma-separated list of tool names invoked, in order. */
    @Column(name = "tools_invoked", length = 1000)
    private String toolsInvoked;

    /** Sanitised summary of tool call arguments (no sensitive data). */
    @Column(name = "tool_args_summary", length = 2000)
    private String toolArgsSummary;

    /** Brief summary of tool results. */
    @Column(name = "tool_results_summary", length = 4000)
    private String toolResultsSummary;

    /** Type of agent response: INFORMATION, RECOMMENDATION, ACTION_PROPOSAL, ACTION_COMPLETED, ERROR. */
    @Column(name = "response_type", length = 50)
    private String responseType;

    /** Action type proposed (e.g. REASSIGN_TASK), or null if not an action turn. */
    @Column(name = "proposed_action_type", length = 100)
    private String proposedActionType;

    /** Resource ID targeted by the action, or null. */
    @Column(name = "proposed_action_resource_id", length = 100)
    private String proposedActionResourceId;

    /** Whether the action was confirmed by the user. */
    @Column(name = "action_confirmed")
    private Boolean actionConfirmed;

    /** Whether the action completed successfully. */
    @Column(name = "action_success")
    private Boolean actionSuccess;

    /** Human-readable reason for failure, or null on success. */
    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    /** Number of tool calls made in this execution. */
    @Column(name = "tool_call_count")
    private int toolCallCount;

    /** Total execution duration in milliseconds. */
    @Column(name = "execution_ms")
    private long executionMs;

    /** Timestamp of this audit record. */
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
