package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.ForgotPasswordRequest;
import com.company.employeemanagement.dto.request.LoginRequest;
import com.company.employeemanagement.dto.request.RegisterRequest;
import com.company.employeemanagement.dto.request.ResetPasswordRequest;
import com.company.employeemanagement.dto.request.VerifyOtpRequest;
import com.company.employeemanagement.dto.response.AuthResponse;
import com.company.employeemanagement.dto.response.MessageResponse;
import com.company.employeemanagement.service.AuthService;
import com.company.employeemanagement.service.PasswordResetService;
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
 * REST controller exposing authentication endpoints for user registration, login,
 * and password reset via OTP.
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
@Tag(name = "Authentication", description = "User registration, JWT login, and password-reset endpoints")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;

    /**
     * Constructs the controller with its required service dependencies.
     *
     * @param authService          the authentication service
     * @param passwordResetService the password-reset OTP service
     */
    public AuthController(final AuthService authService,
                          final PasswordResetService passwordResetService) {
        this.authService = authService;
        this.passwordResetService = passwordResetService;
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

    // ── Password Reset (OTP Flow) ──────────────────────────────────────────────

    /**
     * Initiates the password-reset flow by generating and emailing an OTP.
     *
     * <p>Always returns the same generic response regardless of whether the
     * email is registered, to prevent email-existence enumeration.
     *
     * @param request the request carrying the email address
     * @return {@code 200 OK} with a generic {@link MessageResponse}
     */
    @PostMapping(value = "/forgot-password",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Request a password-reset OTP",
            description = "Generates a 6-digit OTP and emails it to the address if it is registered. "
                    + "Returns the same generic response regardless of whether the email exists."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Generic response (OTP sent if email exists)",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody final ForgotPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.requestPasswordReset(request));
    }

    /**
     * Verifies the OTP submitted by the user.
     *
     * @param request the email + OTP pair
     * @return {@code 200 OK} on success
     */
    @PostMapping(value = "/verify-otp",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Verify the password-reset OTP",
            description = "Checks the OTP against the stored hash. On success the token is marked as verified."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "OTP verified successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "OTP incorrect, expired, or max attempts exceeded",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MessageResponse> verifyOtp(
            @Valid @RequestBody final VerifyOtpRequest request) {
        return ResponseEntity.ok(passwordResetService.verifyOtp(request));
    }

    /**
     * Resets the user's password after a verified OTP.
     *
     * @param request the email + new password + confirmation
     * @return {@code 200 OK} on success
     */
    @PostMapping(value = "/reset-password",
                 consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @SecurityRequirements
    @Operation(
            summary = "Reset password using a verified OTP token",
            description = "Sets a new BCrypt-hashed password after OTP verification. "
                    + "The verified token is consumed and cannot be reused."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Password reset successfully",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed or token invalid",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody final ResetPasswordRequest request) {
        return ResponseEntity.ok(passwordResetService.resetPassword(request));
    }
}
