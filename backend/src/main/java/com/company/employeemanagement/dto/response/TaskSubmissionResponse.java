package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.SubmissionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a task submission returned to API consumers.
 *
 * @param id                         UUID primary key of the submission
 * @param taskId                     UUID of the parent task
 * @param taskTitle                  Title of the parent task (denormalised for convenience)
 * @param submittedById              UUID of the submitting employee
 * @param submittedByName            Full name of the submitting employee
 * @param submissionNotes            Employee's summary notes
 * @param workCompleted              Detailed work description
 * @param additionalComments         Optional extra comments from the employee
 * @param submittedAt                Timestamp of submission (or resubmission)
 * @param reviewStatus               Current review status
 * @param reviewComment              Manager's review comment (null until review occurs)
 * @param reviewedById               UUID of the reviewing manager, or null
 * @param reviewedByName             Name of the reviewing manager, or null
 * @param reviewedAt                 Timestamp of manager review, or null
 * @param createdAt                  Record creation timestamp
 * @param updatedAt                  Record last-modified timestamp
 * @param attachmentOriginalName     Original filename as supplied by the browser, or null
 * @param attachmentMimeType         Validated MIME type of the attachment, or null
 * @param attachmentSizeBytes        File size in bytes, or null
 * @param attachmentUploadedAt       When the attachment was stored, or null
 * @param hasAttachment              Convenience flag — true when an attachment is present
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Task submission record as returned by the API")
public record TaskSubmissionResponse(

        @Schema(description = "UUID of the submission")
        UUID id,

        @Schema(description = "UUID of the parent task")
        UUID taskId,

        @Schema(description = "Title of the parent task", example = "Implement login page")
        String taskTitle,

        @Schema(description = "UUID of the submitting employee")
        UUID submittedById,

        @Schema(description = "Full name of the submitting employee", example = "Jane Doe")
        String submittedByName,

        @Schema(description = "Employee's summary notes about what was done")
        String submissionNotes,

        @Schema(description = "Detailed description of work completed")
        String workCompleted,

        @Schema(description = "Optional additional comments from the employee")
        String additionalComments,

        @Schema(description = "Timestamp when the work was submitted (or last resubmitted)")
        LocalDateTime submittedAt,

        @Schema(description = "Current review status of the submission", example = "PENDING_REVIEW")
        SubmissionStatus reviewStatus,

        @Schema(description = "Manager's review comment (present after review)")
        String reviewComment,

        @Schema(description = "UUID of the manager who reviewed this submission")
        UUID reviewedById,

        @Schema(description = "Name of the manager who reviewed this submission")
        String reviewedByName,

        @Schema(description = "Timestamp when the manager reviewed this submission")
        LocalDateTime reviewedAt,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt,

        // ── Attachment metadata (Phase 6B.1) ───────────────────────────────────

        @Schema(description = "Original filename of the attachment as supplied by the browser",
                example = "report.pdf")
        String attachmentOriginalName,

        @Schema(description = "MIME type of the attachment", example = "application/pdf")
        String attachmentMimeType,

        @Schema(description = "File size of the attachment in bytes", example = "204800")
        Long attachmentSizeBytes,

        @Schema(description = "Timestamp when the attachment was uploaded")
        LocalDateTime attachmentUploadedAt,

        @Schema(description = "True when a file attachment is present with this submission")
        boolean hasAttachment
) {
}
