package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateDepartmentRequest;
import com.company.employeemanagement.dto.request.UpdateDepartmentRequest;
import com.company.employeemanagement.dto.response.DepartmentResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for department management operations.
 *
 * <p>All methods return DTOs — entities are never exposed at this layer or above.
 *
 * @author Employee Management Portal Team
 */
public interface DepartmentService {

    /**
     * Returns all departments as a flat, unfiltered list.
     *
     * <p>Intended for use in form dropdowns and auto-complete widgets where
     * pagination is not needed.
     *
     * @return all departments ordered by name ascending
     */
    List<DepartmentResponse> findAll();

    /**
     * Returns a paginated, optionally keyword-filtered list of departments.
     *
     * @param keyword  optional search term matched against department name and code;
     *                 pass {@code null} or empty string to return all departments
     * @param pageable pagination and sorting parameters
     * @return a {@link PageResponse} containing matching {@link DepartmentResponse} records
     */
    PageResponse<DepartmentResponse> findAllPaged(String keyword, Pageable pageable);

    /**
     * Returns the department with the specified UUID.
     *
     * @param id the UUID of the department to retrieve
     * @return the matching {@link DepartmentResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no department with the given ID exists
     */
    DepartmentResponse findById(UUID id);

    /**
     * Creates a new department.
     *
     * @param request the creation payload
     * @return the newly created {@link DepartmentResponse} with the generated ID
     * @throws com.company.employeemanagement.exception.DuplicateResourceException
     *         if the department code is already in use
     */
    DepartmentResponse create(CreateDepartmentRequest request);

    /**
     * Fully replaces the data of an existing department.
     *
     * @param id      the UUID of the department to update
     * @param request the replacement payload
     * @return the updated {@link DepartmentResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no department with the given ID exists
     * @throws com.company.employeemanagement.exception.DuplicateResourceException
     *         if the new code is already used by a different department
     */
    DepartmentResponse update(UUID id, UpdateDepartmentRequest request);

    /**
     * Deletes the department with the specified UUID.
     *
     * <p>This operation will fail at the database level if the department still
     * has employees assigned to it (FK with {@code ON DELETE RESTRICT}).
     *
     * @param id the UUID of the department to delete
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no department with the given ID exists
     */
    void delete(UUID id);
}
