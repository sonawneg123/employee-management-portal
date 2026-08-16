package com.company.employeemanagement.ai.rag.embedding;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Jackson-mapped request/response types for the Hugging Face Inference API
 * feature-extraction (embeddings) pipeline.
 *
 * <h2>Endpoint</h2>
 * {@code POST https://router.huggingface.co/hf-inference/models/{model}/pipeline/feature-extraction}
 *
 * <h2>Request format</h2>
 * <pre>{@code
 * {
 *   "inputs": ["text one", "text two"],
 *   "options": { "wait_for_model": true }
 * }
 * }</pre>
 *
 * <h2>Response format</h2>
 * The Hugging Face Inference API returns a 2-D JSON array — one float array
 * per input text. There is no OpenAI-style {@code data[].embedding} wrapper:
 * <pre>{@code
 * [[0.123, -0.456, ...], [0.789, 0.012, ...]]
 * }</pre>
 *
 * @author Employee Management Portal Team
 */
public final class HuggingFaceEmbeddingApiTypes {

    private HuggingFaceEmbeddingApiTypes() { }

    /**
     * Request body sent to the HF feature-extraction pipeline endpoint.
     *
     * @param inputs  one or more texts to embed
     * @param options inference options (wait_for_model prevents 503 cold-start errors)
     */
    public record EmbeddingRequest(
            @JsonProperty("inputs")  List<String>      inputs,
            @JsonProperty("options") InferenceOptions  options
    ) { }

    /**
     * Inference options that control model loading behaviour on the HF side.
     *
     * @param waitForModel when {@code true} the request blocks until the model
     *                     is loaded rather than returning HTTP 503 immediately
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record InferenceOptions(
            @JsonProperty("wait_for_model") boolean waitForModel
    ) {
        /** Shared singleton — always wait for model load. */
        public static final InferenceOptions WAIT = new InferenceOptions(true);
    }

    /**
     * The HF inference response for feature-extraction is a 2-D {@code List<List<Double>>}
     * — the outer list is one element per input text, the inner list is the embedding vector.
     *
     * <p>Example for two inputs with 768-dimensional vectors:
     * <pre>{@code [[0.01, -0.02, ...], [0.03, 0.04, ...]] }</pre>
     *
     * <p>This alias exists purely to make the intent explicit in calling code.
     * Jackson deserialises the JSON array directly into a {@code List<List<Double>>}.
     */
    // No wrapper record needed — the response IS a List<List<Double>>.
    // This class provides only the type alias via the static factory below.

    /**
     * Deserialisation target: {@code List<List<Double>>}.
     * Use {@link com.fasterxml.jackson.core.type.TypeReference} in the service:
     * <pre>{@code
     * new TypeReference<List<List<Double>>>() {}
     * }</pre>
     */
    public static final class ResponseTypeHolder {
        private ResponseTypeHolder() { }
    }
}
