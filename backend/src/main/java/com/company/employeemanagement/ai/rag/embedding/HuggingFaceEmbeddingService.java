package com.company.employeemanagement.ai.rag.embedding;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link EmbeddingService} implementation backed by the Hugging Face Inference API
 * (feature-extraction pipeline).
 *
 * <h2>Provider separation</h2>
 * Groq handles chat completions exclusively. This service calls the Hugging Face
 * Inference API for vector embeddings. No Groq code is touched.
 *
 * <h2>Endpoint</h2>
 * <pre>
 * POST https://router.huggingface.co/hf-inference/models/{model}/pipeline/feature-extraction
 * Authorization: Bearer {HF_TOKEN}
 * Content-Type: application/json
 * </pre>
 *
 * <h2>Request body</h2>
 * <pre>{@code
 * {
 *   "inputs": ["text one", "text two"],
 *   "options": { "wait_for_model": true }
 * }
 * }</pre>
 *
 * <h2>Response body</h2>
 * A plain 2-D JSON array — one float array per input text. No OpenAI-style wrapper:
 * <pre>{@code
 * [[0.0123, -0.0456, ...], [0.0789, 0.0012, ...]]
 * }</pre>
 *
 * <h2>Model</h2>
 * Default: {@code nomic-ai/nomic-embed-text-v1.5} (768-dimensional).<br>
 * Override via {@code RAG_EMBEDDING_MODEL} environment variable.
 *
 * <h2>Security</h2>
 * The HF token is never stored in a field and never logged.
 * It is injected into the {@code Authorization} header at construction time only.
 *
 * @author Employee Management Portal Team
 */
