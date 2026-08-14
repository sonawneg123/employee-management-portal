package com.company.employeemanagement.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Typed configuration properties for the Groq API integration.
 *
 * <p>Values are bound from {@code application.properties} entries prefixed
 * with {@code groq}, which in turn read from environment variables:
 * <ul>
 *   <li>{@code GROQ_API_KEY} — secret API key, never logged or exposed.</li>
 *   <li>{@code GROQ_MODEL}   — model identifier, e.g. {@code llama3-8b-8192}.</li>
 * </ul>
 *
 * <p>Registered as a bean via {@code @EnableConfigurationProperties} in
 * {@link GroqConfig}.
 *
 * @author Employee Management Portal Team
 */
@ConfigurationProperties(prefix = "groq")
public class GroqProperties {

    /** Groq API key. Injected from the {@code GROQ_API_KEY} environment variable. */
    private String apiKey;

    /** Groq model identifier. Injected from the {@code GROQ_MODEL} environment variable. */
    private String model;

    /** Base URL for the Groq chat-completions API endpoint. */
    private String baseUrl = "https://api.groq.com/openai/v1/chat/completions";

    /** Request timeout in milliseconds. */
    private int timeoutMs = 30_000;

    /** Maximum number of tokens the model may generate in a single response. */
    private int maxTokens = 1024;

    public String getApiKey() { return apiKey; }
    public void setApiKey(final String apiKey) { this.apiKey = apiKey; }

    public String getModel() { return model; }
    public void setModel(final String model) { this.model = model; }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(final String baseUrl) { this.baseUrl = baseUrl; }

    public int getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(final int timeoutMs) { this.timeoutMs = timeoutMs; }

    public int getMaxTokens() { return maxTokens; }
    public void setMaxTokens(final int maxTokens) { this.maxTokens = maxTokens; }
}
