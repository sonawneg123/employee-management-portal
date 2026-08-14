package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.client.GroqClientException;
import com.company.employeemanagement.ai.dto.AiChatRequest;
import com.company.employeemanagement.ai.dto.AiChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiChatService}.
 *
 * <p>The {@link GroqClient} is mocked so no real network calls are made.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatService")
class AiChatServiceTest {

    @Mock
    private GroqClient groqClient;

    @InjectMocks
    private AiChatService aiChatService;

    private static final String USER_MESSAGE = "What is the leave policy?";
    private static final String AI_ANSWER    = "The annual leave entitlement is 20 days.";

    // ── Successful response ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful Groq response")
    class SuccessfulResponse {

        @Test
        @DisplayName("returns AiChatResponse containing the Groq answer")
        void returnsAnswerFromGroq() {
            when(groqClient.chat(anyString(), anyString())).thenReturn(AI_ANSWER);

            AiChatResponse response = aiChatService.chat(new AiChatRequest(USER_MESSAGE));

            assertThat(response).isNotNull();
            assertThat(response.answer()).isEqualTo(AI_ANSWER);
        }

        @Test
        @DisplayName("passes the system prompt and user message to GroqClient")
        void passesSystemPromptAndMessage() {
            when(groqClient.chat(anyString(), anyString())).thenReturn(AI_ANSWER);

            aiChatService.chat(new AiChatRequest(USER_MESSAGE));

            verify(groqClient).chat(AiSystemPrompt.DEFAULT, USER_MESSAGE);
        }
    }

    // ── Groq authentication failure ───────────────────────────────────────────

    @Nested
    @DisplayName("Groq authentication failure")
    class AuthFailure {

        @Test
        @DisplayName("throws IllegalStateException for AUTH_FAILURE")
        void authFailureThrowsIllegalState() {
            when(groqClient.chat(anyString(), anyString()))
                    .thenThrow(new GroqClientException(
                            "auth failed", GroqClientException.ErrorType.AUTH_FAILURE));

            assertThatThrownBy(() -> aiChatService.chat(new AiChatRequest(USER_MESSAGE)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("configuration issue");
        }
    }

    // ── Groq API failure ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Groq API failure")
    class ApiFailure {

        @Test
        @DisplayName("throws IllegalStateException for API_FAILURE")
        void apiFailureThrowsIllegalState() {
            when(groqClient.chat(anyString(), anyString()))
                    .thenThrow(new GroqClientException(
                            "server error", GroqClientException.ErrorType.API_FAILURE));

            assertThatThrownBy(() -> aiChatService.chat(new AiChatRequest(USER_MESSAGE)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("throws IllegalStateException for TIMEOUT")
        void timeoutThrowsIllegalState() {
            when(groqClient.chat(anyString(), anyString()))
                    .thenThrow(new GroqClientException(
                            "timeout", GroqClientException.ErrorType.TIMEOUT));

            assertThatThrownBy(() -> aiChatService.chat(new AiChatRequest(USER_MESSAGE)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("temporarily unavailable");
        }

        @Test
        @DisplayName("throws IllegalArgumentException for INVALID_REQUEST")
        void invalidRequestThrowsIllegalArgument() {
            when(groqClient.chat(anyString(), anyString()))
                    .thenThrow(new GroqClientException(
                            "bad request", GroqClientException.ErrorType.INVALID_REQUEST));

            assertThatThrownBy(() -> aiChatService.chat(new AiChatRequest(USER_MESSAGE)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("rephrasing");
        }
    }
}
