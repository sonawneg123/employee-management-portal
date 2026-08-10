package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.UpdateEmployeeRequest;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

/**
 * Service contract for employee management operations.
 *
 * <p>All methods return DTOs — entities are never exposed at this layer
 * or above.
 *
 * @author Employee Management Portal Team
 */
public interface EmployeeService {

    /**
     * Returns a paginated, optionally filtered list of employees.
     *
     * @param keyword      optional search term matched against name and job title;
     *                     pass {@code null} or empty string to return all employees
     * @param departmentId optional UUID to filter by department
     * @param status       optional {@link EmployeeStatus} to filter by
     * @param pageable     pagination and sorting parameters
     * @return a {@link PageResponse} containing matching {@link EmployeeResponse} records
     */
    PageResponse<EmployeeResponse> findAll(String keyword, UUID departmentId,
                                           EmployeeStatus status, Pageable pageable);

    /**
     * Returns the employee with the specified UUID.
     *
     * @param id the UUID of the employee to retrieve
     * @return the matching {@link EmployeeResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no employee with the given ID exists
     */
    EmployeeResponse findById(UUID id);

    /**
     * Creates a new employee record.
     *
     * @param request the creation payload
     * @return the newly created {@link EmployeeResponse} with the generated ID
     * @throws com.company.employeemanagement.exception.DuplicateResourceException
     *         if the employee code is already in use
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the referenced department does not exist
     */
    EmployeeResponse create(CreateEmployeeRequest request);

    /**
     * Fully replaces the data of an existing employee record.
     *
     * @param id      the UUID of the employee to update
     * @param request the replacement payload
     * @return the updated {@link EmployeeResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no employee with the given ID exists, or if the referenced
     *         department does not exist
     */
    EmployeeResponse update(UUID id, UpdateEmployeeRequest request);

    /**
     * Deletes the employee with the specified UUID.
     *
     * @param id the UUID of the employee to delete
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no employee with the given ID exists
     */
    void delete(UUID id);
}
