package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.UpdateUserRoleRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.UserListResponse;
import com.company.employeemanagement.service.AdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller exposing ADMIN-only user-management endpoints.
 *
 * <p>Base path: {@code /api/admin/users}
 *
 * <p>All endpoints require the {@code ROLE_ADMIN} authority.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/admin/users")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin – User Management", description = "ADMIN-only operations for managing user accounts and roles")
@SecurityRequirement(name = "BearerAuth")
public class AdminController {

    private final AdminService adminService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param adminService the admin service
     */
    public AdminController(final AdminService adminService) {
        this.adminService = adminService;
    }

    /**
     * Returns a paginated list of all user accounts.
     *
     * @param page zero-based page number (default: 0)
     * @param size page size (default: 20)
     * @return paginated list of users
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List all users",
               description = "Returns a paginated list of all user accounts. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "User list returned"),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<UserListResponse>> findAll(
            @Parameter(description = "Zero-based page number", example = "0")
            @RequestParam(defaultValue = "0") final int page,

            @Parameter(description = "Page size (1–100)", example = "20")
            @RequestParam(defaultValue = "20") final int size) {

        return ResponseEntity.ok(adminService.findAllUsers(page, Math.min(size, 100)));
    }

    /**
     * Updates the role of a user account.
     *
     * <p>Replaces all existing roles with the single specified role.
     *
     * @param id      UUID of the user to update
     * @param request the role update payload
     * @return the updated user
     */
    @PutMapping(value = "/{id}/role",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Update user role",
               description = "Replaces the user's roles with the specified role. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Role updated"),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "User or role not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserListResponse> updateRole(
            @Parameter(description = "UUID of the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateUserRoleRequest request) {

        return ResponseEntity.ok(adminService.updateUserRole(id, request.roleName()));
    }

    /**
     * Enables or disables a user account.
     *
     * @param id      UUID of the user to update
     * @param enabled {@code true} to enable; {@code false} to disable
     * @return the updated user
     */
    @PutMapping(value = "/{id}/enabled",
                produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Enable or disable a user account",
               description = "Sets the isEnabled flag on a user account. Requires ROLE_ADMIN.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Account status updated"),
            @ApiResponse(responseCode = "404", description = "User not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Insufficient permissions",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<UserListResponse> setEnabled(
            @Parameter(description = "UUID of the user", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
            @PathVariable final UUID id,
            @Parameter(description = "true to enable; false to disable", example = "true")
            @RequestParam final boolean enabled) {

        return ResponseEntity.ok(adminService.setUserEnabled(id, enabled));
    }
}
