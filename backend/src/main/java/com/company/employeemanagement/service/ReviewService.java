package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateReviewRequest;
import com.company.employeemanagement.dto.request.UpdateReviewRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.ReviewResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for performance review operations.
 *
 * @author Employee Management Portal Team
 */
public interface ReviewService {

    /**
     * Returns a paginated list of reviews, optionally filtered by employee.
     *
     * <p>ADMIN, HR, and MANAGER may query all reviews or filter by employee.
     * EMPLOYEE may only retrieve reviews for their own employee record.
     *
     * @param employeeId optional UUID to filter by a specific employee; {@code null} = all
     * @param pageable   pagination and sorting
     * @return a page of {@link ReviewResponse} records
     */
    PageResponse<ReviewResponse> findAll(UUID employeeId, Pageable pageable);

    /**
     * Returns a single review by its UUID.
     *
     * <p>EMPLOYEE may only access reviews belonging to their own employee record.
     *
     * @param id the UUID of the review
     * @return the matching {@link ReviewResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no review with that ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if an EMPLOYEE attempts to access another employee's review
     */
    ReviewResponse findById(UUID id);

    /**
     * Creates a new performance review.
     *
     * <p>Only ADMIN, HR, and MANAGER may create reviews.
     *
     * @param request the creation payload
     * @return the newly created {@link ReviewResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the referenced employee does not exist
     */
    ReviewResponse create(CreateReviewRequest request);

    /**
     * Updates an existing performance review.
     *
     * <p>Only ADMIN, HR, and MANAGER may update reviews.
     *
     * @param id      the UUID of the review to update
     * @param request the replacement payload
     * @return the updated {@link ReviewResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no review with that ID exists
     */
    ReviewResponse update(UUID id, UpdateReviewRequest request);

    /**
     * Deletes a performance review.
     *
     * <p>Only ADMIN may delete reviews.
     *
     * @param id the UUID of the review to delete
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no review with that ID exists
     */
    void delete(UUID id);
}
