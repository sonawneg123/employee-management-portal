package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskStatusRequest;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskActivityResponse;
import com.company.employeemanagement.dto.response.TaskDashboardStatsResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.dto.response.WorkloadResponse;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing task management endpoints.
 *
 * <p>Base path: {@code /api/tasks}
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/tasks")
@Tag(name = "Tasks", description = "Task creation, assignment, and lifecycle management")
@SecurityRequirement(name = "BearerAuth")
public class TaskController {

    private final TaskService taskService;

    public TaskController(final TaskService taskService) {
        this.taskService = taskService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List tasks",
               description = "Returns a paginated list of tasks. Employees are automatically scoped to their own assigned tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of tasks returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<TaskResponse>> findAll(
            @RequestParam(required = false) final UUID assignedEmployeeId,
            @RequestParam(required = false) final UUID createdByEmployeeId,
            @RequestParam(required = false) final TaskStatus status,
            @RequestParam(required = false) final TaskPriority priority,
            @RequestParam(required = false) final TaskCategory category,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt") final String sort,
            @RequestParam(defaultValue = "desc") final String direction
    ) {
        Pageable pageable = buildPageable(page, size, sort, direction);
        return ResponseEntity.ok(
                taskService.findAll(assignedEmployeeId, createdByEmployeeId,
                        status, priority, category, pageable));
    }

    @GetMapping(value = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "My assigned tasks")
    public ResponseEntity<PageResponse<TaskResponse>> myAssignedTasks(
            @RequestParam(required = false) final TaskStatus status,
            @RequestParam(required = false) final TaskPriority priority,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "dueDate") final String sort,
            @RequestParam(defaultValue = "asc") final String direction
    ) {
        Pageable pageable = buildPageable(page, size, sort, direction);
        return ResponseEntity.ok(taskService.findMyAssignedTasks(status, priority, pageable));
    }

    @GetMapping(value = "/created", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Tasks created by me")
    public ResponseEntity<PageResponse<TaskResponse>> myCreatedTasks(
            @RequestParam(required = false) final TaskStatus status,
            @RequestParam(required = false) final TaskPriority priority,
            @RequestParam(defaultValue = "0") final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "createdAt") final String sort,
            @RequestParam(defaultValue = "desc") final String direction
    ) {
        Pageable pageable = buildPageable(page, size, sort, direction);
        return ResponseEntity.ok(taskService.findMyCreatedTasks(status, priority, pageable));
    }

    @GetMapping(value = "/dashboard-stats", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Task dashboard statistics",
               description = "Returns aggregate task counts, overdue count, and completion percentage.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Dashboard statistics returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskDashboardStatsResponse> dashboardStats() {
        return ResponseEntity.ok(taskService.getDashboardStats());
    }

    @GetMapping(value = "/workload-summary", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Employee workload summary",
               description = "Returns active task counts per employee.")
    public ResponseEntity<List<WorkloadResponse>> workloadSummary() {
        return ResponseEntity.ok(taskService.getWorkloadSummary());
    }

    @GetMapping(value = "/workload/{employeeId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Employee workload",
               description = "Returns workload details for a specific employee.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Workload returned"),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<WorkloadResponse> workload(
            @PathVariable final UUID employeeId) {
        return ResponseEntity.ok(taskService.getWorkload(employeeId));
    }

    @GetMapping(value = "/employee-availability", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Employee availability",
               description = "Returns all active employees with their check-in status and active task count.")
    public ResponseEntity<List<EmployeeAvailabilityResponse>> employeeAvailability() {
        return ResponseEntity.ok(taskService.getEmployeeAvailability());
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get task by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task found"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskResponse> findById(@PathVariable final UUID id) {
        return ResponseEntity.ok(taskService.findById(id));
    }

    @GetMapping(value = "/{id}/activities", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Task activity timeline",
               description = "Returns the full activity timeline for a task. Employees restricted to own tasks.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Activity timeline returned"),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Task not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<TaskActivityResponse>> activityTimeline(
            @PathVariable final UUID id) {
        return ResponseEntity.ok(taskService.getActivityTimeline(id));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Mutating endpoints
    // ─────────────────────────────────────────────────────────────────────────

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Create task")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Task created"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Referenced employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Employee not checked in",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskResponse> create(
            @Valid @RequestBody final CreateTaskRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.create(request));
    }

    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Update task")
    public ResponseEntity<TaskResponse> update(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateTaskRequest request) {
        return ResponseEntity.ok(taskService.update(id, request));
    }

    @PatchMapping(value = "/{id}/status",
                  consumes = MediaType.APPLICATION_JSON_VALUE,
                  produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Update task status")
    public ResponseEntity<TaskResponse> updateStatus(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateTaskStatusRequest request) {
        return ResponseEntity.ok(taskService.updateStatus(id, request));
    }

    @PostMapping(value = "/{id}/reassign",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Reassign task",
               description = "Reassigns a task to a different employee. The new employee must be checked in.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Task reassigned"),
            @ApiResponse(responseCode = "404", description = "Task or employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "New employee not checked in",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<TaskResponse> reassign(
            @PathVariable final UUID id,
            @Valid @RequestBody final ReassignTaskRequest request) {
        return ResponseEntity.ok(taskService.reassign(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Delete task")
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        taskService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private Pageable buildPageable(final int page, final int size,
                                    final String sortField, final String direction) {
        Sort sort = direction.equalsIgnoreCase("asc")
                ? Sort.by(sortField).ascending()
                : Sort.by(sortField).descending();
        return PageRequest.of(page, Math.min(size, 100), sort);
    }
}
