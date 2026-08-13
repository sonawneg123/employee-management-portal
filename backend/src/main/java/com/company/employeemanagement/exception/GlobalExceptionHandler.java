package com.company.employeemanagement.exception;

import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Centralised exception handler that translates application exceptions into
 * RFC 7807 {@link ProblemDetail} responses.
 *
 * <p>Every handled exception produces a JSON body with the standardised
 * {@code type}, {@code title}, {@code status}, {@code detail}, and
 * {@code instance} fields, plus any domain-specific extension properties.
 *
 * <p>IMPORTANT: Handlers return {@code ResponseEntity<ProblemDetail>} rather than
 * a bare {@code ProblemDetail}. When returning bare {@code ProblemDetail} from a
 * {@code @RestControllerAdvice} in Spring 6.x / Boot 3.x the framework does NOT
 * automatically propagate the status value stored inside {@code ProblemDetail} to
 * the HTTP response status line — the response would come back as {@code 200 OK}
 * while the body contains the real status number. Wrapping in {@code ResponseEntity}
 * is the correct and explicit approach.
 *
 * <p>Unhandled exceptions fall through to Spring Boot's default
 * {@code BasicErrorController} which also returns {@link ProblemDetail}.
 *
 * @author Employee Management Portal Team
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * Handles {@link ResourceNotFoundException} — resource not found (404).
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(final ResourceNotFoundException ex,
                                                                 final WebRequest request) {
        log.warn("Resource not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Resource Not Found");
        problem.setType(URI.create("https://company.com/errors/resource-not-found"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(problem);
    }

    /**
     * Handles {@link DuplicateResourceException} — conflict (409).
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> handleDuplicateResource(final DuplicateResourceException ex,
                                                                  final WebRequest request) {
        log.warn("Duplicate resource: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Duplicate Resource");
        problem.setType(URI.create("https://company.com/errors/duplicate-resource"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Handles application-level {@link AccessDeniedException} — forbidden (403).
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(final AccessDeniedException ex,
                                                             final WebRequest request) {
        log.warn("Access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://company.com/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Handles Spring Security's {@link org.springframework.security.access.AccessDeniedException}
     * — forbidden (403).
     */
    @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleSpringAccessDenied(
            final org.springframework.security.access.AccessDeniedException ex,
            final WebRequest request) {
        log.warn("Spring Security access denied: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.FORBIDDEN, "You do not have permission to perform this action.");
        problem.setTitle("Access Denied");
        problem.setType(URI.create("https://company.com/errors/access-denied"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(problem);
    }

    /**
     * Handles {@link BadCredentialsException} — invalid credentials (401).
     *
     * <p>Returns a generic "Invalid email or password." message regardless of
     * whether the email exists, to prevent user enumeration.
     */
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(final BadCredentialsException ex,
                                                               final WebRequest request) {
        log.warn("Authentication failure: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        problem.setTitle("Authentication Failed");
        problem.setType(URI.create("https://company.com/errors/authentication-failed"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Handles {@link DisabledException} — account disabled (401).
     */
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ProblemDetail> handleDisabled(final DisabledException ex,
                                                         final WebRequest request) {
        log.warn("Account disabled: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "This account has been disabled.");
        problem.setTitle("Account Disabled");
        problem.setType(URI.create("https://company.com/errors/account-disabled"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Handles {@link LockedException} — account locked (401).
     */
    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ProblemDetail> handleLocked(final LockedException ex,
                                                       final WebRequest request) {
        log.warn("Account locked: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "This account has been locked.");
        problem.setTitle("Account Locked");
        problem.setType(URI.create("https://company.com/errors/account-locked"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problem);
    }

    /**
     * Handles {@link MethodArgumentNotValidException} — Bean Validation failures (400).
     *
     * <p>Returns a {@link ProblemDetail} augmented with a {@code violations} map
     * whose keys are field names and values are the first validation message
     * for that field.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(final MethodArgumentNotValidException ex,
                                                           final WebRequest request) {
        Map<String, String> violations = ex.getBindingResult().getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (first, second) -> first
                ));
        log.debug("Validation failed: {}", violations);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more fields failed validation.");
        problem.setTitle("Validation Failed");
        problem.setType(URI.create("https://company.com/errors/validation-failed"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        problem.setProperty("violations", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles {@link ConstraintViolationException} — constraint violations from
     * path variables and request parameters (400).
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDetail> handleConstraintViolation(final ConstraintViolationException ex,
                                                                    final WebRequest request) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        cv -> cv.getPropertyPath().toString(),
                        cv -> cv.getMessage(),
                        (first, second) -> first
                ));
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "One or more parameters failed validation.");
        problem.setTitle("Constraint Violation");
        problem.setType(URI.create("https://company.com/errors/constraint-violation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        problem.setProperty("violations", violations);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Handles {@link IllegalStateException} — business rule violations (409 Conflict).
     *
     * <p>Used when an operation cannot proceed due to the current state of the resource
     * (e.g., trying to approve a leave request that is not PENDING).
     */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ProblemDetail> handleIllegalState(final IllegalStateException ex,
                                                             final WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Invalid Operation");
        problem.setType(URI.create("https://company.com/errors/invalid-operation"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.CONFLICT).body(problem);
    }

    /**
     * Handles {@link IllegalArgumentException} — invalid input not caught by Bean Validation (400).
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleIllegalArgument(final IllegalArgumentException ex,
                                                                final WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Input");
        problem.setType(URI.create("https://company.com/errors/invalid-input"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(problem);
    }

    /**
     * Catch-all handler for any unrecognised runtime exceptions (500).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleGeneral(final Exception ex,
                                                        final WebRequest request) {
        log.error("Unhandled exception: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "An unexpected error occurred. Please try again later.");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://company.com/errors/internal-error"));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("path", extractPath(request));
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(problem);
    }

    /**
     * Extracts the request URI path from a {@link WebRequest} description string.
     */
    private String extractPath(final WebRequest request) {
        String description = request.getDescription(false);
        return description.replace("uri=", "");
    }
}
