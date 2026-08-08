package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing authentication endpoints for user registration and login.
 *
 * <p>Base path: {@code /api/auth}
 *
 * <p>These endpoints are publicly accessible — no JWT is required to call them.
 * The {@link SecurityRequirements} annotation with an empty array overrides the
 * global Bearer security requirement defined in {@link com.company.employeemanagement.config.OpenApiConfig}.
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/auth")
@Tag(name = "Authentication", description = "User registration and JWT login endpoints")
public class AuthController {

    private final AuthService authService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param authService the authentication service
     */
    public AuthController(final AuthService authService) {
        this.authService = authService;
    }

    /**
     * Registers a new user account and returns a JWT access token.
     *
     * <p>On success the caller is immediately authenticated — no separate
     * login call is required.
     *
     * @param request the registration payload
     * @return {@code 201 Created} with the {@link AuthResponse} body
     */
    @PostMapping(value = "/register",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Register a new user account",
            description = "Creates a new user with the default ROLE_EMPLOYEE role and returns a JWT token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Account created successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> register(
            @Valid @RequestBody final RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    /**
     * Authenticates a user with email and password and returns a JWT access token.
     *
     * @param request the login payload
     * @return {@code 200 OK} with the {@link AuthResponse} body
     */
    @PostMapping(value = "/login",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Authenticate and obtain JWT",
            description = "Validates credentials and returns a signed JWT Bearer token."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Authenticated successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = AuthResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody final LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
}
