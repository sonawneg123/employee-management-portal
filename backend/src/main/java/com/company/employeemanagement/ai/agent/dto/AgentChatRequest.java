package com.company.employeemanagement.ai.agent.dto;

import java.util.List;

/**
 * Request DTO for the agentic AI copilot endpoint.
 *
 * @param message         the user's natural-language request
 * @param confirmationToken optional token to confirm a previously proposed action
 *
 * @author Employee Management Portal Team
 */
public record AgentChatRequest(
        String message,
        String confirmationToken
) {
    public AgentChatRequest(final String message) {
        this(message, null);
    }
}