@Service
public class HuggingFaceEmbeddingService implements EmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(HuggingFaceEmbeddingService.class);

    /**
     * HF Inference Router base URL for the serverless inference provider.
     * Model name and pipeline type are appended per request.
     */
    static final String HF_BASE_URL =
            "https://router.huggingface.co/hf-inference/models";

    /**
     * Pipeline path segment appended after the model identifier.
     * Full path per request: {@code /{model}/pipeline/feature-extraction}
     */
    static final String PIPELINE_PATH = "/pipeline/feature-extraction";

    /**
     * ParameterizedTypeReference used to deserialise the HF response (a 2-D float array)
     * via Spring's RestClient without losing generic type information at runtime.
     */
    private static final ParameterizedTypeReference<List<List<Double>>> RESPONSE_TYPE =
            new ParameterizedTypeReference<>() { };

    /** Stored for diagnostic messages — never the token. */
    private final String configuredModel;

    private final RestClient    restClient;
    private final RagProperties ragProperties;

    /**
     * Constructs the service.
     *
     * <p>The HF token is read from the {@code HF_TOKEN} environment variable
     * (bound to {@code hf.token} in {@code application.properties}).
     * It is injected into the Authorization header immediately and then discarded;
     * it is never stored in a field and never logged.
     *
     * @param hfToken       the Hugging Face user access token; blank/null logs a warning
     * @param ragProperties RAG configuration (embedding model, enabled flag)
     */
    public HuggingFaceEmbeddingService(
            @Value("${hf.token:}") final String hfToken,
            final RagProperties ragProperties) {

        this.ragProperties   = ragProperties;
        this.configuredModel = ragProperties.getEmbedding().getModel();

        if (hfToken == null || hfToken.isBlank()) {
            log.warn("HF_TOKEN is not configured — embedding requests will fail with 401. "
                    + "Set HF_TOKEN in .env or the environment.");
        } else {
            log.info("HuggingFaceEmbeddingService initialised — model: '{}', base: {}",
                    configuredModel, HF_BASE_URL);
        }

        this.restClient = RestClient.builder()
                .baseUrl(HF_BASE_URL)
                .defaultHeader("Authorization", "Bearer " + hfToken)
                .defaultHeader("Content-Type",  "application/json")
                .build();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link #embedBatch(List)} with a single-element list.
     */
    @Override
    public float[] embed(final String text) {
        if (text == null || text.isBlank()) {
            throw new EmbeddingException("Embedding input text must not be blank.");
        }
        return embedBatch(List.of(text)).get(0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Sends a single HTTP POST to the Hugging Face feature-extraction pipeline.
     * All provider errors are translated to {@link EmbeddingException}.
     *
     * <p>The HF response is a plain 2-D JSON array; no OpenAI-style wrapper is used.
     */
    @Override
    public List<float[]> embedBatch(final List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            throw new EmbeddingException("Embedding batch must not be null or empty.");
        }

        final String model = ragProperties.getEmbedding().getModel();

        // Path: /{model}/pipeline/feature-extraction
        final String requestPath = "/" + model + PIPELINE_PATH;

        final HuggingFaceEmbeddingApiTypes.EmbeddingRequest requestBody =
                new HuggingFaceEmbeddingApiTypes.EmbeddingRequest(
                        texts,
                        HuggingFaceEmbeddingApiTypes.InferenceOptions.WAIT);

        log.debug("Requesting HF embeddings for {} text(s), model '{}'", texts.size(), model);

        try {
            final List<List<Double>> rawResponse = restClient.post()
                    .uri(requestPath)
                    .body(requestBody)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        final int    status = res.getStatusCode().value();
                        final String body   = readBodySafely(res);
                        switch (status) {
                            case 401, 403 -> {
                                // Token must never appear in logs or exceptions.
                                log.error("Hugging Face Inference API authentication failed ({})."
                                        + " Check HF_TOKEN — token is NOT logged for security.",
                                        status);
                                throw new EmbeddingException(
                                        "Embedding service authentication failed (" + status + "). "
                                        + "Please contact the system administrator.");
                            }
                            case 400 -> {
                                log.warn("Hugging Face Inference API bad request (400) — "
                                        + "model: '{}', body: {}", model, body);
                                throw new EmbeddingException(
                                        "Embedding service rejected the request (400). "
                                        + "Model: '" + model + "'. Check input or model name.");
                            }
                            case 429 -> {
                                log.warn("Hugging Face Inference API rate-limited (429). "
                                        + "body: {}", body);
                                throw new EmbeddingException(
                                        "Embedding service rate limit exceeded (429). "
                                        + "Please try again later.");
                            }
                            default -> {
                                log.warn("Hugging Face Inference API client error: HTTP {} — "
                                        + "model: '{}' — body: {}", status, model, body);
                                throw new EmbeddingException(
                                        "Embedding service rejected the request (HTTP " + status + ").");
                            }
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        final int    status = res.getStatusCode().value();
                        final String body   = readBodySafely(res);
                        log.warn("Hugging Face Inference API server error: HTTP {} — body: {}",
                                status, body);
                        throw new EmbeddingException(
                                "Embedding service unavailable (HTTP " + status + "). "
                                + "Please try again later.");
                    })
                    .body(RESPONSE_TYPE);

            return extractVectors(rawResponse, texts.size(), model);

        } catch (EmbeddingException e) {
            throw e;
        } catch (ResourceAccessException e) {
            log.warn("Hugging Face Inference API timed out or connection refused: {}",
                    e.getMessage());
            throw new EmbeddingException(
                    "Embedding service is temporarily unreachable. Please try again later.", e);
        } catch (RestClientException e) {
            log.error("Unexpected error communicating with Hugging Face Inference API: {}",
                    e.getMessage());
            throw new EmbeddingException(
                    "Embedding service communication error. Please try again later.", e);
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates the HF response (plain 2-D array) and converts to float arrays.
     *
     * <p>Embedding vectors are intentionally not logged — they are large (768 floats
     * for {@code nomic-embed-text-v1.5}) and contain no diagnostic value.
     */
    private List<float[]> extractVectors(
            final List<List<Double>> rawResponse,
            final int expectedCount,
            final String model) {

        if (rawResponse == null || rawResponse.isEmpty()) {
            throw new EmbeddingException(
                    "Hugging Face Inference API returned an empty or null response.");
        }
        if (rawResponse.size() != expectedCount) {
            log.warn("Expected {} embedding(s) from HF but got {}.",
                    expectedCount, rawResponse.size());
        }

        final List<float[]> vectors = new ArrayList<>(rawResponse.size());
        for (int i = 0; i < rawResponse.size(); i++) {
            final List<Double> row = rawResponse.get(i);
            if (row == null || row.isEmpty()) {
                throw new EmbeddingException(
                        "Hugging Face Inference API returned a null or empty vector at index " + i + ".");
            }
            final float[] vector = new float[row.size()];
            for (int j = 0; j < row.size(); j++) {
                vector[j] = row.get(j).floatValue();
            }
            vectors.add(vector);
        }
        log.debug("HF embeddings received: {} vector(s), dimension {} (model: '{}')",
                vectors.size(), vectors.get(0).length, model);
        return vectors;
    }

    private String readBodySafely(final ClientHttpResponse res) {
        try {
            final byte[] bytes = res.getBody().readAllBytes();
            return new String(bytes, StandardCharsets.UTF_8);
        } catch (IOException e) {
            return "<unable to read error body: " + e.getMessage() + ">";
        }
    }
}
