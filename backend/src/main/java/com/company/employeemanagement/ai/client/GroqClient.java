package com.company.employeemanagement.ai.client;

import com.company.employeemanagement.ai.config.GroqProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Low-level client responsible for communicating with the Groq Chat Completions API.
 *
 * <p>This class is the only component in the application that knows about the
 * Groq HTTP wire format. All callers above this layer (service, controller)
 * deal exclusively with plain strings.
 *
 * <p>Error handling:
 * <ul>
 *   <li>HTTP 401 — authentication failure (bad or missing API key).</li>
 *   <li>HTTP 400 — invalid request payload sent to Groq.</li>
 *   <li>HTTP 429 / 5xx — upstream API overload or failure.</li>
 *   <li>{@link ResourceAccessException} — network timeout or connection refused.</li>
 *   <li>Empty / malformed response body — unexpected Groq response.</li>
 * </ul>
 *
 * <p>Security: the API key is never written to application logs.
 *
 * @author Employee Management Portal Team
 */
@Component
public class GroqClient {

    private static final Logger log = LoggerFactory.getLogger(GroqClient.class);

    private final RestClient    restClient;
    private final GroqProperties groqProperties;

    /**
     * Constructs the client with the pre-configured Groq {@link RestClient}.
     *
     * <p>Logs the resolved model name at startup so misconfiguration is
     * immediately visible in logs. The API key is never logged.
     *
     * @param restClient     the Groq-specific {@link RestClient} bean
     * @param groqProperties the bound Groq configuration properties
     */
    public GroqClient(@Qualifier("groqRestClient") final RestClient restClient,
                      final GroqProperties groqProperties) {
        this.restClient     = restClient;
        this.groqProperties = groqProperties;
        String model = groqProperties.getModel();
        if (model == null || model.isBlank()) {
            log.error("Groq model is not configured. Set GROQ_MODEL in .env or application.properties. "
                    + "Requests will fail until a valid model is provided.");
        } else {
            log.info("GroqClient initialised — model: {}", model);
        }
    }

    /**
     * Sends a single user message (with a system prompt) to the Groq API and
     * returns the model's reply as a plain string.
     *
     * @param systemPrompt the system instruction that shapes model behaviour
     * @param userMessage  the end-user's question or statement
     * @return the assistant's reply text
     * @throws GroqClientException if any communication or API error occurs
     */
    public String chat(final String systemPrompt, final String userMessage) {
        GroqApiTypes.ChatRequest requestBody = buildRequest(systemPrompt, userMessage);

        try {
            GroqApiTypes.ChatResponse response = restClient.post()
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        int status = res.getStatusCode().value();
                        // Read the body for diagnostics — never log the API key.
                        String errorBody = readBodySafely(res);
                        if (status == 401) {
                            log.error("Groq API authentication failed (401). Check GROQ_API_KEY configuration.");
                            throw new GroqClientException(
                                    "AI service authentication failed. Please contact the system administrator.",
                                    GroqClientException.ErrorType.AUTH_FAILURE);
                        }
                        log.warn("Groq API returned client error: HTTP {} — response body: {}", status, errorBody);
                        throw new GroqClientException(
                                "The AI service rejected the request.",
                                GroqClientException.ErrorType.INVALID_REQUEST);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        int status = res.getStatusCode().value();
                        String errorBody = readBodySafely(res);
                        log.warn("Groq API returned server error: HTTP {} — response body: {}", status, errorBody);
                        throw new GroqClientException(
                                "The AI service is temporarily unavailable. Please try again later.",
                                GroqClientException.ErrorType.API_FAILURE);
                    })
                    .body(GroqApiTypes.ChatResponse.class);

            return extractAnswer(response);

        } catch (GroqClientException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Groq API request timed out or connection refused: {}", e.getMessage());
            throw new GroqClientException(
                    "The AI assistant is temporarily unavailable. Please try again later.",
                    GroqClientException.ErrorType.TIMEOUT);
        } catch (RestClientException e) {
            log.error("Unexpected error communicating with Groq API: {}", e.getMessage());
            throw new GroqClientException(
                    "The AI assistant is temporarily unavailable. Please try again later.",
                    GroqClientException.ErrorType.API_FAILURE);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Reads the HTTP response body as a UTF-8 string for diagnostic logging.
     *
     * <p>Intentionally swallows {@link IOException} — a failure to read the error
     * body must never mask the original API error. The API key is not present in
     * Groq error response bodies and is therefore safe to log here.
     *
     * @param res the {@link ClientHttpResponse} received in the status-handler lambda
     * @return the response body as a string, or a fallback message on read failure
     */
    private String readBodySafely(final ClientHttpResponse res) {
        try {
            byte[] bytes = res.getBody().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unable to read response body: " + e.getMessage() + ">";
        }
    }

    private GroqApiTypes.ChatRequest buildRequest(final String systemPrompt, final String userMessage) {
        return new GroqApiTypes.ChatRequest(
                groqProperties.getModel(),
                List.of(
                        new GroqApiTypes.Message("system", systemPrompt),
                        new GroqApiTypes.Message("user", userMessage)
                ),
                groqProperties.getMaxTokens(),
                0.7
        );
    }

    private String extractAnswer(final GroqApiTypes.ChatResponse response) {
        if (response == null
                || response.choices() == null
                || response.choices().isEmpty()
                || response.choices().get(0).message() == null
                || response.choices().get(0).message().content() == null) {
            log.warn("Groq API returned an empty or unexpected response structure.");
            throw new GroqClientException(
                    "The AI assistant returned an unexpected response. Please try again.",
                    GroqClientException.ErrorType.API_FAILURE);
        }
        return response.choices().get(0).message().content();
    }
}
