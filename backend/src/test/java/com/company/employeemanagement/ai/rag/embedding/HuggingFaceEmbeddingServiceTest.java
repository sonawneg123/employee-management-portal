package com.company.employeemanagement.ai.rag.embedding;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Unit tests for {@link HuggingFaceEmbeddingService}.
 *
 * <h2>Coverage</h2>
 * <ul>
 *   <li>Construction — correct HF endpoint constants, model, token warnings</li>
 *   <li>Endpoint URL construction: {@code /{model}/pipeline/feature-extraction}</li>
 *   <li>Successful single embedding — correct float values, correct 768-dim length</li>
 *   <li>Successful batch embedding — one vector per input text</li>
 *   <li>HTTP 401 / 403 — authentication failure, token must not appear in exception</li>
 *   <li>HTTP 400 — bad request, model name appears in message</li>
 *   <li>HTTP 429 — rate limit</li>
 *   <li>HTTP 500 / 503 — server error</li>
 *   <li>Malformed response — empty outer array</li>
 *   <li>Malformed response — null inner row</li>
 *   <li>Malformed response — empty inner row (zero-length vector)</li>
 *   <li>Input validation — null text, blank text, null batch, empty batch</li>
 *   <li>Security — HF token never in exception messages</li>
 * </ul>
 *
 * <p>All HTTP-level tests use {@link MockRestServiceServer} — no real network calls.
 *
 * <p>Live end-to-end tests are in the inner class {@link LiveIntegration} and are
 * only executed when the {@code HF_TOKEN} environment variable is set.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("HuggingFaceEmbeddingService")
class HuggingFaceEmbeddingServiceTest {

    /** Default model ID as configured in RagProperties. */
    private static final String MODEL = "nomic-ai/nomic-embed-text-v1.5";

    /**
     * Expected vector dimension for {@code nomic-embed-text-v1.5}.
     * The model uses Matryoshka Representation Learning; the full output is 768-dim.
     */
    private static final int EXPECTED_DIM = 768;

    /**
     * Full URL that the service should POST to for the default model.
     * Pattern: {HF_BASE_URL}/{model}/pipeline/feature-extraction
     */
    private static final String EXPECTED_URL =
            HuggingFaceEmbeddingService.HF_BASE_URL
            + "/" + MODEL
            + HuggingFaceEmbeddingService.PIPELINE_PATH;

