package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a task attachment returned to API consumers.
 *
 * @param id           UUID of the attachment
 * @param taskId       UUID of the parent task
 * @param uploaderId   UUID of the uploader employee
 * @param uploaderName Display name of the uploader
 * @param originalName Original filename from the client
 * @param mimeType     MIME type of the file
 * @param sizeBytes    Size of the file in bytes
 * @param createdAt    Upload timestamp
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Task attachment metadata as returned by the API")
public record TaskAttachmentResponse(

        @Schema(description = "UUID of the attachment")
        UUID id,

        @Schema(description = "UUID of the parent task")
        UUID taskId,

        @Schema(description = "UUID of the uploader")
        UUID uploaderId,

        @Schema(description = "Display name of the uploader", example = "John Manager")
        String uploaderName,

        @Schema(description = "Original filename", example = "requirements.pdf")
        String originalName,

        @Schema(description = "MIME type", example = "application/pdf")
        String mimeType,

        @Schema(description = "File size in bytes", example = "204800")
        long sizeBytes,

        @Schema(description = "Upload timestamp")
        LocalDateTime createdAt
) {
}
