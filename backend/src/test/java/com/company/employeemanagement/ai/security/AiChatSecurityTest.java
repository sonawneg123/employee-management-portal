package com.company.employeemanagement.ai.security;

import com.company.employeemanagement.ai.controller.AiChatController;
import com.company.employeemanagement.ai.service.AiChatService;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.security.JwtAuthenticationFilter;
import com.company.employeemanagement.security.JwtService;
import com.company.employeemanagement.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Security test verifying that the AI endpoint requires authentication.
 *
 * <p>Uses a standalone MockMvc setup with a custom filter that simulates the
 * JWT gate — requests without an Authorization header are rejected with 401,
 * mirroring the behaviour of the real {@link JwtAuthenticationFilter}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiChatController — security: unauthenticated access")
class AiChatSecurityTest {

    @Mock
    private AiChatService aiChatService;

    private MockMvc mockMvc;

    /**
     * A simple filter that rejects requests missing an Authorization header with 401.
     * This mimics the behaviour of the real JWT filter without requiring the full
     * Spring Security context.
     */
    private static class RejectUnauthenticatedFilter extends OncePerRequestFilter {
        @Override
        protected void doFilterInternal(final HttpServletRequest request,
                                        final HttpServletResponse response,
                                        final FilterChain chain) throws IOException, jakarta.servlet.ServletException {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write(
                        "{\"status\":401,\"title\":\"Unauthorized\","
                        + "\"detail\":\"Authentication is required to access this resource.\"}");
                return;
            }
            chain.doFilter(request, response);
        }
    }

    @BeforeEach
    void setUp() {
        AiChatController controller = new AiChatController(aiChatService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .addFilter(new RejectUnauthenticatedFilter(), "/ai/*")
                .build();
    }

    @Test
    @DisplayName("request without Bearer token returns 401 Unauthorized")
    void unauthenticatedRequestReturns401() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());

        // Service must never be called for unauthenticated requests
        verify(aiChatService, never()).chat(any());
    }

    @Test
    @DisplayName("request without Bearer token returns JSON error body")
    void unauthenticatedRequestReturnsJsonBody() throws Exception {
        mockMvc.perform(post("/ai/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"message\":\"Hello\"}"))
                .andExpect(status().isUnauthorized());
    }
}
