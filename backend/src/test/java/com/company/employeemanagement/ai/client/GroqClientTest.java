package com.company.employeemanagement.ai.client;

import com.company.employeemanagement.ai.config.GroqProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GroqClient}.
 *
 * <p>Uses Mockito to stub the {@link RestClient} fluent chain so no real
 * network calls are made.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GroqClient")
class GroqClientTest {

    // ── Constants ─────────────────────────────────────────────────────────────

    /** The current default model — must match application.properties groq.model default. */
    private static final String DEFAULT_MODEL = "llama-3.1-8b-instant";

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Returns a {@link GroqProperties} wired with the default model. */
    private GroqProperties defaultProps() {
        GroqProperties props = new GroqProperties();
        props.setApiKey("test-key");
        props.setModel(DEFAULT_MODEL);
        props.setMaxTokens(512);
        return props;
    }

    /**
     * Builds a {@link GroqClient} whose {@link RestClient} call chain is wired
     * to return the given {@link GroqApiTypes.ChatResponse}.
     */
    @SuppressWarnings("unchecked")
    private GroqClient clientWithResponse(final GroqApiTypes.ChatResponse response) {
        RestClient.RequestBodyUriSpec  uriSpec  = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec     bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec        respSpec = mock(RestClient.ResponseSpec.class);
        RestClient                     rc       = mock(RestClient.class);

        when(rc.post()).thenReturn(uriSpec);
        doReturn(bodySpec).when(uriSpec).body(any(Object.class));
        when(bodySpec.retrieve()).thenReturn(respSpec);
        when(respSpec.onStatus(any(), any())).thenReturn(respSpec);
        when(respSpec.body(GroqApiTypes.ChatResponse.class)).thenReturn(response);

        return new GroqClient(rc, defaultProps());
    }

    /**
     * Builds a {@link GroqClient} whose {@link RestClient} call chain throws
     * the given exception at the terminal {@code body()} step.
     */
    @SuppressWarnings("unchecked")
    private GroqClient clientThatThrows(final RuntimeException ex) {
        RestClient.RequestBodyUriSpec  uriSpec  = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec     bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec        respSpec = mock(RestClient.ResponseSpec.class);
        RestClient                     rc       = mock(RestClient.class);

        when(rc.post()).thenReturn(uriSpec);
        doReturn(bodySpec).when(uriSpec).body(any(Object.class));
        when(bodySpec.retrieve()).thenReturn(respSpec);
        when(respSpec.onStatus(any(), any())).thenReturn(respSpec);
        doThrow(ex).when(respSpec).body(GroqApiTypes.ChatResponse.class);

        return new GroqClient(rc, defaultProps());
    }

    // ── ChatRequest JSON serialisation ────────────────────────────────────────

    /**
     * These tests pin the exact JSON shape sent to the Groq API.
     *
     * <p>Regression tests for two historical bugs:
     * <ol>
     *   <li>{@code maxTokens} (camelCase) serialised instead of {@code max_tokens}
     *       — fixed by placing {@code @JsonProperty("max_tokens")} on the record
     *       accessor, not the constructor parameter.</li>
     *   <li>{@code llama3-8b-8192} model name used as default — that model was
     *       retired by Groq in April 2025 and returns HTTP 400.
     *       Fixed by updating the default to {@code llama-3.1-8b-instant}.</li>
     * </ol>
     */
    @Nested
    @DisplayName("ChatRequest JSON serialisation")
    class ChatRequestSerialisationTest {

        // Plain ObjectMapper — same behaviour as Spring's RestClient uses by default.
        private final ObjectMapper plainMapper = new ObjectMapper();

        // Spring's MappingJackson2HttpMessageConverter — the exact converter
        // used by RestClient when serialising the request body over HTTP.
        private final ObjectMapper springMapper =
                new MappingJackson2HttpMessageConverter().getObjectMapper();

        @Test
        @DisplayName("plain ObjectMapper: max_tokens is snake_case (regression — camelCase caused HTTP 400)")
        void plainMapperMaxTokensSnakeCase() throws Exception {
            String json = plainMapper.writeValueAsString(chatRequest());

            assertThat(json).contains("\"max_tokens\"");
            assertThat(json).doesNotContain("\"maxTokens\"");
        }

        @Test
        @DisplayName("Spring RestClient converter: max_tokens is snake_case")
        void springConverterMaxTokensSnakeCase() throws Exception {
            String json = springMapper.writeValueAsString(chatRequest());

            assertThat(json).contains("\"max_tokens\"");
            assertThat(json).doesNotContain("\"maxTokens\"");
        }

