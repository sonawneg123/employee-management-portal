package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateLeaveRequest;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.request.UpdateLeaveRequest;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for leave request management operations.
 *
 * <p>All methods return DTOs — entities are never exposed at this layer or above.
 *
 * @author Employee Management Portal Team
 */
public interface LeaveRequestService {

    /**
     * Returns a paginated list of leave requests, optionally filtered by
     * employee and/or status.
     *
     * @param employeeId optional UUID to filter by a specific employee
     * @param pageable   pagination and sorting parameters
     * @return a {@link PageResponse} of {@link LeaveRequestResponse} records
     */
    PageResponse<LeaveRequestResponse> findAll(UUID employeeId, Pageable pageable);

    /**
     * Returns the leave request with the specified UUID.
     *
     * @param id the UUID of the leave request
     * @return the matching {@link LeaveRequestResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no leave request with the given ID exists
     */
    LeaveRequestResponse findById(UUID id);

    /**
     * Submits a new leave request on behalf of an employee.
     *
     * @param request the creation payload
     * @return the newly created {@link LeaveRequestResponse} with the generated ID
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the referenced employee does not exist
     * @throws IllegalStateException if {@code endDate} is before {@code startDate}
     */
    LeaveRequestResponse create(CreateLeaveRequest request);

    /**
     * Updates a leave request that is still in {@code PENDING} status.
     *
     * @param id      the UUID of the leave request
     * @param request the replacement payload
     * @return the updated {@link LeaveRequestResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no leave request with the given ID exists
     * @throws IllegalStateException if the request is no longer in {@code PENDING} status
     */
    LeaveRequestResponse update(UUID id, UpdateLeaveRequest request);

    /**
     * Approves a {@code PENDING} leave request.
     *
     * @param id      the UUID of the leave request to approve
     * @param request optional reviewer notes
     * @return the updated {@link LeaveRequestResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no leave request with the given ID exists
     * @throws IllegalStateException if the request is not in {@code PENDING} status
     */
    LeaveRequestResponse approve(UUID id, ReviewLeaveRequest request);

    /**
     * Rejects a {@code PENDING} leave request.
     *
     * @param id      the UUID of the leave request to reject
     * @param request optional rejection reason
     * @return the updated {@link LeaveRequestResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no leave request with the given ID exists
     * @throws IllegalStateException if the request is not in {@code PENDING} status
     */
    LeaveRequestResponse reject(UUID id, ReviewLeaveRequest request);

    /**
     * Cancels a {@code PENDING} leave request (employee action).
     *
     * @param id the UUID of the leave request to cancel
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no leave request with the given ID exists
     * @throws IllegalStateException if the request is not in {@code PENDING} status
     */
    void cancel(UUID id);
}
