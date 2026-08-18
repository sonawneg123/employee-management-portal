package com.company.employeemanagement.ai.agent.tool;

import java.util.Set;

/**
 * Abstraction for a single tool available to the Agentic AI Copilot.
 *
 * <p>Each implementation represents a discrete capability — data retrieval or a
 * controlled action — that the agent may invoke on behalf of an authenticated
 * user. Authorization is enforced by the agent framework <em>before</em> a tool
 * is invoked; the tool itself should still validate inputs and delegate to
 * existing application services without duplicating business logic.
 *
 * <h2>Security contract</h2>
 * <ul>
 *   <li>The agent framework checks {@link #getAllowedRoles()} before calling
 *       {@link #execute}; a tool is never called for an under-privileged user.</li>
 *   <li>Mutating tools must require confirmation ({@link #isRequiresConfirmation()})
 *       and must delegate to the existing service layer so all existing
 *       business rules remain active.</li>
 *   <li>Tools must never expose passwords, tokens, salaries, or raw JWTs.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public interface AiAgentTool {

    /**
     * Stable identifier used in LLM function-call JSON.
     * Must be a valid JSON object key (lower_snake_case).
     *
     * @return the tool name
     */
    String getName();

    /**
     * Human-readable description used in the LLM system prompt to explain
     * what the tool does and when to use it.
     *
     * @return the tool description
     */
    String getDescription();

    /**
     * JSON Schema string (inline) describing the parameters this tool accepts.
     * Used when constructing the function-calling payload sent to Groq.
     *
     * @return JSON Schema for the tool parameters, or {@code "{}"} if none
     */
    String getParameterSchema();

    /**
     * Spring Security role strings that are authorised to call this tool.
     * E.g. {@code Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER")}.
     *
     * @return the non-empty set of allowed roles
     */
    Set<String> getAllowedRoles();

    /**
     * Whether this tool performs a mutating / consequential action that requires
     * explicit user confirmation before execution.
     *
     * @return {@code true} for action tools that require confirmation
     */
    boolean isRequiresConfirmation();

    /**
     * Whether this tool is read-only (no side effects).
     *
     * @return {@code true} if the tool only reads data
     */
    boolean isReadOnly();

    /**
     * Executes the tool with the given arguments JSON string.
     *
     * <p>The caller guarantees that the authenticated user's roles have already
     * been checked against {@link #getAllowedRoles()}.
     *
     * @param argumentsJson the JSON object string produced by the LLM for this tool call
     * @param context       the execution context containing authentication details
     * @return a plain-text or JSON summary of the result
     */
    String execute(String argumentsJson, AgentToolContext context);
}
