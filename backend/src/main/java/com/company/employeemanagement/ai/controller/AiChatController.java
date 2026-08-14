package com.company.employeemanagement.ai.controller;

import com.company.employeemanagement.ai.dto.AiChatRequest;
import com.company.employeemanagement.ai.dto.AiChatResponse;
import com.company.employeemanagement.ai.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing the AI HR Assistant chat endpoint.
 *
 * <p>Base path: {@code /api/ai}
 *
 * <p>All endpoints in this controller require a valid JWT Bearer token.
 * Authentication is enforced by the existing {@link com.company.employeemanagement.config.SecurityConfig}
 * — the {@code /ai/**} path falls under the {@code anyRequest().authenticated()} rule.
 *
 * <p>The Groq API key is handled entirely server-side; it is never included
 * in any response body or log message.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/ai")
@Tag(name = "AI Assistant", description = "Groq-powered HR AI Assistant — Phase 1 chat endpoint")
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * Constructs the controller with the AI chat service.
     *
     * @param aiChatService the AI chat application service
     */
    public AiChatController(final AiChatService aiChatService) {
        this.aiChatService = aiChatService;
    }

    /**
     * Sends a user message to the AI HR Assistant and returns the generated response.
     *
     * <p>The endpoint requires JWT authentication. Unauthenticated requests
     * receive a {@code 401 Unauthorized} JSON response from the security filter.
     *
     * @param request the validated chat request containing the user's message
     * @return {@code 200 OK} with the {@link AiChatResponse} body
     */
    @PostMapping(value = "/chat",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
            summary = "Send a message to the AI HR Assistant",
            description = "Forwards the user's message to a Groq-hosted LLM and returns the generated response. "
                    + "Requires a valid JWT Bearer token. The Groq API key is never exposed to the client."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "AI response returned successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AiChatResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed — message is blank or too long",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "AI service state conflict or configuration error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Unexpected server error",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AiChatResponse> chat(
            @Valid @RequestBody final AiChatRequest request) {
        return ResponseEntity.ok(aiChatService.chat(request));
    }
}
