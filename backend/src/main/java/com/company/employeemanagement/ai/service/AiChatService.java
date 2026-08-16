package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.client.GroqClientException;
import com.company.employeemanagement.ai.dto.AiChatRequest;
import com.company.employeemanagement.ai.dto.AiChatResponse;
import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.service.KnowledgeRetrievalService;
import com.company.employeemanagement.ai.rag.service.RagPromptContextBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * Application service that orchestrates RAG-grounded AI chat interactions (Phase 2B).
 *
 * <h2>Phase 2B retrieval flow</h2>
 * <ol>
 *   <li>The user's message is used as the retrieval query against the Phase 2A
 *       knowledge base via {@link KnowledgeRetrievalService}.</li>
 *   <li>The retrieved chunks are formatted by {@link RagPromptContextBuilder}
 *       into a clearly delimited context block.</li>
 *   <li>The context block is combined with the Phase 1 system prompt and
 *       RAG grounding rules via {@link AiSystemPrompt#buildGroundedSystemPrompt}.</li>
 *   <li>The grounded prompt and the original user message are forwarded to Groq.</li>
 * </ol>
 *
 * <h2>No-context behavior</h2>
 * When the knowledge base returns no relevant chunks, the context block contains a
 * no-context notice that explicitly instructs the model not to invent company-specific
 * information. The Groq call proceeds normally.
 *
 * <h2>RAG retrieval failure behavior</h2>
 * If {@link KnowledgeRetrievalService#search} throws any exception, the service logs
 * a warning and falls back to the Phase 1 (un-grounded) system prompt. The Groq call
 * is never abandoned due to a retrieval failure alone.
 *
 * <h2>Backwards compatibility</h2>
 * The response contract ({@link AiChatResponse}) is unchanged. The existing
 * {@code POST /api/ai/chat} endpoint continues to work without any frontend changes.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final GroqClient groqClient;
    private final KnowledgeRetrievalService retrievalService;
    private final RagPromptContextBuilder contextBuilder;
    private final RagProperties ragProperties;

    /**
     * Constructs the service with all required collaborators.
     *
     * @param groqClient       the low-level Groq API client
     * @param retrievalService the Phase 2A knowledge retrieval interface
     * @param contextBuilder   the RAG context section builder
     * @param ragProperties    RAG configuration (enabled flag, topK)
     */
    public AiChatService(final GroqClient groqClient,
                         final KnowledgeRetrievalService retrievalService,
                         final RagPromptContextBuilder contextBuilder,
                         final RagProperties ragProperties) {
        this.groqClient       = groqClient;
        this.retrievalService = retrievalService;
        this.contextBuilder   = contextBuilder;
        this.ragProperties    = ragProperties;
    }

    /**
     * Processes a user's chat message and returns a RAG-grounded AI-generated response.
     *
     * <p>The message is used both as the retrieval query and as the Groq user turn.
     *
     * @param request the validated chat request
     * @return the AI assistant's response
     * @throws IllegalStateException    if the service is not properly configured
     * @throws IllegalArgumentException if the upstream AI service rejects the request
     */
    public AiChatResponse chat(final AiChatRequest request) {
        log.debug("AI chat request received — message length: {} chars", request.message().length());

        // ── 1. Retrieve relevant knowledge chunks ────────────────────────────
        final String systemPrompt = buildGroundedPrompt(request.message());

        // ── 2. Forward to Groq ───────────────────────────────────────────────
        try {
            log.debug("Sending grounded request to Groq");
            String answer = groqClient.chat(systemPrompt, request.message());
            log.debug("AI chat response obtained — answer length: {} chars", answer.length());
            return new AiChatResponse(answer);

        } catch (GroqClientException e) {
            switch (e.getErrorType()) {
                case AUTH_FAILURE -> {
                    log.error("Groq authentication failure — verify GROQ_API_KEY configuration.");
                    throw new IllegalStateException(
                            "The AI assistant is currently unavailable due to a configuration issue. "
                            + "Please contact the system administrator.");
                }
                case INVALID_REQUEST -> {
                    log.warn("Groq rejected the request: {}", e.getMessage());
                    throw new IllegalArgumentException(
                            "The AI assistant could not process your request. Please try rephrasing.");
                }
                default -> {
                    log.warn("Groq API error ({}): {}", e.getErrorType(), e.getMessage());
                    throw new IllegalStateException(
                            "The AI assistant is temporarily unavailable. Please try again later.");
                }
            }
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Retrieves relevant knowledge chunks for {@code userMessage} and builds the
     * grounded system prompt. Falls back to the Phase 1 prompt on any retrieval
     * failure so that the Groq call is never abandoned due to a RAG problem.
     *
     * @param userMessage the original user question (used as the retrieval query)
     * @return a complete system prompt — grounded when chunks were found, Phase 1
     *         baseline when RAG is disabled or retrieval fails
     */
    private String buildGroundedPrompt(final String userMessage) {
        if (!ragProperties.isEnabled()) {
            log.debug("RAG is disabled — using Phase 1 system prompt");
            return AiSystemPrompt.DEFAULT;
        }

        final List<KnowledgeSearchResult> chunks = retrieveChunksSafely(userMessage);

        log.debug("Building grounded AI prompt with {} chunk(s)", chunks.size());
        final String contextSection = contextBuilder.buildContextSection(chunks);
        return AiSystemPrompt.buildGroundedSystemPrompt(contextSection);
    }

    /**
     * Calls {@link KnowledgeRetrievalService#search} and returns the results.
     * On any unexpected exception it logs a warning and returns an empty list so
     * the caller can gracefully fall back to the no-context path.
     *
     * @param userMessage the query text
     * @return retrieved chunks, or an empty list on failure
     */
    private List<KnowledgeSearchResult> retrieveChunksSafely(final String userMessage) {
        try {
            log.debug("RAG retrieval started for AI chat request");
            final KnowledgeSearchRequest searchRequest =
                    new KnowledgeSearchRequest(userMessage, ragProperties.getTopK());
            final List<KnowledgeSearchResult> chunks = retrievalService.search(searchRequest);
            log.info("RAG retrieval returned {} chunk(s)", chunks.size());
            return chunks;
        } catch (Exception e) {
            log.warn("RAG retrieval failed — falling back to un-grounded chat. Cause: {}",
                    e.getMessage());
            return Collections.emptyList();
        }
    }
}