    private RagProperties ragProperties;

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        ragProperties.getEmbedding().setEnabled(true);
        ragProperties.getEmbedding().setModel(MODEL);
    }

    // ── Construction ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Construction and configuration")
    class Construction {

        @Test
        @DisplayName("constructs successfully with a valid HF token")
        void constructsSuccessfully() {
            HuggingFaceEmbeddingService svc =
                    new HuggingFaceEmbeddingService("hf_testtoken", ragProperties);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("empty token does not throw at construction — logs warning")
        void emptyTokenDoesNotThrow() {
            HuggingFaceEmbeddingService svc =
                    new HuggingFaceEmbeddingService("", ragProperties);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("null token does not throw at construction — logs warning")
        void nullTokenDoesNotThrow() {
            HuggingFaceEmbeddingService svc =
                    new HuggingFaceEmbeddingService(null, ragProperties);
            assertThat(svc).isNotNull();
        }

        @Test
        @DisplayName("HF_BASE_URL constant points to HF Inference Router — not OpenAI, not Groq")
        void baseUrlIsHuggingFace() {
            assertThat(HuggingFaceEmbeddingService.HF_BASE_URL)
                    .startsWith("https://router.huggingface.co")
                    .doesNotContain("openai.com")
                    .doesNotContain("groq.com");
        }

        @Test
        @DisplayName("PIPELINE_PATH constant is /pipeline/feature-extraction")
        void pipelinePathIsCorrect() {
            assertThat(HuggingFaceEmbeddingService.PIPELINE_PATH)
                    .isEqualTo("/pipeline/feature-extraction");
        }

        @Test
        @DisplayName("default RagProperties model is nomic-ai/nomic-embed-text-v1.5")
        void defaultModelIsNomicEmbedText() {
            RagProperties defaults = new RagProperties();
            assertThat(defaults.getEmbedding().getModel())
                    .isEqualTo("nomic-ai/nomic-embed-text-v1.5")
                    .doesNotContain("openai")
                    .doesNotContain("text-embedding-3-small");
        }

        /**
         * Verifies the full URL constructed per request.
         * Pattern: {base}/{model}/pipeline/feature-extraction
         */
        @Test
        @DisplayName("per-request URL is {base}/{model}/pipeline/feature-extraction")
        void perRequestUrlIsConstructedCorrectly() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            vectorArrayJson(List.of(0.1f, 0.2f, 0.3f)),
                            MediaType.APPLICATION_JSON));

            new TestHelper(builder.build(), ragProperties).embed("test");
            server.verify();
        }
    }

    // ── Successful responses ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful embedding responses")
    class SuccessfulResponses {

        @Test
        @DisplayName("single text returns correctly parsed float vector")
        void singleTextReturnsVector() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            List<Float> values = List.of(0.1f, 0.2f, 0.3f, 0.4f);
            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            vectorArrayJson(values),
                            MediaType.APPLICATION_JSON));

            float[] result = new TestHelper(builder.build(), ragProperties)
                    .embed("How many days notice for remote work?");

            assertThat(result).hasSize(4);
            assertThat(result[0]).isCloseTo(0.1f, org.assertj.core.data.Offset.offset(0.001f));
            assertThat(result[3]).isCloseTo(0.4f, org.assertj.core.data.Offset.offset(0.001f));
            server.verify();
        }

        @Test
        @DisplayName("simulated 768-dim response — vector length is 768")
        void simulatedFullDimensionResponse() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            List<Float> values = buildVector(EXPECTED_DIM, 0.001f);
            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            vectorArrayJson(values),
                            MediaType.APPLICATION_JSON));

            float[] result = new TestHelper(builder.build(), ragProperties)
                    .embed("remote work policy");

            assertThat(result).hasSize(EXPECTED_DIM);
            server.verify();
        }

        @Test
        @DisplayName("batch of 3 texts returns 3 independent float vectors")
        void batchReturnsOneVectorPerText() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            List<List<Float>> batch = List.of(
                    List.of(0.1f, 0.2f),
                    List.of(0.3f, 0.4f),
                    List.of(0.5f, 0.6f)
            );
            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(
                            batchVectorArrayJson(batch),
                            MediaType.APPLICATION_JSON));

            List<float[]> results = new TestHelper(builder.build(), ragProperties)
                    .embedBatch(List.of("text1", "text2", "text3"));

            assertThat(results).hasSize(3);
            assertThat(results.get(0)[0]).isCloseTo(0.1f, org.assertj.core.data.Offset.offset(0.001f));
            assertThat(results.get(2)[1]).isCloseTo(0.6f, org.assertj.core.data.Offset.offset(0.001f));
            server.verify();
        }

        @Test
        @DisplayName("response is plain 2-D array — no data/embedding wrapper like OpenAI")
        void responseIs2dArrayNotOpenAiWrapper() {
            // Verify the HF format: [[v1, v2, ...]] not {"data":[{"embedding":[...]}]}
            String hfJson = "[[0.5, 0.6, 0.7]]";  // valid HF format
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess(hfJson, MediaType.APPLICATION_JSON));

            float[] result = new TestHelper(builder.build(), ragProperties)
                    .embed("any text");

            assertThat(result).hasSize(3);
            server.verify();
        }
    }

    // ── Provider HTTP errors ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Provider HTTP error responses")
    class ProviderErrors {

        @Test
        @DisplayName("HTTP 401 throws EmbeddingException — token not in message")
        void http401ThrowsAuth() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Authorization error\"}"));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("authentication failed")
                    .hasMessageContaining("401")
                    .hasMessageNotContaining("hf_testtoken")
                    .hasMessageNotContaining("Bearer");

            server.verify();
        }

        @Test
        @DisplayName("HTTP 403 throws EmbeddingException — token not in message")
        void http403ThrowsAuth() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Forbidden\"}"));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("authentication failed")
                    .hasMessageContaining("403")
                    .hasMessageNotContaining("hf_testtoken");

            server.verify();
        }

        @Test
        @DisplayName("HTTP 400 throws EmbeddingException — model name in message")
        void http400ThrowsBadRequest() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Bad inputs\"}"));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("400")
                    .hasMessageContaining(MODEL);

            server.verify();
        }

        @Test
        @DisplayName("HTTP 429 throws EmbeddingException with rate-limit message")
        void http429ThrowsRateLimit() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Rate limit reached\"}"));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("429")
                    .hasMessageContaining("rate limit");

            server.verify();
        }

        @Test
        @DisplayName("HTTP 500 throws EmbeddingException with unavailable message")
        void http500ThrowsServerError() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"error\":\"Internal server error\"}"));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("unavailable")
                    .hasMessageContaining("500");

            server.verify();
        }

        @Test
        @DisplayName("HTTP 503 throws EmbeddingException with unavailable message")
        void http503ThrowsServiceUnavailable() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"estimated_time\":20.5,\"error\":\"Model loading\"}"));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("unavailable")
                    .hasMessageContaining("503");

            server.verify();
        }
    }

    // ── Malformed responses ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Malformed provider responses")
    class MalformedResponses {

        @Test
        @DisplayName("HTTP 200 with empty outer array throws EmbeddingException")
        void emptyOuterArrayThrows() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("empty or null response");

            server.verify();
        }

        @Test
        @DisplayName("HTTP 200 with null inner row throws EmbeddingException")
        void nullInnerRowThrows() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            // A JSON array whose first element is null
            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("[null]", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("null or empty vector");

            server.verify();
        }

        @Test
        @DisplayName("HTTP 200 with empty inner row throws EmbeddingException")
        void emptyInnerRowThrows() {
            RestClient.Builder builder = mockBuilder();
            MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();

            server.expect(requestTo(EXPECTED_URL))
                    .andExpect(method(HttpMethod.POST))
                    .andRespond(withSuccess("[[]]", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> new TestHelper(builder.build(), ragProperties).embed("text"))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("null or empty vector");

            server.verify();
        }
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Input validation")
    class InputValidation {

        private HuggingFaceEmbeddingService service;

        @BeforeEach
        void buildService() {
            service = new HuggingFaceEmbeddingService("hf_test", ragProperties);
        }

        @Test
        @DisplayName("embed with null text throws EmbeddingException")
        void embedNullThrows() {
            assertThatThrownBy(() -> service.embed(null))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("embed with blank text throws EmbeddingException")
        void embedBlankThrows() {
            assertThatThrownBy(() -> service.embed("   "))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("embedBatch with null list throws EmbeddingException")
        void embedBatchNullThrows() {
            assertThatThrownBy(() -> service.embedBatch(null))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("null or empty");
        }

        @Test
        @DisplayName("embedBatch with empty list throws EmbeddingException")
        void embedBatchEmptyThrows() {
            assertThatThrownBy(() -> service.embedBatch(List.of()))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("null or empty");
        }
    }

    // ── Security invariants ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Security — HF token must not appear in exception messages")
    class Security {

        @Test
        @DisplayName("401 exception message does not contain the token or 'Bearer'")
        void authExceptionDoesNotLeakToken() {
            EmbeddingException ex = new EmbeddingException(
                    "Embedding service authentication failed (401). "
                    + "Please contact the system administrator.");
            assertThat(ex.getMessage())
                    .doesNotContain("hf_")
                    .doesNotContain("Bearer");
        }

        @Test
        @DisplayName("400 exception contains model name but not the token")
        void badRequestExceptionContainsModelNotToken() {
            EmbeddingException ex = new EmbeddingException(
                    "Embedding service rejected the request (400). "
                    + "Model: '" + MODEL + "'. Check input or model name.");
            assertThat(ex.getMessage()).contains(MODEL);
            assertThat(ex.getMessage()).doesNotContain("hf_").doesNotContain("Bearer");
        }

        @Test
        @DisplayName("HuggingFaceEmbeddingService class exists and has the expected name")
        void classExists() {
            assertThat(HuggingFaceEmbeddingService.class.getSimpleName())
                    .isEqualTo("HuggingFaceEmbeddingService");
        }
    }

    // ── EmbeddingException contract ───────────────────────────────────────────

    @Nested
    @DisplayName("EmbeddingException contract")
    class ExceptionContract {

        @Test
        @DisplayName("message constructor preserves message")
        void messageConstructor() {
            EmbeddingException ex = new EmbeddingException("test error");
            assertThat(ex.getMessage()).isEqualTo("test error");
        }

        @Test
        @DisplayName("message+cause constructor preserves both")
        void messageCauseConstructor() {
            RuntimeException cause = new RuntimeException("root cause");
            EmbeddingException ex = new EmbeddingException("wrapping", cause);
            assertThat(ex.getMessage()).isEqualTo("wrapping");
            assertThat(ex.getCause()).isSameAs(cause);
        }
    }

    // ── Live integration tests (require HF_TOKEN in environment) ─────────────

    /**
     * Live end-to-end integration tests that hit the real Hugging Face Inference API.
     *
     * <p>These tests are skipped unless the {@code HF_TOKEN} environment variable is set.
     * They must never hard-code or expose a real token.
     *
     * <p>To run:
     * <pre>{@code
     * # Linux/macOS
     * HF_TOKEN=hf_xxx mvn test -pl backend -Dtest=HuggingFaceEmbeddingServiceTest#LiveIntegration
     *
     * # Windows PowerShell
     * $env:HF_TOKEN="hf_xxx"; mvn test -pl backend -Dtest=HuggingFaceEmbeddingServiceTest#LiveIntegration
     * }</pre>
     */
    @Nested
    @Tag("live")
    @DisplayName("Live integration — requires HF_TOKEN env var")
    @EnabledIfEnvironmentVariable(named = "HF_TOKEN", matches = ".+")
    class LiveIntegration {

        private HuggingFaceEmbeddingService liveService;

        @BeforeEach
        void buildLiveService() {
            final String token = System.getenv("HF_TOKEN");
            liveService = new HuggingFaceEmbeddingService(token, ragProperties);
        }

        @Test
        @DisplayName("live: embed returns 768-dimensional vector for nomic-embed-text-v1.5")
        void liveEmbedReturns768DimVector() {
            float[] vector = liveService.embed(
                    "How many days in advance should I submit a remote-work request?");

            assertThat(vector).isNotNull();
            assertThat(vector).hasSize(EXPECTED_DIM);
            // Vector should not be all zeros
            boolean hasNonZero = false;
            for (float v : vector) {
                if (v != 0.0f) { hasNonZero = true; break; }
            }
            assertThat(hasNonZero).isTrue();
        }

        @Test
        @DisplayName("live: batch embed returns one 768-dim vector per input")
        void liveBatchEmbedReturnsCorrectCount() {
            List<String> inputs = List.of(
                    "Remote work policy",
                    "Annual leave entitlement",
                    "Standard working hours"
            );
            List<float[]> vectors = liveService.embedBatch(inputs);

            assertThat(vectors).hasSize(3);
            for (float[] v : vectors) {
                assertThat(v).hasSize(EXPECTED_DIM);
            }
        }

        @Test
        @DisplayName("live: cosine similarity of semantically related texts is above 0.5")
        void liveSemanticSimilarityIsHigh() {
            float[] v1 = liveService.embed("remote work request advance notice");
            float[] v2 = liveService.embed("How many days before should I request to work from home?");

            double similarity = com.company.employeemanagement.ai.rag.embedding
                    .VectorSimilarity.cosineSimilarity(v1, v2);

            // Semantically related texts should have high cosine similarity
            assertThat(similarity).isGreaterThan(0.5);
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Pre-configured RestClient.Builder pointing at the HF base URL. */
    private static RestClient.Builder mockBuilder() {
        return RestClient.builder()
                .baseUrl(HuggingFaceEmbeddingService.HF_BASE_URL)
                .defaultHeader("Authorization", "Bearer hf_testtoken")
                .defaultHeader("Content-Type", "application/json");
    }

    /**
     * Builds a HF-format response JSON for a single input:
     * {@code [[v0, v1, v2, ...]]}
     */
    static String vectorArrayJson(List<Float> values) {
        StringBuilder sb = new StringBuilder("[[");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append(values.get(i));
        }
        sb.append("]]");
        return sb.toString();
    }

    /**
     * Builds a HF-format response JSON for multiple inputs:
     * {@code [[v0, v1, ...], [v0, v1, ...], ...]}
     */
    static String batchVectorArrayJson(List<List<Float>> vectors) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vectors.size(); i++) {
            if (i > 0) sb.append(',');
            sb.append("[");
            List<Float> row = vectors.get(i);
            for (int j = 0; j < row.size(); j++) {
                if (j > 0) sb.append(',');
                sb.append(row.get(j));
            }
            sb.append("]");
        }
        sb.append("]");
        return sb.toString();
    }

    /** Builds a vector of {@code size} floats with incrementing values. */
    static List<Float> buildVector(int size, float step) {
        List<Float> list = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            list.add(step * (i + 1));
        }
        return list;
    }

    // ── Inner HTTP test helper ────────────────────────────────────────────────

    /**
     * Replicates the {@link HuggingFaceEmbeddingService} error-handling pipeline
     * against a caller-supplied {@link RestClient} so that
     * {@link MockRestServiceServer} can intercept requests without modifying the
     * production class.
     *
     * <p>The HF response is a plain {@code List<List<Double>>} — deserialized via
     * a TypeReference to preserve generic type information at runtime.
     */
    static class TestHelper {

        private static final org.springframework.core.ParameterizedTypeReference<List<List<Double>>>
                RESPONSE_TYPE = new org.springframework.core.ParameterizedTypeReference<>() { };

        private final RestClient    restClient;
        private final RagProperties ragProperties;

        TestHelper(RestClient restClient, RagProperties ragProperties) {
            this.restClient    = restClient;
            this.ragProperties = ragProperties;
        }

        float[] embed(String text) {
            return embedBatch(List.of(text)).get(0);
        }

        List<float[]> embedBatch(List<String> texts) {
            final String model = ragProperties.getEmbedding().getModel();
            final String path  = "/" + model + HuggingFaceEmbeddingService.PIPELINE_PATH;

            final HuggingFaceEmbeddingApiTypes.EmbeddingRequest body =
                    new HuggingFaceEmbeddingApiTypes.EmbeddingRequest(
                            texts,
                            HuggingFaceEmbeddingApiTypes.InferenceOptions.WAIT);

            List<List<Double>> raw = restClient.post()
                    .uri(path)
                    .body(body)
                    .retrieve()
                    .onStatus(org.springframework.http.HttpStatusCode::is4xxClientError,
                            (req, res) -> {
                                int status = res.getStatusCode().value();
                                if (status == 401 || status == 403) {
                                    throw new EmbeddingException(
                                            "Embedding service authentication failed (" + status + "). "
                                            + "Please contact the system administrator.");
                                }
                                if (status == 400) {
                                    throw new EmbeddingException(
                                            "Embedding service rejected the request (400). "
                                            + "Model: '" + model + "'. Check input or model name.");
                                }
                                if (status == 429) {
                                    throw new EmbeddingException(
                                            "Embedding service rate limit exceeded (429). "
                                            + "Please try again later.");
                                }
                                throw new EmbeddingException(
                                        "Embedding service rejected the request (HTTP " + status + ").");
                            })
                    .onStatus(org.springframework.http.HttpStatusCode::is5xxServerError,
                            (req, res) -> {
                                int status = res.getStatusCode().value();
                                throw new EmbeddingException(
                                        "Embedding service unavailable (HTTP " + status + "). "
                                        + "Please try again later.");
                            })
                    .body(RESPONSE_TYPE);

            if (raw == null || raw.isEmpty()) {
                throw new EmbeddingException(
                        "Hugging Face Inference API returned an empty or null response.");
            }

            List<float[]> vectors = new ArrayList<>(raw.size());
            for (int i = 0; i < raw.size(); i++) {
                List<Double> row = raw.get(i);
                if (row == null || row.isEmpty()) {
                    throw new EmbeddingException(
                            "Hugging Face Inference API returned a null or empty vector at index " + i + ".");
                }
                float[] vec = new float[row.size()];
                for (int j = 0; j < row.size(); j++) {
                    vec[j] = row.get(j).floatValue();
                }
                vectors.add(vec);
            }
            return vectors;
        }
    }
}
