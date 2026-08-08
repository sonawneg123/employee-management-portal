package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateLeaveRequest;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.request.UpdateLeaveRequest;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.LeaveRequestMapper;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import com.company.employeemanagement.service.LeaveRequestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Implementation of {@link LeaveRequestService} providing leave request lifecycle
 * management — submission, update, approval, rejection, and cancellation.
 *
 * @author Employee Management Portal Team
 */
@Service
public class LeaveRequestServiceImpl implements LeaveRequestService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final LeaveRequestMapper leaveRequestMapper;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param leaveRequestRepository repository for leave request persistence
     * @param employeeRepository     repository for employee lookups
     * @param leaveRequestMapper     MapStruct mapper for entity-to-DTO conversion
     */
    public LeaveRequestServiceImpl(final LeaveRequestRepository leaveRequestRepository,
                                    final EmployeeRepository employeeRepository,
                                    final LeaveRequestMapper leaveRequestMapper) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.leaveRequestMapper = leaveRequestMapper;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<LeaveRequestResponse> findAll(final UUID employeeId,
                                                       final Pageable pageable) {
        Page<LeaveRequestResponse> page;
        if (employeeId != null) {
            page = leaveRequestRepository.findByEmployeeId(employeeId, pageable)
                    .map(leaveRequestMapper::toResponse);
        } else {
            page = leaveRequestRepository.findAll(pageable)
                    .map(leaveRequestMapper::toResponse);
        }
        return PageResponse.from(page);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public LeaveRequestResponse findById(final UUID id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));
        return leaveRequestMapper.toResponse(leaveRequest);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Validates that {@code endDate} is not before {@code startDate} before persisting.
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
     * <p>Only {@code PENDING} requests may be modified by the submitting employee.
     */
    @Override
    @Transactional
    public LeaveRequestResponse update(final UUID id, final UpdateLeaveRequest request) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        requirePending(leaveRequest);

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

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void cancel(final UUID id) {
        LeaveRequest leaveRequest = leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("LeaveRequest", id));

        requirePending(leaveRequest);

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
     * Returns the UUID of the currently authenticated user from the Security context,
     * or {@code null} if the context does not carry a UUID principal.
     *
     * @return reviewer UUID, or {@code null}
     */
    private UUID currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return null;
        }
        try {
            return UUID.fromString(auth.getName());
        } catch (IllegalArgumentException ex) {
            // Principal is an email/username, not a UUID — reviewer tracking is best-effort
            return null;
        }
    }
}
