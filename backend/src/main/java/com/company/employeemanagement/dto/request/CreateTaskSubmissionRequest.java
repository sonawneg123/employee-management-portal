package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new task submission (employee submits work for review).
 *
 * @param submissionNotes    Summary of what was done (required)
 * @param workCompleted      Detailed description of completed work
 * @param additionalComments Optional extra comments (e.g., known issues)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for submitting task work for manager review")
public record CreateTaskSubmissionRequest(

        @Schema(description = "Summary notes describing what was done",
                example = "Implemented the login page as per the design spec.")
        @NotBlank(message = "Submission notes are required")
        String submissionNotes,

        @Schema(description = "Detailed description of work completed",
                example = "Implemented LoginForm component, added JWT handling, wrote unit tests.")
        String workCompleted,

        @Schema(description = "Optional additional comments or caveats",
                example = "Noted one edge case with empty password — added a TODO for Phase 2.")
        String additionalComments
) {
}
