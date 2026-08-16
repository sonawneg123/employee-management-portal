package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for updating an existing submission (resubmit after changes requested).
 *
 * @param submissionNotes    Updated summary of what was done (required)
 * @param workCompleted      Updated description of completed work
 * @param additionalComments Updated optional comments
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for updating/resubmitting a task submission")
public record UpdateTaskSubmissionRequest(

        @Schema(description = "Updated summary notes describing what was done",
                example = "Addressed the review comments — refactored the auth flow.")
        @NotBlank(message = "Submission notes are required")
        String submissionNotes,

        @Schema(description = "Updated description of work completed")
        String workCompleted,

        @Schema(description = "Updated optional comments")
        String additionalComments
) {
}
