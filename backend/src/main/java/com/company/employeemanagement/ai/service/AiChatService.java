package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.client.GroqClientException;
import com.company.employeemanagement.ai.dto.AiChatRequest;
import com.company.employeemanagement.ai.dto.AiChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service that orchestrates Phase-1 AI chat interactions.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Validates that the Groq integration is configured before forwarding
 *       requests.</li>
 *   <li>Injects the system prompt from {@link AiSystemPrompt}.</li>
 *   <li>Delegates to {@link GroqClient} for the actual API call.</li>
 *   <li>Translates {@link GroqClientException} into friendly, loggable
 *       {@link RuntimeException} messages suitable for the global exception
 *       handler.</li>
 * </ul>
 *
 * <p>This service does NOT access the database and does NOT implement RAG.
 * It is the correct extension point for Phase 2 (RAG/embeddings) later.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AiChatService {

    private static final Logger log = LoggerFactory.getLogger(AiChatService.class);

    private final GroqClient groqClient;

    /**
     * Constructs the service with the Groq API client.
     *
     * @param groqClient the low-level Groq API client
     */
    public AiChatService(final GroqClient groqClient) {
        this.groqClient = groqClient;
    }

    /**
     * Processes a user's chat message and returns the AI-generated response.
     *
     * <p>The message is forwarded verbatim to Groq — no additional
     * context or database enrichment occurs in Phase 1.
     *
     * @param request the validated chat request
     * @return the AI assistant's response
     * @throws IllegalStateException    if the service is not properly configured
     * @throws IllegalArgumentException if the upstream AI service rejects the request
     */
    public AiChatResponse chat(final AiChatRequest request) {
        log.debug("AI chat request received — message length: {} chars", request.message().length());

        try {
            String answer = groqClient.chat(AiSystemPrompt.DEFAULT, request.message());
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
}
