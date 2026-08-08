package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Employee} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * through the Criteria API (used for advanced search with multiple optional
 * parameters).
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>,
        JpaSpecificationExecutor<Employee> {

    /**
     * Checks whether an employee with the given code already exists.
     *
     * @param employeeCode the unique employee code to check
     * @return {@code true} if the code is already taken
     */
    boolean existsByEmployeeCode(String employeeCode);

    /**
     * Finds an employee by their unique employee code.
     *
     * @param employeeCode the employee code to search for
     * @return an {@link Optional} containing the matching {@link Employee}
     */
    Optional<Employee> findByEmployeeCode(String employeeCode);

    /**
     * Returns a paginated list of employees filtered by department UUID.
     *
     * @param departmentId the UUID of the department to filter by
     * @param pageable     pagination and sorting parameters
     * @return a page of employees belonging to the specified department
     */
    Page<Employee> findByDepartmentId(UUID departmentId, Pageable pageable);

    /**
     * Returns a paginated list of employees filtered by employment status.
     *
     * @param status   the {@link EmployeeStatus} to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of employees with the specified status
     */
    Page<Employee> findByStatus(EmployeeStatus status, Pageable pageable);

    /**
     * Full-text search across employee first name, last name, and job title
     * using a case-insensitive LIKE pattern.
     *
     * <p>Uses a LEFT JOIN so that employees without a linked user account
     * (user is optional) are still included when their {@code jobTitle}
     * matches the keyword.
     *
     * @param keyword  the search term
     * @param pageable pagination and sorting parameters
     * @return a page of matching employees
     */
    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN e.user u
            WHERE LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (u IS NOT NULL AND (
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               ))
            """)
    Page<Employee> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);
}
