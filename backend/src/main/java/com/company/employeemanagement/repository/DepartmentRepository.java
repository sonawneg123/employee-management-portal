package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Department} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, UUID> {

    /**
     * Finds a department by its unique short code.
     *
     * @param code the department code (e.g., {@code "ENG"})
     * @return an {@link Optional} containing the matching {@link Department},
     *         or empty if not found
     */
    Optional<Department> findByCode(String code);

    /**
     * Checks whether a department with the given code already exists.
     *
     * @param code the department code to check
     * @return {@code true} if a department with that code exists
     */
    boolean existsByCode(String code);

    /**
     * Returns the number of employees currently assigned to the given department.
     *
     * <p>Executed as a single {@code SELECT COUNT(*)} scalar query — avoids
     * loading the full {@code employees} collection just to obtain the count.
     *
     * @param departmentId the UUID of the department
     * @return the number of employees in the department
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.department.id = :departmentId")
    long countEmployeesByDepartmentId(@Param("departmentId") UUID departmentId);

    /**
     * Case-insensitive search across department name and code.
     *
     * @param keyword  the search term
     * @param pageable pagination and sorting parameters
     * @return a page of departments whose name or code contains the keyword
     */
    @Query("""
            SELECT d FROM Department d
            WHERE LOWER(d.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(d.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    Page<Department> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ── Dashboard aggregation queries ─────────────────────────────────────────

    /**
     * Returns department name, code, and employee count as a projection list
     * for the dashboard pie chart. Each element is an {@code Object[]} with:
     * <ul>
     *   <li>{@code [0]} — department name ({@code String})</li>
     *   <li>{@code [1]} — department code ({@code String})</li>
     *   <li>{@code [2]} — employee headcount ({@code Long})</li>
     * </ul>
     *
     * <p>Departments with zero employees are included.
     *
     * @return list of {@code [name, code, count]} rows ordered by count descending
     */
    @Query("""
            SELECT d.name, d.code, COUNT(e)
            FROM Department d
            LEFT JOIN d.employees e
            GROUP BY d.id, d.name, d.code
            ORDER BY COUNT(e) DESC
            """)
    List<Object[]> findDepartmentDistribution();
}
