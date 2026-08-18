package com.company.employeemanagement.ai.agent.controller;

import com.company.employeemanagement.ai.agent.dto.AgentChatRequest;
import com.company.employeemanagement.ai.agent.dto.AgentChatResponse;
import com.company.employeemanagement.ai.agent.service.AiAgentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for the Phase 7E Agentic AI Copilot.
 *
 * <p>Base path: {@code /api/ai/agent}
 * Requires authentication. Role-aware tool authorization is handled in the service layer.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/ai/agent")
@Tag(name = "AI Copilot", description = "Phase 7E Agentic AI Copilot")
public class AiAgentController {

    private final AiAgentService agentService;

    public AiAgentController(final AiAgentService agentService) {
        this.agentService = agentService;
    }

    /**
     * Sends a message to the Agentic AI Copilot and returns its response.
     *
     * <p>If {@code confirmationToken} is provided in the request, this turn
     * executes a previously proposed action.
     *
     * @param request the chat request
     * @return the agent's response
     */
    @PostMapping(value = "/chat",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirement(name = "BearerAuth")
    @Operation(
            summary = "Send a message to the AI Copilot",
            description = "Agentic chat endpoint. The agent can call tools to retrieve live data and propose actions."
    )
    public ResponseEntity<AgentChatResponse> chat(@RequestBody final AgentChatRequestDto request) {
        AgentChatRequest agentRequest = new AgentChatRequest(
                request.message(),
                request.confirmationToken()
        );
        return ResponseEntity.ok(agentService.chat(agentRequest));
    }

    /**
     * Request body DTO for the agent chat endpoint.
     *
     * @param message           the user's message
     * @param confirmationToken optional confirmation token for executing a proposed action
     */
    public record AgentChatRequestDto(
            @NotBlank(message = "Message must not be blank")
            @Size(max = 4000, message = "Message must not exceed 4000 characters")
            String message,

            String confirmationToken
    ) {}
}
