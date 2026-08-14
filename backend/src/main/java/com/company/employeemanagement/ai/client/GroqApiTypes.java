package com.company.employeemanagement.ai.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Internal DTOs that model the Groq Chat Completions API wire format.
 *
 * <p>These types are used exclusively by {@link GroqClient} and are not
 * part of the public API surface. They are not exposed to the controller
 * or service layers beyond the extracted answer text.
 *
 * @author Employee Management Portal Team
 */
public final class GroqApiTypes {

    private GroqApiTypes() { }

    // ── Outbound request ──────────────────────────────────────────────────────

    /**
     * Top-level request body sent to the Groq chat-completions endpoint.
     *
     * <p>Jackson serialises Java record components via their accessor methods
     * (e.g. {@code maxTokens()}), so {@code @JsonProperty} must be placed on
     * the accessor — not only the constructor parameter — to control the
     * serialised field name. Groq requires snake_case {@code "max_tokens"}.
     *
     * @param model       the Groq model identifier
     * @param messages    the conversation messages
     * @param maxTokens   maximum tokens to generate
     * @param temperature sampling temperature (0.0–2.0)
     */
    public record ChatRequest(
            String model,
            List<Message> messages,
            int maxTokens,
            double temperature
    ) {
        /**
         * Accessor used by Jackson for serialisation.
         * Returns {@code max_tokens} as required by the Groq API.
         */
        @JsonProperty("max_tokens")
        @Override
        public int maxTokens() { return maxTokens; }
    }

    /**
     * A single chat message with a role and content.
     *
     * @param role    one of {@code "system"}, {@code "user"}, or {@code "assistant"}
     * @param content the message text
     */
    public record Message(String role, String content) { }

    // ── Inbound response ──────────────────────────────────────────────────────

    /**
     * Top-level response body received from the Groq chat-completions endpoint.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ChatResponse(List<Choice> choices) { }

    /**
     * A single completion choice returned by the model.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Choice(
            @JsonProperty("finish_reason") String finishReason,
            @JsonProperty("message") AssistantMessage message
    ) { }

    /**
     * The assistant message returned within a choice.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantMessage(String role, String content) { }
}
