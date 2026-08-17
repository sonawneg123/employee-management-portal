package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.UpdateEmployeeRequest;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.service.EmployeeService;
import com.company.employeemanagement.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
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

import java.io.IOException;
import java.util.UUID;

/**
 * REST controller exposing employee management endpoints.
 *
 * <p>Base path: {@code /api/employees}
 *
 * <p>All endpoints require a valid JWT Bearer token.
 * Role-based access control is enforced via {@link PreAuthorize} annotations:
 * <ul>
 *   <li>{@code GET} — accessible by ADMIN, HR, MANAGER, EMPLOYEE</li>
 *   <li>{@code POST / PUT} — accessible by ADMIN, HR only</li>
 *   <li>{@code DELETE} — accessible by ADMIN only</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/employees")
@Tag(name = "Employees", description = "CRUD operations and search for employee records")
@SecurityRequirement(name = "BearerAuth")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeRepository employeeRepository;
    private final FileStorageService fileStorageService;

    /**
     * Constructs the controller with its required dependencies.
     *
     * @param employeeService    the employee management service
     * @param employeeRepository repository for direct employee lookups
     * @param fileStorageService service for reading profile photo files
     */
    public EmployeeController(final EmployeeService employeeService,
                               final EmployeeRepository employeeRepository,
                               final FileStorageService fileStorageService) {
        this.employeeService = employeeService;
        this.employeeRepository = employeeRepository;
        this.fileStorageService = fileStorageService;
    }

    /**
     * Returns a paginated, optionally filtered list of employees.
     *
     * @param keyword      optional search term (matched against name and job title)
     * @param departmentId optional UUID to filter by department
     * @param status       optional employment status filter
     * @param page         zero-based page number (default: 0)
     * @param size         page size between 1 and 100 (default: 20)
     * @param sortBy       field name to sort by (default: {@code "createdAt"})
     * @param sortDir      sort direction: {@code asc} or {@code desc} (default: {@code "desc"})
     * @return a paginated {@link PageResponse} of {@link EmployeeResponse} records
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(
            summary = "List employees",
            description = "Returns a paginated list of employees. Optionally filter by keyword, departmentId, or status."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Page of employees returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<EmployeeResponse>> findAll(
            @Parameter(description = "Optional search keyword (name or job title)")
            @RequestParam(required = false) final String keyword,

            @Parameter(description = "Filter by department UUID")
            @RequestParam(required = false) final UUID departmentId,

            @Parameter(description = "Filter by employment status")
            @RequestParam(required = false) final EmployeeStatus status,

            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") final int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") final int size,

            @Parameter(description = "Sort field", example = "createdAt")
            @RequestParam(defaultValue = "createdAt") final String sortBy,

            @Parameter(description = "Sort direction: asc or desc", example = "desc")
            @RequestParam(defaultValue = "desc") final String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), sort);
        return ResponseEntity.ok(employeeService.findAll(keyword, departmentId, status, pageable));
    }

    /**
     * Returns a single employee by UUID.
     *
     * @param id the UUID of the employee
     * @return the matching {@link EmployeeResponse}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get employee by ID",
               description = "Returns the employee with the given UUID.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmployeeResponse> findById(
            @Parameter(description = "UUID of the employee",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        return ResponseEntity.ok(employeeService.findById(id));
    }

    /**
     * Creates a new employee record.
     *
     * @param request the creation payload
     * @return {@code 201 Created} with the new {@link EmployeeResponse}
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Create employee",
               description = "Creates a new employee record. Requires ADMIN or HR role.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Employee created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Employee code already exists",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmployeeResponse> create(
            @Valid @RequestBody final CreateEmployeeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(employeeService.create(request));
    }

    /**
     * Fully replaces an existing employee record.
     *
     * @param id      the UUID of the employee to update
     * @param request the replacement payload
     * @return the updated {@link EmployeeResponse}
     */
    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    @Operation(summary = "Update employee",
               description = "Replaces all updatable fields of an employee. Requires ADMIN or HR role.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Employee updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = EmployeeResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<EmployeeResponse> update(
            @Parameter(description = "UUID of the employee to update",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateEmployeeRequest request) {
        return ResponseEntity.ok(employeeService.update(id, request));
    }

    /**
     * Deletes an employee by UUID.
     *
     * @param id the UUID of the employee to delete
     * @return {@code 204 No Content} on success
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete employee",
               description = "Permanently deletes an employee record. Requires ADMIN role.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Employee deleted"),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "UUID of the employee to delete",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) {
        employeeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Streams the profile photo for any employee by UUID.
     *
     * <p>This endpoint allows any authenticated user to retrieve a colleague's
     * profile photo for display in employee tables and avatars.
     *
     * @param id UUID of the employee whose photo to retrieve
     * @return the image bytes with the correct {@code Content-Type} header
     * @throws IOException if the underlying file cannot be read
     */
    @GetMapping("/{id}/profile-photo")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get employee profile photo",
               description = "Returns the profile photo for the given employee. "
                       + "Any authenticated role may call this endpoint.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Photo returned"),
            @ApiResponse(responseCode = "404", description = "Employee or photo not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<InputStreamResource> getProfilePhoto(
            @Parameter(description = "UUID of the employee",
                       example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id) throws IOException {

        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", id));

        if (employee.getProfilePhotoStorageKey() == null) {
            throw new ResourceNotFoundException("Profile photo", "employeeId", id);
        }

        String mimeType = employee.getProfilePhotoMimeType();
        MediaType mediaType = (mimeType != null && !mimeType.isBlank())
                ? MediaType.parseMediaType(mimeType)
                : MediaType.APPLICATION_OCTET_STREAM;

        InputStreamResource resource = new InputStreamResource(
                fileStorageService.openForRead(employee.getProfilePhotoStorageKey()));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + employee.getProfilePhotoStoredName() + "\"")
                .contentType(mediaType)
                .body(resource);
    }
}
