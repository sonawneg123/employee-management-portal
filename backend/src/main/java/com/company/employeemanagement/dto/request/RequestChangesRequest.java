package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for a manager requesting changes on a task submission.
 *
 * @param reviewComment Explanation of what needs to change (required)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for requesting changes on a task submission")
public record RequestChangesRequest(

        @Schema(description = "Manager's comments explaining what needs to change",
                example = "Please add unit tests for the error handling paths and update the API docs.")
        @NotBlank(message = "Review comment is required when requesting changes")
        String reviewComment
) {
}
