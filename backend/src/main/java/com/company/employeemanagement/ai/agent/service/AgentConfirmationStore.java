package com.company.employeemanagement.ai.agent.service;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for pending {@link AgentActionProposal} confirmation tokens.
 *
 * <p>Security properties:
 * <ul>
 *   <li>Tokens are random UUIDs — not predictable.</li>
 *   <li>Each token is single-use: it is removed on first access.</li>
 *   <li>Tokens expire after {@link AgentActionProposal#EXPIRY_SECONDS} seconds.</li>
 *   <li>Tokens are scoped to the owner's userId — another user cannot confirm.</li>
 *   <li>An expired confirmation is rejected even if the token exists.</li>
 *   <li>A stale-sweep runs every 60 seconds to prevent memory growth.</li>
 * </ul>
 *
 * <p>This component uses an in-memory ConcurrentHashMap, which is suitable for
 * single-node deployments. For clustered deployments, replace with a
 * distributed cache (Redis, etc.).
 *
 * @author Employee Management Portal Team
 */
@Component
public class AgentConfirmationStore {

    private static final Logger log = LoggerFactory.getLogger(AgentConfirmationStore.class);

    private final Map<String, AgentActionProposal> store = new ConcurrentHashMap<>();

    /**
     * Stores a new action proposal and returns its token.
     *
     * @param proposal the action proposal to store
     * @return the opaque confirmation token
     */
    public String store(final AgentActionProposal proposal) {
        store.put(proposal.token(), proposal);
        log.debug("Stored confirmation token={} action={} owner={}",
                proposal.token(), proposal.actionType(), proposal.ownerUserId());
        return proposal.token();
    }

    /**
     * Creates, stores, and returns a new proposal with a random token.
     *
     * @param actionType   action type key
     * @param resourceId   affected resource UUID string
     * @param parameters   structured parameters for execution
     * @param description  human-readable description shown to user
     * @param ownerUserId  userId of the requesting user
     * @return the newly created and stored proposal
     */
    public AgentActionProposal createAndStore(final String actionType,
                                               final String resourceId,
                                               final Map<String, String> parameters,
                                               final String description,
                                               final String ownerUserId) {
        String token = UUID.randomUUID().toString();
        AgentActionProposal proposal = new AgentActionProposal(
                token,
                actionType,
                resourceId,
                parameters,
                description,
                ownerUserId,
                Instant.now().plusSeconds(AgentActionProposal.EXPIRY_SECONDS)
        );
        store(proposal);
        return proposal;
    }

    /**
     * Retrieves and <em>removes</em> the proposal for the given token.
     * Returns empty if the token does not exist, has expired, or does not
     * belong to the given user.
     *
     * @param token       the confirmation token sent by the client
     * @param userId      the UUID of the currently authenticated user
     * @return an {@link Optional} containing the proposal, or empty
     */
    public Optional<AgentActionProposal> consumeToken(final String token, final String userId) {
        AgentActionProposal proposal = store.remove(token);
        if (proposal == null) {
            log.warn("Confirmation token not found or already used: {}", token);
            return Optional.empty();
        }
        if (!proposal.ownerUserId().equals(userId)) {
            log.warn("Confirmation token owner mismatch — token={} expectedOwner={} actualUser={}",
                    token, proposal.ownerUserId(), userId);
            return Optional.empty();
        }
        if (!proposal.isValid()) {
            log.warn("Confirmation token expired — token={} expiredAt={}", token, proposal.expiresAt());
            return Optional.empty();
        }
        log.debug("Consumed confirmation token={} action={}", token, proposal.actionType());
        return Optional.of(proposal);
    }

    /**
     * Removes all expired proposals from the store.
     * Runs every 60 seconds.
     */
    @Scheduled(fixedDelay = 60_000)
    public void sweepExpired() {
        int removed = 0;
        for (Map.Entry<String, AgentActionProposal> entry : store.entrySet()) {
            if (!entry.getValue().isValid()) {
                store.remove(entry.getKey());
                removed++;
            }
        }
        if (removed > 0) {
            log.debug("AgentConfirmationStore: swept {} expired token(s)", removed);
        }
    }
}
