package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateReviewRequest;
import com.company.employeemanagement.dto.request.UpdateReviewRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.ReviewResponse;
import com.company.employeemanagement.service.ReviewService;
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
 * REST controller exposing performance review endpoints.
 *
 * <p>Base path: {@code /api/reviews}
 *
 * <p>Authorization summary:
 * <ul>
 *   <li>GET (list, detail) — ADMIN, HR, MANAGER, EMPLOYEE (employees see own only)</li>
 *   <li>POST, PUT — ADMIN, HR, MANAGER</li>
 *   <li>DELETE — ADMIN only</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/reviews")
@Tag(name = "Reviews", description = "Performance review management")
@SecurityRequirement(name = "BearerAuth")
public class ReviewController {

    private final ReviewService reviewService;

    /**
     * Constructs the controller with its required service dependency.
     *
     * @param reviewService the performance review service
     */
    public ReviewController(final ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    /**
     * Returns a paginated list of performance reviews.
     *
     * @param employeeId optional filter by employee UUID
     * @param page       zero-based page number (default: 0)
     * @param size       page size (default: 20)
     * @param sortBy     sort field (default: {@code "reviewDate"})
     * @param sortDir    sort direction: {@code asc} or {@code desc} (default: {@code "desc"})
     * @return paginated {@link PageResponse} of {@link ReviewResponse}
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "List performance reviews",
               description = "Returns a paginated list. EMPLOYEE sees only their own reviews.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reviews returned",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = PageResponse.class))),
            @ApiResponse(responseCode = "401", description = "Not authenticated",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<PageResponse<ReviewResponse>> findAll(
            @Parameter(description = "Filter by employee UUID")
            @RequestParam(required = false) final UUID employeeId,
            @RequestParam(defaultValue = "0")  final int page,
            @RequestParam(defaultValue = "20") final int size,
            @RequestParam(defaultValue = "reviewDate") final String sortBy,
            @RequestParam(defaultValue = "desc") final String sortDir) {

        Sort sort = "asc".equalsIgnoreCase(sortDir)
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return ResponseEntity.ok(reviewService.findAll(employeeId, pageable));
    }

    /**
     * Returns a single performance review by UUID.
     *
     * @param id UUID of the review
     * @return the matching {@link ReviewResponse}
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE')")
    @Operation(summary = "Get review by ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review found",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ReviewResponse> findById(
            @PathVariable final UUID id) {
        return ResponseEntity.ok(reviewService.findById(id));
    }

    /**
     * Creates a new performance review.
     *
     * <p>Accessible by ADMIN, HR, MANAGER.
     *
     * @param request the creation payload
     * @return {@code 201 Created} with the new review
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE,
                 produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Create a performance review")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Review created",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Employee not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ReviewResponse> create(
            @Valid @RequestBody final CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(request));
    }

    /**
     * Updates an existing performance review.
     *
     * <p>Accessible by ADMIN, HR, MANAGER.
     *
     * @param id      UUID of the review to update
     * @param request the replacement payload
     * @return the updated {@link ReviewResponse}
     */
    @PutMapping(value = "/{id}",
                consumes = MediaType.APPLICATION_JSON_VALUE,
                produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    @Operation(summary = "Update a performance review")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Review updated",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = ReviewResponse.class))),
            @ApiResponse(responseCode = "400", description = "Validation failed",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ReviewResponse> update(
            @PathVariable final UUID id,
            @Valid @RequestBody final UpdateReviewRequest request) {
        return ResponseEntity.ok(reviewService.update(id, request));
    }

    /**
     * Deletes a performance review.
     *
     * <p>Accessible by ADMIN only.
     *
     * @param id UUID of the review to delete
     * @return {@code 204 No Content}
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete a performance review")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Review deleted"),
            @ApiResponse(responseCode = "404", description = "Review not found",
                    content = @Content(mediaType = "application/problem+json",
                            schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable final UUID id) {
        reviewService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
