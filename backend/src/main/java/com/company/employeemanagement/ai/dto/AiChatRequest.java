package com.company.employeemanagement.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for the AI chat endpoint.
 *
 * @param message the user's question or prompt sent to the AI assistant
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Request payload for the AI HR Assistant chat endpoint")
public record AiChatRequest(

        @Schema(
                description = "The user's message or question for the AI assistant",
                example = "What is the company leave policy?"
        )
        @NotBlank(message = "Message must not be blank")
        @Size(max = 4000, message = "Message must not exceed 4000 characters")
        String message

) {
}
