package com.company.employeemanagement.ai.agent.dto;

import java.util.List;

/**
 * Response DTO returned by the agentic AI copilot endpoint.
 *
 * @param answer             the final human-readable answer
 * @param responseType       one of: INFORMATION, RECOMMENDATION, ACTION_PROPOSAL, ACTION_COMPLETED, ERROR
 * @param toolsExecuted      names of tools invoked during this turn (shown in UI progress)
 * @param confirmationToken  populated when responseType is ACTION_PROPOSAL — client must send this back
 * @param actionSummary      human-readable summary of the proposed or completed action
 *
 * @author Employee Management Portal Team
 */
public record AgentChatResponse(
        String answer,
        String responseType,
        List<String> toolsExecuted,
        String confirmationToken,
        String actionSummary
) {
    /** Convenience factory — information/recommendation (no action). */
    public static AgentChatResponse information(final String answer, final List<String> tools) {
        return new AgentChatResponse(answer, "INFORMATION", tools, null, null);
    }

    /** Convenience factory — recommendation. */
    public static AgentChatResponse recommendation(final String answer, final List<String> tools) {
        return new AgentChatResponse(answer, "RECOMMENDATION", tools, null, null);
    }

    /** Convenience factory — action proposal awaiting confirmation. */
    public static AgentChatResponse actionProposal(final String answer, final List<String> tools,
                                                     final String confirmationToken,
                                                     final String actionSummary) {
        return new AgentChatResponse(answer, "ACTION_PROPOSAL", tools, confirmationToken, actionSummary);
    }

    /** Convenience factory — confirmed action completed. */
    public static AgentChatResponse actionCompleted(final String answer, final List<String> tools,
                                                      final String actionSummary) {
        return new AgentChatResponse(answer, "ACTION_COMPLETED", tools, null, actionSummary);
    }

    /** Convenience factory — error. */
    public static AgentChatResponse error(final String answer) {
        return new AgentChatResponse(answer, "ERROR", List.of(), null, null);
    }
}
