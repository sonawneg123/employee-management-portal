package com.company.employeemanagement.ai.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Response DTO returned from the AI chat endpoint.
 *
 * @param answer the AI-generated response to the user's message
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Response payload from the AI HR Assistant chat endpoint")
public record AiChatResponse(

        @Schema(
                description = "The AI assistant's generated response",
                example = "The standard annual leave entitlement is 20 working days per year."
        )
        String answer

) {
}
