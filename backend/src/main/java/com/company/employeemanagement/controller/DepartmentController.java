package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateDepartmentRequest;
import com.company.employeemanagement.dto.request.UpdateDepartmentRequest;
import com.company.employeemanagement.dto.response.DepartmentResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.service.DepartmentService;
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

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing department management endpoints.
 *
 * <p>Base path: {@code /api/departments}
 *
 * <p>Role-based access control:
 * <ul>
 *   <li>{@code GET} — all authenticated roles (ADMIN, HR, MANAGER, EMPLOYEE)</li>
 *   <li>{@code POST / PUT} — ADMIN and HR only</li>
 *   <li>{@code DELETE} — ADMIN only</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/departments")
@Tag(name = "Departments", description = "CRUD operations for organisational departments")
@SecurityRequirement(name = "BearerAuth")
public class DepartmentController {

    private final DepartmentService departmentService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param departmentService the department management service
     */
    public DepartmentController(final DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    /**
     * Returns all departments as a flat list.
     *
     * <p>Intended for use in dropdowns and auto-complete widgets.
     * Returns all departments in one call (no pagination).
     *
     * @return list of all departments ordered by name
     */
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List all departments (no pagination)",
               description = "Returns a flat list of all departments. Use for dropdowns.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "List returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<List<DepartmentResponse>> findAll() {
        return ResponseEntity.ok(departmentService.findAll());
    }

    /**
     * Returns a paginated, optionally keyword-filtered list of departments.
     *
     * @param keyword optional search term (matched against name and code)
     * @param page    zero-based page number (default: 0)
     * @param size    page size between 1 and 100 (default: 20)
     * @param sortBy  field name to sort by (default: {@code "name"})
     * @param sortDir sort direction: {@code asc} or {@code desc} (default: {@code "asc"})
     * @return paginated {@link PageResponse} of {@link DepartmentResponse}
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List departments (paginated)",
               description = "Returns a paginated list of departments. Optionally filter by keyword.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of departments returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<DepartmentResponse>> findAllPaged(
            @Parameter(description = "Optional search keyword (name or code)")
            @RequestParam(required = false) final String keyword,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") final int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") final int size,

            @Parameter(description = "Sort field", example = "name")
            @RequestParam(defaultValue = "name") final String sortBy,

            @Parameter(description = "Sort direction: asc or desc", example = "asc")
            @RequestParam(defaultValue = "asc") final String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(departmentService.findAllPaged(keyword, pageable));
    }

    /**
     * Returns a single department by UUID.
     *
     * @param id the UUID of the department
     * @return the matching {@link DepartmentResponse}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get department by ID",
               description = "Returns the department with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DepartmentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DepartmentResponse> findById(
            @Parameter(description = "UUID of the department",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        return ResponseEntity.ok(departmentService.findById(id));
    }

    /**
     * Creates a new department.
     *
     * @param request the creation payload
     * @return {@code 201 Created} with the new {@link DepartmentResponse}
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create department",
               description = "Creates a new department. Requires ADMIN or HR role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Department created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DepartmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Department code already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DepartmentResponse> create(
            @Valid @RequestBody final CreateDepartmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(departmentService.create(request));
    }

    /**
     * Fully replaces an existing department record.
     *
     * @param id      the UUID of the department to update
     * @param request the replacement payload
     * @return the updated {@link DepartmentResponse}
     */
    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update department",
               description = "Replaces name and code of a department. Requires ADMIN or HR role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Department updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = DepartmentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Department code already used",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<DepartmentResponse> update(
            @Parameter(description = "UUID of the department to update",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateDepartmentRequest request) {
        return ResponseEntity.ok(departmentService.update(id, request));
    }

    /**
     * Deletes a department by UUID.
     *
     * <p>Will fail if the department still has employees assigned (FK constraint).
     *
     * @param id the UUID of the department to delete
     * @return {@code 204 No Content} on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete department",
               description = "Permanently deletes a department. Requires ADMIN role. "
                           + "Fails if employees are still assigned to the department.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Department deleted"),
            @ApiResponse(responseCode = "404", description = "Department not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the department to delete",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        departmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