        @Test
        @DisplayName("serialised JSON contains all four required Groq top-level fields")
        void allRequiredFieldsPresent() throws Exception {
            String json = plainMapper.writeValueAsString(chatRequest());

            assertThat(json).contains("\"model\"");
            assertThat(json).contains("\"messages\"");
            assertThat(json).contains("\"max_tokens\"");
            assertThat(json).contains("\"temperature\"");
        }

        @Test
        @DisplayName("model field contains the current default model name (regression — retired model caused HTTP 400)")
        void modelFieldMatchesCurrentDefault() throws Exception {
            String json = plainMapper.writeValueAsString(chatRequest());

            // llama-3.1-8b-instant is the active Groq model as of mid-2025.
            // If this fails, update DEFAULT_MODEL and application.properties groq.model default.
            assertThat(json).contains("\"" + DEFAULT_MODEL + "\"");
            assertThat(json).doesNotContain("\"llama3-8b-8192\""); // retired model must never appear
        }

        @Test
        @DisplayName("messages array contains system and user roles with content")
        void messagesArrayCorrect() throws Exception {
            GroqApiTypes.ChatRequest req = new GroqApiTypes.ChatRequest(
                    DEFAULT_MODEL,
                    List.of(
                            new GroqApiTypes.Message("system", "You are an HR assistant."),
                            new GroqApiTypes.Message("user", "What is leave policy?")
                    ),
                    512,
                    0.7
            );
            String json = plainMapper.writeValueAsString(req);

            assertThat(json).contains("\"role\":\"system\"");
            assertThat(json).contains("\"role\":\"user\"");
            assertThat(json).contains("\"content\":\"You are an HR assistant.\"");
            assertThat(json).contains("\"content\":\"What is leave policy?\"");
        }

        @Test
        @DisplayName("Message record: role and content field names are unmodified")
        void messageFieldNames() throws Exception {
            GroqApiTypes.Message msg = new GroqApiTypes.Message("user", "Hello?");
            String json = plainMapper.writeValueAsString(msg);

            assertThat(json).contains("\"role\"");
            assertThat(json).contains("\"content\"");
        }

        private GroqApiTypes.ChatRequest chatRequest() {
            return new GroqApiTypes.ChatRequest(
                    DEFAULT_MODEL,
                    List.of(new GroqApiTypes.Message("user", "hello")),
                    512,
                    0.7
            );
        }
    }

    // ── Successful response ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful response")
    class SuccessfulResponse {

        @Test
        @DisplayName("returns the assistant message content on success")
        void returnsAnswerOnSuccess() {
            GroqApiTypes.AssistantMessage msg    = new GroqApiTypes.AssistantMessage(
                    "assistant", "The leave policy allows 20 days per year.");
            GroqApiTypes.Choice           choice = new GroqApiTypes.Choice("stop", msg);
            GroqApiTypes.ChatResponse     resp   = new GroqApiTypes.ChatResponse(List.of(choice));

            String result = clientWithResponse(resp).chat("system prompt", "What is the leave policy?");

            assertThat(result).isEqualTo("The leave policy allows 20 days per year.");
        }
    }

    // ── Timeout / network error ───────────────────────────────────────────────

    @Nested
    @DisplayName("Timeout and network errors")
    class TimeoutErrors {

        @Test
        @DisplayName("ResourceAccessException becomes GroqClientException with TIMEOUT type")
        void resourceAccessExceptionBecomesTimeout() {
            assertThatThrownBy(() ->
                    clientThatThrows(new ResourceAccessException("connect timed out"))
                            .chat("system", "message"))
                    .isInstanceOf(GroqClientException.class)
                    .satisfies(ex -> assertThat(((GroqClientException) ex).getErrorType())
                            .isEqualTo(GroqClientException.ErrorType.TIMEOUT));
        }
    }

    // ── Model configuration guard ─────────────────────────────────────────────

    @Nested
    @DisplayName("Model configuration guard")
    class ModelConfigGuard {

        @Test
        @DisplayName("constructs successfully and logs INFO when model is set")
        void constructsSuccessfullyWithValidModel() {
            // If the constructor throws, this test fails — it must not throw.
            GroqProperties props = new GroqProperties();
            props.setApiKey("any-key");
            props.setModel(DEFAULT_MODEL);
            RestClient rc = mock(RestClient.class);

            // No exception expected
            new GroqClient(rc, props);
        }

        @Test
        @DisplayName("constructs without throwing when model is null (logs ERROR instead)")
        void doesNotThrowWhenModelIsNull() {
            // GroqClient logs an error but must not crash Spring context on startup.
            GroqProperties props = new GroqProperties();
            props.setApiKey("any-key");
            props.setModel(null); // misconfigured
            RestClient rc = mock(RestClient.class);

            // Constructor must not throw — it logs an error instead.
            new GroqClient(rc, props);
        }
    }
}
