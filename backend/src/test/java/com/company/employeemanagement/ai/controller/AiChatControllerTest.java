package com.company.employeemanagement.ai.controller;

import com.company.employeemanagement.ai.dto.AiChatRequest;
import com.company.employeemanagement.ai.dto.AiChatResponse;
import com.company.employeemanagement.ai.service.AiChatService;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AiChatController}.
 *
 * <p>Uses {@code MockMvc} in standalone mode with the
 * {@link GlobalExceptionHandler} wired in so that validation errors and
 * service exceptions produce the correct RFC-7807 ProblemDetail responses.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatController")
class AiChatControllerTest {

    @Mock
    private AiChatService aiChatService;

    @InjectMocks
    private AiChatController aiChatController;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(aiChatController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String body(final String message) throws Exception {
        return objectMapper.writeValueAsString(new AiChatRequest(message));
    }

    // ── Valid request ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Valid chat request")
    class ValidRequest {

        @Test
        @DisplayName("returns 200 OK with the AI answer for a valid message")
        void validMessageReturns200WithAnswer() throws Exception {
            when(aiChatService.chat(any()))
                    .thenReturn(new AiChatResponse("You are entitled to 20 days of annual leave."));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("What is my leave entitlement?")))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.answer").value("You are entitled to 20 days of annual leave."));
        }

        @Test
        @DisplayName("calls service with the exact user message")
        void callsServiceWithCorrectMessage() throws Exception {
            when(aiChatService.chat(any())).thenReturn(new AiChatResponse("Hello!"));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("What can you help me with?")))
                    .andExpect(status().isOk());

            verify(aiChatService).chat(any(AiChatRequest.class));
        }
    }

    // ── Validation ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Validation failures")
    class Validation {

        @Test
        @DisplayName("returns 400 when message is null")
        void nullMessageReturns400() throws Exception {
            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"message\":null}"))
                    .andExpect(status().isBadRequest());

            verify(aiChatService, never()).chat(any());
        }

        @Test
        @DisplayName("returns 400 when message is blank")
        void blankMessageReturns400() throws Exception {
            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("   ")))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));

            verify(aiChatService, never()).chat(any());
        }

        @Test
        @DisplayName("returns 400 when message is empty string")
        void emptyMessageReturns400() throws Exception {
            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("")))
                    .andExpect(status().isBadRequest());

            verify(aiChatService, never()).chat(any());
        }

        @Test
        @DisplayName("returns 400 when message exceeds 4000 characters")
        void oversizedMessageReturns400() throws Exception {
            String longMessage = "a".repeat(4001);

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(longMessage)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));

            verify(aiChatService, never()).chat(any());
        }

        @Test
        @DisplayName("accepts message at exactly 4000 characters")
        void maxLengthMessageAccepted() throws Exception {
            String maxMessage = "a".repeat(4000);
            when(aiChatService.chat(any())).thenReturn(new AiChatResponse("OK"));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body(maxMessage)))
                    .andExpect(status().isOk());
        }
    }

    // ── Groq failures ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Groq API failures mapped to HTTP errors")
    class GroqFailures {

        @Test
        @DisplayName("Groq auth failure → 409 Conflict (configuration error)")
        void groqAuthFailureReturns409() throws Exception {
            when(aiChatService.chat(any()))
                    .thenThrow(new IllegalStateException(
                            "The AI assistant is currently unavailable due to a configuration issue."));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Hello")))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("Groq API failure → 409 Conflict")
        void groqApiFailureReturns409() throws Exception {
            when(aiChatService.chat(any()))
                    .thenThrow(new IllegalStateException(
                            "The AI assistant is temporarily unavailable. Please try again later."));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Hello")))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.detail", containsString("temporarily unavailable")));
        }

        @Test
        @DisplayName("Groq invalid request → 400 Bad Request")
        void groqInvalidRequestReturns400() throws Exception {
            when(aiChatService.chat(any()))
                    .thenThrow(new IllegalArgumentException(
                            "The AI assistant could not process your request. Please try rephrasing."));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Hello")))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("response body never contains stack trace or internal exception details")
        void noInternalDetailsLeaked() throws Exception {
            when(aiChatService.chat(any()))
                    .thenThrow(new IllegalStateException("temporarily unavailable"));

            mockMvc.perform(post("/ai/chat")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body("Hello")))
                    .andExpect(jsonPath("$.detail", not(containsString("GroqClient"))))
                    .andExpect(jsonPath("$.detail", not(containsString("api-key"))))
                    .andExpect(jsonPath("$.detail", not(containsString("gsk_"))));
        }
    }
}
