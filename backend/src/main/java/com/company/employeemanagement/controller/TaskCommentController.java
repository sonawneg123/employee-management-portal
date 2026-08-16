package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateTaskCommentRequest;
import com.company.employeemanagement.dto.response.TaskCommentResponse;
import com.company.employeemanagement.service.TaskCommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for task comment operations.
 *
 * <p>Base path: {@code /api/tasks/{taskId}/comments}
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/tasks/{taskId}/comments")
@Tag(name = "Task Comments", description = "Discussion thread on a task")
@SecurityRequirement(name = "BearerAuth")
public class TaskCommentController {

    private final TaskCommentService commentService;

    public TaskCommentController(final TaskCommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * Returns all comments for the given task, oldest first.
     *
     * @param taskId UUID of the task
     * @return list of comments
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List task comments",
               description = "Returns all comments for the task. Employees are restricted to their own tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Comments returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TaskCommentResponse>> findByTaskId(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId) {
        return ResponseEntity.ok(commentService.findByTaskId(taskId));
    }

    /**
     * Posts a new comment on the task.
     *
     * @param taskId  UUID of the task
     * @param request the comment payload
     * @return {@code 201 Created} with the new comment
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Post a comment",
               description = "Adds a new comment to the task discussion thread.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Comment created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskCommentResponse> create(
            @Parameter(description = "UUID of the task")
            @PathVariable final UUID taskId,
            @Valid @RequestBody final CreateTaskCommentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(commentService.create(taskId, request));
    }
}
