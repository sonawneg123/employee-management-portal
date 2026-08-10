package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.ChangePasswordRequest;
import com.company.employeemanagement.service.SettingsService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing user settings endpoints.
 *
 * <p>Base path: {@code /api/settings}
 *
 * <p>All endpoints require a valid JWT Bearer token. All operations are
 * automatically scoped to the currently authenticated user — no user ID is
 * accepted.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/settings")
@Tag(name = "Settings", description = "User account settings — password management")
@SecurityRequirement(name = "BearerAuth")
public class SettingsController {

    private final SettingsService settingsService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param settingsService the settings management service
     */
    public SettingsController(final SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    /**
     * Changes the authenticated user's password.
     *
     * <p>Validates the current password, confirms the new password matches
     * its confirmation, and persists the BCrypt-hashed new password.
     *
     * @param request the change-password payload
     * @return {@code 204 No Content} on success
     */
    @PostMapping(value = "/change-password",
                 consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Change password",
               description = "Verifies the current password and replaces it with the new one.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Validation failed or passwords don't match",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Current password incorrect or not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody final ChangePasswordRequest request) {
        settingsService.changePassword(
                request.currentPassword(),
                request.newPassword(),
                request.confirmPassword());
        return ResponseEntity.noContent().build();
    }
}
