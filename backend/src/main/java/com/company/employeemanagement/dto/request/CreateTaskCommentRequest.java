package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for posting a new comment on a task.
 *
 * @param content The text body of the comment (required, max 4000 chars)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for creating a task comment")
public record CreateTaskCommentRequest(

        @Schema(description = "Comment text", example = "Please clarify the acceptance criteria.")
        @NotBlank(message = "Comment content is required")
        @Size(max = 4000, message = "Comment must not exceed 4000 characters")
        String content
) {
}
