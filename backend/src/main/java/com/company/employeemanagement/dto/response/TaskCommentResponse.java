package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a task comment returned to API consumers.
 *
 * @param id           UUID of the comment
 * @param taskId       UUID of the task this comment belongs to
 * @param authorId     UUID of the authoring employee
 * @param authorName   Display name of the author
 * @param content      Text body of the comment
 * @param edited       Whether the comment has been edited after posting
 * @param createdAt    Timestamp when the comment was created
 * @param updatedAt    Timestamp of the last modification
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Task comment as returned by the API")
public record TaskCommentResponse(

        @Schema(description = "UUID of the comment")
        UUID id,

        @Schema(description = "UUID of the parent task")
        UUID taskId,

        @Schema(description = "UUID of the author employee")
        UUID authorId,

        @Schema(description = "Display name of the author", example = "Jane Doe")
        String authorName,

        @Schema(description = "Text body of the comment")
        String content,

        @Schema(description = "True if the comment was edited after original posting")
        boolean edited,

        @Schema(description = "Comment creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Comment last-modified timestamp")
        LocalDateTime updatedAt
) {
}
