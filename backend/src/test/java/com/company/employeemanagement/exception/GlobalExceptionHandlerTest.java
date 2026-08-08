package com.company.employeemanagement.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link GlobalExceptionHandler}.
 *
 * <p>Each handler method is invoked directly on the handler instance.
 * Spring MVC wiring is verified in the integration test suite.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("GlobalExceptionHandler")
class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;
    private WebRequest              webRequest;

    @BeforeEach
    void setUp() {
        handler    = new GlobalExceptionHandler();
        webRequest = new ServletWebRequest(new MockHttpServletRequest("GET", "/employees"));
    }

    // ──────────────────────────────────────────────────────────────────────────
    // ResourceNotFoundException → 404
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("ResourceNotFoundException")
    class HandleResourceNotFound {

        @Test
        @DisplayName("returns 404 status")
        void returns404() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Employee", UUID.randomUUID());
            ProblemDetail problem = handler.handleResourceNotFound(ex, webRequest);
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        }

        @Test
        @DisplayName("title is 'Resource Not Found'")
        void hasCorrectTitle() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Department", "code", "ENG");
            ProblemDetail problem = handler.handleResourceNotFound(ex, webRequest);
            assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
        }

        @Test
        @DisplayName("detail contains the resource name")
        void detailContainsResourceName() {
            ResourceNotFoundException ex = new ResourceNotFoundException(
                    "Employee", "email", "john@example.com");
            ProblemDetail problem = handler.handleResourceNotFound(ex, webRequest);
            assertThat(problem.getDetail()).contains("Employee");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DuplicateResourceException → 409
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DuplicateResourceException")
    class HandleDuplicateResource {

        @Test
        @DisplayName("returns 409 status")
        void returns409() {
            DuplicateResourceException ex = new DuplicateResourceException(
                    "User", "email", "dupe@example.com");
            ProblemDetail problem = handler.handleDuplicateResource(ex, webRequest);
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        }

        @Test
        @DisplayName("title is 'Duplicate Resource'")
        void hasCorrectTitle() {
            DuplicateResourceException ex = new DuplicateResourceException(
                    "Employee", "employeeCode", "EMP-001");
            ProblemDetail problem = handler.handleDuplicateResource(ex, webRequest);
            assertThat(problem.getTitle()).isEqualTo("Duplicate Resource");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // BadCredentialsException → 401
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("BadCredentialsException")
    class HandleBadCredentials {

        @Test
        @DisplayName("returns 401 status")
        void returns401() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");
            ProblemDetail problem = handler.handleBadCredentials(ex, webRequest);
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        }

        @Test
        @DisplayName("detail obscures actual error (does not leak credentials)")
        void detailIsGeneric() {
            BadCredentialsException ex = new BadCredentialsException("Bad credentials");
            ProblemDetail problem = handler.handleBadCredentials(ex, webRequest);
            assertThat(problem.getDetail()).isEqualTo("Invalid email or password.");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DisabledException → 401
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DisabledException")
    class HandleDisabled {

        @Test
        @DisplayName("returns 401 with 'Account Disabled' title")
        void returns401WithCorrectTitle() {
            DisabledException ex = new DisabledException("User is disabled");
            ProblemDetail problem = handler.handleDisabled(ex, webRequest);
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problem.getTitle()).isEqualTo("Account Disabled");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // LockedException → 401
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("LockedException")
    class HandleLocked {

        @Test
        @DisplayName("returns 401 with 'Account Locked' title")
        void returns401WithCorrectTitle() {
            LockedException ex = new LockedException("Account locked");
            ProblemDetail problem = handler.handleLocked(ex, webRequest);
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
            assertThat(problem.getTitle()).isEqualTo("Account Locked");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // MethodArgumentNotValidException → 400
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("MethodArgumentNotValidException")
    class HandleValidation {

        @Test
        @DisplayName("returns 400 with violations map")
        void returns400WithViolations() {
            FieldError fieldError = new FieldError("registerRequest", "email",
                    "Email must be a valid email address");

            BindingResult bindingResult = mock(BindingResult.class);
            when(bindingResult.getFieldErrors()).thenReturn(List.of(fieldError));

            MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
            when(ex.getBindingResult()).thenReturn(bindingResult);

            ProblemDetail problem = handler.handleValidation(ex, webRequest);

            assertThat(problem.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
            assertThat(problem.getTitle()).isEqualTo("Validation Failed");
            assertThat(problem.getProperties())
                    .containsKey("violations");
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Generic Exception → 500
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Generic Exception")
    class HandleGeneral {

        @Test
        @DisplayName("returns 500 for unhandled exceptions")
        void returns500() {
            Exception ex = new RuntimeException("Something unexpected");
            ProblemDetail problem = handler.handleGeneral(ex, webRequest);
            assertThat(problem.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
            assertThat(problem.getTitle()).isEqualTo("Internal Server Error");
        }

        @Test
        @DisplayName("does not leak internal exception message in detail")
        void doesNotLeakMessage() {
            Exception ex = new RuntimeException("Database connection pool exhausted");
            ProblemDetail problem = handler.handleGeneral(ex, webRequest);
            assertThat(problem.getDetail()).doesNotContain("Database connection pool exhausted");
        }
    }
}
