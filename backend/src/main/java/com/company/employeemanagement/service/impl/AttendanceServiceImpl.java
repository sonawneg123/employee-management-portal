package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateAttendanceRequest;
import com.company.employeemanagement.dto.request.UpdateAttendanceRequest;
import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.AttendanceMapper;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.AttendanceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AttendanceService} providing attendance record
 * listing, retrieval, creation, and update operations.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AttendanceServiceImpl implements AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceMapper attendanceMapper;
    private final SecurityUtils securityUtils;

    /**
     * Constructs the service with all required dependencies.
     *
     * @param attendanceRepository repository for attendance persistence
     * @param employeeRepository   repository for employee lookups
     * @param attendanceMapper     MapStruct mapper for entity-to-DTO conversion
     * @param securityUtils        helper for current-principal inspection
     */
    public AttendanceServiceImpl(final AttendanceRepository attendanceRepository,
                                  final EmployeeRepository employeeRepository,
                                  final AttendanceMapper attendanceMapper,
                                  final SecurityUtils securityUtils) {
        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceMapper = attendanceMapper;
        this.securityUtils = securityUtils;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> findAll(final UUID employeeId,
                                                     final LocalDate date,
                                                     final AttendanceStatus status,
                                                     final Pageable pageable) {
        Page<Attendance> page = attendanceRepository.findByFilters(employeeId, date, status, pageable);
        List<AttendanceResponse> content = page.getContent().stream()
                .map(attendanceMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.from(new PageImpl<>(content, pageable, page.getTotalElements()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>An EMPLOYEE principal can only view their own attendance records.
     */
    @Override
    @Transactional(readOnly = true)
    public AttendanceResponse findById(final UUID id) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", id));

        if (securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged()) {
            Employee ownEmployee = securityUtils.getCurrentEmployee()
                    .orElseThrow(() -> new AccessDeniedException(
                            "No employee record is linked to your account."));
            if (!ownEmployee.getId().equals(attendance.getEmployee().getId())) {
                throw new AccessDeniedException(
                        "You may only access your own attendance records.");
            }
        }

        return attendanceMapper.toResponse(attendance);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<AttendanceResponse> findMyAttendance(final LocalDate date,
                                                              final AttendanceStatus status,
                                                              final Pageable pageable) {
        // If the authenticated user has no linked employee record yet (e.g. newly
        // registered account not yet associated by HR), return an empty page instead
        // of 403 so the self-service page renders gracefully.
        java.util.Optional<UUID> maybeId = securityUtils.getCurrentEmployee()
                .map(Employee::getId);
        if (maybeId.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0L));
        }
        Page<Attendance> page = attendanceRepository.findByFilters(maybeId.get(), date, status, pageable);
        List<AttendanceResponse> content = page.getContent().stream()
                .map(attendanceMapper::toResponse)
                .collect(Collectors.toList());
        return PageResponse.from(new PageImpl<>(content, pageable, page.getTotalElements()));
    }

    /**
     * {@inheritDoc}
     *
     * <p>The employee is resolved from the authenticated principal — the caller
     * cannot pass an employee ID.
     */
    @Override
    @Transactional
    public AttendanceResponse checkIn() {
        Employee employee = securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account. Contact HR."));

        LocalDate today = LocalDate.now();

        attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "You have already checked in for today (" + today + ").");
                });

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .attendanceDate(today)
                .checkInTime(java.time.LocalTime.now())
                .status(AttendanceStatus.PRESENT)
                .build();

        return attendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AttendanceResponse checkOut() {
        Employee employee = securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account. Contact HR."));

        LocalDate today = LocalDate.now();

        Attendance attendance = attendanceRepository.findByEmployeeIdAndAttendanceDate(employee.getId(), today)
                .orElseThrow(() -> new com.company.employeemanagement.exception.ResourceNotFoundException(
                        "Attendance", "date", today.toString()));

        if (attendance.getCheckOutTime() != null) {
            throw new IllegalStateException("You have already checked out for today (" + today + ").");
        }

        attendance.setCheckOutTime(java.time.LocalTime.now());
        return attendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AttendanceResponse create(final CreateAttendanceRequest request) {
        Employee employee = employeeRepository.findById(request.employeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.employeeId()));

        attendanceRepository.findByEmployeeIdAndAttendanceDate(
                request.employeeId(), request.attendanceDate())
                .ifPresent(existing -> {
                    throw new IllegalStateException(
                            "An attendance record already exists for employee "
                            + request.employeeId() + " on " + request.attendanceDate());
                });

        Attendance attendance = Attendance.builder()
                .employee(employee)
                .attendanceDate(request.attendanceDate())
                .checkInTime(request.checkInTime())
                .checkOutTime(request.checkOutTime())
                .status(request.status())
                .notes(request.notes())
                .build();

        return attendanceMapper.toResponse(attendanceRepository.save(attendance));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public AttendanceResponse update(final UUID id, final UpdateAttendanceRequest request) {
        Attendance attendance = attendanceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Attendance", id));

        attendance.setCheckInTime(request.checkInTime());
        attendance.setCheckOutTime(request.checkOutTime());
        attendance.setStatus(request.status());
        attendance.setNotes(request.notes());

        return attendanceMapper.toResponse(attendanceRepository.save(attendance));
    }
}
