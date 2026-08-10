package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateAttendanceRequest;
import com.company.employeemanagement.dto.request.UpdateAttendanceRequest;
import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service contract for attendance record management.
 *
 * <p>All methods return DTOs — entities are never exposed at this layer or above.
 *
 * @author Employee Management Portal Team
 */
public interface AttendanceService {

    /**
     * Returns a paginated list of attendance records, optionally filtered by
     * employee, date, and/or status.
     *
     * @param employeeId optional UUID to filter by employee
     * @param date       optional date to filter by
     * @param status     optional {@link AttendanceStatus} to filter by
     * @param pageable   pagination and sorting parameters
     * @return a {@link PageResponse} of {@link AttendanceResponse} records
     */
    PageResponse<AttendanceResponse> findAll(UUID employeeId, LocalDate date,
                                             AttendanceStatus status, Pageable pageable);

    /**
     * Returns the attendance record with the specified UUID.
     *
     * @param id the UUID of the attendance record
     * @return the matching {@link AttendanceResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no record with the given ID exists
     */
    AttendanceResponse findById(UUID id);

    /**
     * Returns the authenticated employee's own attendance records.
     *
     * @param date     optional date filter
     * @param status   optional status filter
     * @param pageable pagination and sorting parameters
     * @return a {@link PageResponse} of the caller's attendance records
     */
    PageResponse<AttendanceResponse> findMyAttendance(LocalDate date,
                                                       AttendanceStatus status,
                                                       Pageable pageable);

    /**
     * Creates a new attendance record (HR / Admin manual marking).
     *
     * @param request the creation payload
     * @return the newly created {@link AttendanceResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the referenced employee does not exist
     * @throws IllegalStateException if an attendance record already exists for
     *         the employee on the given date
     */
    AttendanceResponse create(CreateAttendanceRequest request);

    /**
     * Updates an existing attendance record.
     *
     * @param id      the UUID of the record to update
     * @param request the replacement payload
     * @return the updated {@link AttendanceResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no record with the given ID exists
     */
    AttendanceResponse update(UUID id, UpdateAttendanceRequest request);
}
