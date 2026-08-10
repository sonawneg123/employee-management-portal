package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateLeaveRequest;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.request.UpdateLeaveRequest;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import com.company.employeemanagement.service.LeaveRequestService;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing leave request management endpoints.
 *
 * <p>Base path: {@code /api/leaves}
 *
 * <p>Role-based access:
 * <ul>
 *   <li>Listing and viewing — all authenticated roles</li>
 *   <li>Creating and updating — all authenticated roles (employees manage their own leaves)</li>
 *   <li>Approve / Reject — HR and ADMIN only</li>
 *   <li>Cancel — the submitting employee (or ADMIN/HR)</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/leaves")
@Tag(name = "Leave Requests", description = "Leave request submission, review, and lifecycle management")
@SecurityRequirement(name = "BearerAuth")
public class LeaveController {

    private final LeaveRequestService leaveRequestService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param leaveRequestService the leave request management service
     */
    public LeaveController(final LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    /**
     * Returns a paginated list of leave requests, optionally filtered by employee,
     * status, and/or leave type.
     *
     * @param employeeId optional UUID to filter by employee
     * @param status     optional status filter (PENDING, APPROVED, REJECTED, CANCELLED)
     * @param type       optional leave type filter
     * @param page       zero-based page number (default: 0)
     * @param size       page size between 1 and 100 (default: 20)
     * @param sortBy     field to sort by — also accepted as {@code sort} (default: {@code "createdAt"})
     * @param sortDir    sort direction — also accepted as {@code direction} (default: {@code "desc"})
     * @param sort       alias for {@code sortBy} (frontend compatibility)
     * @param direction  alias for {@code sortDir} (frontend compatibility)
     * @return paginated list of leave requests
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List leave requests",
               description = "Returns a paginated list of leave requests. Filter by employeeId, status, or type.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of leave requests returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<LeaveRequestResponse>> findAll(
            @Parameter(description = "Filter by employee UUID")
            @RequestParam(required = false) final UUID employeeId,

            @Parameter(description = "Filter by leave status")
            @RequestParam(required = false) final LeaveStatus status,

            @Parameter(description = "Filter by leave type")
            @RequestParam(required = false) final LeaveType type,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") final int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") final int size,

            @Parameter(description = "Sort field (also accepted as 'sort')", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") final String sortBy,

            @Parameter(description = "Sort direction: asc or desc (also accepted as 'direction')",
                       example = "desc")
            @RequestParam(defaultValue = "desc") final String sortDir,

            @Parameter(description = "Alias for sortBy (frontend compatibility)", hidden = true)
            @RequestParam(required = false) final String sort,

            @Parameter(description = "Alias for sortDir (frontend compatibility)", hidden = true)
            @RequestParam(required = false) final String direction
    ) {
        final String effectiveSortBy  = (sort      != null) ? sort      : sortBy;
        final String effectiveSortDir = (direction != null) ? direction : sortDir;
        Sort sortObj = effectiveSortDir.equalsIgnoreCase("asc")
                ? Sort.by(effectiveSortBy).ascending()
                : Sort.by(effectiveSortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sortObj);
        return ResponseEntity.ok(leaveRequestService.findAll(employeeId, status, type, pageable));
    }

    /**
     * Returns a single leave request by UUID.
     *
     * @param id the UUID of the leave request
     * @return the matching {@link LeaveRequestResponse}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get leave request by ID",
               description = "Returns the leave request with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LeaveRequestResponse> findById(
            @Parameter(description = "UUID of the leave request",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        return ResponseEntity.ok(leaveRequestService.findById(id));
    }

    /**
     * Submits a new leave request.
     *
     * @param request the submission payload
     * @return {@code 201 Created} with the new {@link LeaveRequestResponse}
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Submit leave request",
               description = "Submits a new leave request. The request starts in PENDING status.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Leave request created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Referenced employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LeaveRequestResponse> create(
            @Valid @RequestBody final CreateLeaveRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(leaveRequestService.create(request));
    }

    /**
     * Updates a {@code PENDING} leave request.
     *
     * @param id      the UUID of the leave request
     * @param request the replacement payload
     * @return the updated {@link LeaveRequestResponse}
     */
    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Update leave request",
               description = "Modifies a leave request that is still in PENDING status.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or request not pending",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LeaveRequestResponse> update(
            @Parameter(description = "UUID of the leave request to update",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateLeaveRequest request) {
        return ResponseEntity.ok(leaveRequestService.update(id, request));
    }

    /**
     * Approves a {@code PENDING} leave request (HR / Admin only).
     *
     * @param id      the UUID of the leave request
     * @param request optional reviewer notes
     * @return the updated {@link LeaveRequestResponse} with {@code APPROVED} status
     */
    @PostMapping(value = "/{id}/approve",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Approve leave request",
               description = "Approves a PENDING leave request. Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request approved",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING status",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LeaveRequestResponse> approve(
            @Parameter(description = "UUID of the leave request to approve",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @RequestBody(required = false) final ReviewLeaveRequest request) {
        return ResponseEntity.ok(
                leaveRequestService.approve(id, request != null ? request : new ReviewLeaveRequest(null)));
    }

    /**
     * Rejects a {@code PENDING} leave request (HR / Admin only).
     *
     * @param id      the UUID of the leave request
     * @param request optional rejection reason
     * @return the updated {@link LeaveRequestResponse} with {@code REJECTED} status
     */
    @PostMapping(value = "/{id}/reject",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Reject leave request",
               description = "Rejects a PENDING leave request. Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Leave request rejected",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = LeaveRequestResponse.class))),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING status",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<LeaveRequestResponse> reject(
            @Parameter(description = "UUID of the leave request to reject",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @RequestBody(required = false) final ReviewLeaveRequest request) {
        return ResponseEntity.ok(
                leaveRequestService.reject(id, request != null ? request : new ReviewLeaveRequest(null)));
    }

    /**
     * Cancels a {@code PENDING} leave request (employee action).
     *
     * @param id the UUID of the leave request to cancel
     * @return {@code 204 No Content} on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Cancel leave request",
               description = "Cancels a leave request that is still in PENDING status.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Leave request cancelled"),
            @ApiResponse(responseCode = "400", description = "Request is not in PENDING status",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Leave request not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> cancel(
            @Parameter(description = "UUID of the leave request to cancel",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        leaveRequestService.cancel(id);
        return ResponseEntity.noContent().build();
    }
}
