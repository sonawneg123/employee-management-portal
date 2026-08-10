package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateAttendanceRequest;
import com.company.employeemanagement.dto.request.UpdateAttendanceRequest;
import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.service.AttendanceService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller exposing attendance management endpoints.
 *
 * <p>Base path: {@code /api/attendance}
 *
 * <p>Role-based access:
 * <ul>
 *   <li>Listing all records — ADMIN, HR, MANAGER</li>
 *   <li>Viewing a single record — ADMIN, HR, MANAGER, or the owning EMPLOYEE</li>
 *   <li>My attendance — all authenticated roles (scoped to caller)</li>
 *   <li>Creating — ADMIN, HR</li>
 *   <li>Updating — ADMIN, HR</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/attendance")
@Tag(name = "Attendance", description = "Attendance record management and employee self-service")
@SecurityRequirement(name = "BearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param attendanceService the attendance management service
     */
    public AttendanceController(final AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    /**
     * Returns a paginated list of attendance records (HR / Admin / Manager view).
     *
     * @param employeeId optional UUID to filter by employee
     * @param date       optional date filter (ISO format: {@code yyyy-MM-dd})
     * @param status     optional attendance status filter
     * @param page       zero-based page number (default: 0)
     * @param size       page size between 1 and 100 (default: 20)
     * @param sortBy     field to sort by (default: {@code "attendanceDate"})
     * @param sortDir    sort direction: {@code asc} or {@code desc} (default: {@code "desc"})
     * @return paginated list of attendance records
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "List attendance records",
               description = "Returns a paginated list of attendance records. Requires ADMIN, HR, or MANAGER role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of attendance records returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<AttendanceResponse>> findAll(
            @Parameter(description = "Filter by employee UUID")
            @RequestParam(required = false) final UUID employeeId,

            @Parameter(description = "Filter by attendance date (yyyy-MM-dd)", example = "2025-07-01")
            @RequestParam(required = false) final LocalDate date,

            @Parameter(description = "Filter by attendance status")
            @RequestParam(required = false) final AttendanceStatus status,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") final int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") final int size,

            @Parameter(description = "Sort field", example = "attendanceDate")
            @RequestParam(defaultValue = "attendanceDate") final String sortBy,

            @Parameter(description = "Sort direction: asc or desc", example = "desc")
            @RequestParam(defaultValue = "desc") final String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(attendanceService.findAll(employeeId, date, status, pageable));
    }

    /**
     * Returns the authenticated user's own attendance records.
     *
     * @param date    optional date filter
     * @param status  optional status filter
     * @param page    zero-based page number (default: 0)
     * @param size    page size (default: 20)
     * @param sortBy  sort field (default: {@code "attendanceDate"})
     * @param sortDir sort direction (default: {@code "desc"})
     * @return the caller's attendance records
     */
    @GetMapping(value = "/my", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "My attendance",
               description = "Returns the authenticated user's own attendance records.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of own attendance records returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<AttendanceResponse>> myAttendance(
            @Parameter(description = "Filter by date (yyyy-MM-dd)")
            @RequestParam(required = false) final LocalDate date,

            @Parameter(description = "Filter by attendance status")
            @RequestParam(required = false) final AttendanceStatus status,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") final int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") final int size,

            @Parameter(description = "Sort field", example = "attendanceDate")
            @RequestParam(defaultValue = "attendanceDate") final String sortBy,

            @Parameter(description = "Sort direction: asc or desc", example = "desc")
            @RequestParam(defaultValue = "desc") final String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(attendanceService.findMyAttendance(date, status, pageable));
    }

    /**
     * Returns a single attendance record by UUID.
     *
     * @param id the UUID of the attendance record
     * @return the matching {@link AttendanceResponse}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get attendance record by ID",
               description = "Returns the attendance record with the given UUID. "
                           + "Employees may only retrieve their own records.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance record found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AttendanceResponse.class))),
            @ApiResponse(responseCode = "404", description = "Attendance record not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AttendanceResponse> findById(
            @Parameter(description = "UUID of the attendance record",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        return ResponseEntity.ok(attendanceService.findById(id));
    }

    /**
     * Creates a new attendance record (HR / Admin manual marking).
     *
     * @param request the creation payload
     * @return {@code 201 Created} with the new {@link AttendanceResponse}
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create attendance record",
               description = "Manually marks attendance for an employee. Requires ADMIN or HR role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attendance record created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AttendanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or duplicate record",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AttendanceResponse> create(
            @Valid @RequestBody final CreateAttendanceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(attendanceService.create(request));
    }

    /**
     * Updates an existing attendance record (HR / Admin only).
     *
     * @param id      the UUID of the attendance record to update
     * @param request the replacement payload
     * @return the updated {@link AttendanceResponse}
     */
    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update attendance record",
               description = "Updates check-in/out times, status, and notes. Requires ADMIN or HR role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Attendance record updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AttendanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Attendance record not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AttendanceResponse> update(
            @Parameter(description = "UUID of the attendance record to update",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.update(id, request));
    }
}
