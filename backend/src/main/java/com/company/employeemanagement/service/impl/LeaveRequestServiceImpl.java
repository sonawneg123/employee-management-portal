package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateLeaveRequest;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.request.UpdateLeaveRequest;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.LeaveRequestMapper;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.LeaveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of {@link LeaveRequestService} providing leave request lifecycle
 * management — submission, update, approval, rejection, and cancellation.
 *
 * <p>Resource ownership is enforced for EMPLOYEE principals:
 * <ul>
 *   <li>An EMPLOYEE can only create a leave request for themselves.</li>
 *   <li>An EMPLOYEE can only read, update, or cancel their own leave requests.</li>
 *   <li>Only ADMIN, HR, and MANAGER may approve or reject any request.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;
    private final SecurityUtils securityUtils;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param leaveRequestRepository repository for leave request persistence
     * @param employeeRepository     repository for employee lookups
     * @param leaveRequestMapper     MapStruct mapper for entity-to-DTO conversion
     * @param securityUtils          helper for current-principal inspection
     */
    public LeaveRequestServiceImpl(final LeaveRequestRepository leaveRequestRepository,
                                    final EmployeeRepository employeeRepository,
                                    final LeaveRequestMapper leaveRequestMapper,
                                    final SecurityUtils securityUtils) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestMapper = leaveRequestMapper;
        this.securityUtils = securityUtils;
    }

    /**
     * {@inheritDoc}
     *
     * <p>If the caller holds only the {@code ROLE_EMPLOYEE} role, the result is
     * automatically scoped to their own employee record, regardless of the
     * {@code employeeId} filter parameter.
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> findAll(final UUID employeeId,
                                                       final Pageable pageable) {
        Page<LeaveRequestResponse> page;

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            // Employee may only see their own leave requests
            UUID ownEmployeeId = securityUtils.getCurrentEmployee()
                    .map(Employee::getId)
                    .orElseThrow(() -> new AccessDeniedException(
                            "No employee record is linked to your account."));
            page = leaveRequestRepository.findByEmployeeId(ownEmployeeId, pageable)
                    .map(leaveRequestMapper::toResponse);
        } else if (employeeId != null) {
            page = leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                    .map(leaveRequestMapper::toResponse);
        } else {
            page = leaveRequestRepository.findAll(pageable)
                    .map(leaveRequestMapper::toResponse);
        }

        return PageResponse.from(page);
    }

    /**
     * {@inheritDoc}
     *
     * <p>An EMPLOYEE can only view their own leave requests; attempting to
     * retrieve another employee's leave request yields a 403.
     */
    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse findById(final UUID id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            requireOwnLeave(leaveRequest);
        }

        return leaveRequestMapper.toResponse(leaveRequest);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates that {@code endDate} is not before {@code startDate} before persisting.
     *
     * <p>An EMPLOYEE can only submit leave for themselves; the {@code employeeId}
     * in the request must match their own employee record.
     */
    @Override
    @Transactional
    public LeaveRequestResponse create(final CreateLeaveRequest request) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException(
                    "End date must not be before start date.");
        }

        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.employeeId()));

        // EMPLOYEE role: enforce that the request is for the caller's own record
        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            Employee ownEmployee = securityUtils.getCurrentEmployee()
                    .orElseThrow(() -> new AccessDeniedException(
                            "No employee record is linked to your account."));
            if (!ownEmployee.getId().equals(employee.getId())) {
                throw new AccessDeniedException(
                        "You may only submit leave requests for your own employee record.");
            }
        }

        LeaveRequest leaveRequest = LeaveRequest.builder()
                .employee(employee)
                .leaveType(request.leaveType())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .reason(request.reason())
                .status(LeaveStatus.PENDING)
                .build();

        LeaveRequest saved = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toResponse(saved);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Only {@code PENDING} requests may be modified. An EMPLOYEE can only
     * update their own leave request.
     */
    @Override
    @Transactional
    public LeaveRequestResponse update(final UUID id, final UpdateLeaveRequest request) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        requirePending(leaveRequest);

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            requireOwnLeave(leaveRequest);
        }

        if (request.endDate().isBefore(request.startDate())) {
            throw new IllegalArgumentException("End date must not be before start date.");
        }

        leaveRequest.setLeaveType(request.leaveType());
        leaveRequest.setStartDate(request.startDate());
        leaveRequest.setEndDate(request.endDate());
        leaveRequest.setReason(request.reason());

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toResponse(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Records the reviewer's principal name and the decision timestamp.
     * Only ADMIN, HR, and MANAGER callers may reach this method (enforced
     * by URL-level rules in {@code SecurityConfig}).
     */
    @Override
    @Transactional
    public LeaveRequestResponse approve(final UUID id, final ReviewLeaveRequest request) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        requirePending(leaveRequest);

        leaveRequest.setStatus(LeaveStatus.APPROVED);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setReviewedBy(currentUserId());

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toResponse(updated);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public LeaveRequestResponse reject(final UUID id, final ReviewLeaveRequest request) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        requirePending(leaveRequest);

        leaveRequest.setStatus(LeaveStatus.REJECTED);
        leaveRequest.setReviewedAt(LocalDateTime.now());
        leaveRequest.setReviewedBy(currentUserId());

        LeaveRequest updated = leaveRequestRepository.save(leaveRequest);
        return leaveRequestMapper.toResponse(updated);
    }

    /**
     * {@inheritDoc}
     *
     * <p>An EMPLOYEE can only cancel their own PENDING leave request.
     */
    @Override
    @Transactional
    public void cancel(final UUID id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        requirePending(leaveRequest);

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            requireOwnLeave(leaveRequest);
        }

        leaveRequest.setStatus(LeaveStatus.CANCELLED);
        leaveRequestRepository.save(leaveRequest);
    }

    // ───────────────────────── private helpers ─────────────────────────────────

    /**
     * Asserts that the given leave request is still {@code PENDING}.
     *
     * @param leaveRequest the request to check
     * @throws IllegalStateException if the request has already been reviewed or cancelled
     */
    private void requirePending(final LeaveRequest leaveRequest) {
        if (leaveRequest.getStatus() != LeaveStatus.PENDING) {
            throw new IllegalStateException(
                    "Leave request " + leaveRequest.getId() + " is not in PENDING status. "
                    + "Current status: " + leaveRequest.getStatus());
        }
    }

    /**
     * Asserts that the given leave request belongs to the currently authenticated
     * EMPLOYEE principal.
     *
     * @param leaveRequest the leave request to check
     * @throws AccessDeniedException if the request belongs to a different employee
     */
    private void requireOwnLeave(final LeaveRequest leaveRequest) {
        Employee ownEmployee = securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));
        if (!ownEmployee.getId().equals(leaveRequest.getEmployee().getId())) {
            throw new AccessDeniedException(
                    "You may only access your own leave requests.");
        }
    }

    /**
     * Returns the UUID of the currently authenticated user from the Security context,
     * or {@code null} if the context does not carry a UUID principal.
     *
     * @return reviewer UUID, or {@code null}
     */
    private UUID currentUserId() {
        return securityUtils.getCurrentUserId().orElse(null);
    }
}
