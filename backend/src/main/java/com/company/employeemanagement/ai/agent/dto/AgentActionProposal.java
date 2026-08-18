package com.company.employeemanagement.ai.agent.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Structural representation of an action proposed by the AI agent.
 *
 * <p>Storing the proposed action as a typed record (not as arbitrary LLM text)
 * prevents prompt injection from encoding malicious instructions inside the
 * confirmation payload.
 *
 * <p>Security rules:
 * <ul>
 *   <li>The backend re-validates parameters before execution.</li>
 *   <li>The token is tied to the authenticated userId — it cannot be used by another user.</li>
 *   <li>The token expires after {@code EXPIRY_SECONDS}.</li>
 *   <li>The token is single-use — it is invalidated after confirmation or rejection.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public record AgentActionProposal(

        /** Opaque short-lived token sent to the frontend. */
        String token,

        /** Action type key: REASSIGN_TASK, APPROVE_LEAVE, REJECT_LEAVE, ADD_TASK_COMMENT, CREATE_TASK. */
        String actionType,

        /** Resource UUID relevant to the action (task ID, leave ID, etc.). */
        String resourceId,

        /** Structured parameters for the action (no LLM-generated values that could inject). */
        Map<String, String> parameters,

        /** Human-readable description shown to the user on the confirmation card. */
        String description,

        /** UUID of the user who owns this proposal. */
        String ownerUserId,

        /** Expiry timestamp. */
        Instant expiresAt

) {
    /** Default expiry window in seconds. */
    public static final int EXPIRY_SECONDS = 120;

    /**
     * Returns {@code true} if this proposal has not yet expired.
     */
    public boolean isValid() {
        return Instant.now().isBefore(expiresAt);
    }
}
