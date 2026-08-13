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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Spring Data JPA repository for {@link Employee} entities.
 *
 * <p>Extends {@link JpaSpecificationExecutor} to support dynamic filtering
 * through the Criteria API (used for advanced search with multiple optional
 * parameters).
 *
 * <p>List-oriented queries ({@code findAllWithAssociations},
 * {@code searchByKeyword}) use {@code JOIN FETCH} to load the mandatory
 * {@code department} and optional {@code user} associations in a single
 * database round-trip, preventing the N+1 select problem that would otherwise
 * occur when the mapper accesses those lazily-loaded associations.
 *
 * <p>Because Hibernate cannot apply SQL-level {@code LIMIT}/{@code OFFSET}
 * pagination when a {@code JOIN FETCH} is present on a collection relationship,
 * these queries use a two-step approach:
 * <ol>
 *   <li>A scalar ID query with {@code Pageable} to retrieve the page of IDs
 *       efficiently at the database level.</li>
 *   <li>A fetch query that loads full entities by those IDs in a single
 *       {@code IN (…)} statement.</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, UUID>,
        JpaSpecificationExecutor<Employee> {

    /**
     * Finds the employee record linked to the given portal user ID.
     *
     * <p>Used for ownership checks — an EMPLOYEE user can only access
     * the employee record that is linked to their own {@link com.company.employeemanagement.entity.User}.
     *
     * @param userId the UUID of the linked user account
     * @return an {@link Optional} containing the matching {@link Employee}, or empty if none exists
     */
    Optional<Employee> findByUserId(UUID userId);

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

    // ── Scalar ID queries (pagination happens here) ───────────────────────────

    /**
     * Returns a page of employee IDs for all employees — used as the first step
     * of the two-query pagination strategy in
     * {@link com.company.employeemanagement.service.impl.EmployeeServiceImpl}.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of UUID primary keys
     */
    @Query("SELECT e.id FROM Employee e")
    Page<UUID> findAllIds(Pageable pageable);

    /**
     * Returns a page of employee IDs with optional keyword, departmentId, and status filters.
     * Null parameters are treated as "no filter" (wildcard).
     *
     * @param keyword      optional search term; pass {@code null} to skip keyword filtering
     * @param departmentId optional department UUID filter; pass {@code null} to skip
     * @param status       optional {@link EmployeeStatus} filter; pass {@code null} to skip
     * @param pageable     pagination and sorting parameters
     * @return a page of matching employee UUIDs
     */
    @Query("""
            SELECT e.id FROM Employee e
            LEFT JOIN e.user u
            WHERE (:keyword      IS NULL
                   OR LOWER(e.jobTitle)   LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR (e.firstName IS NOT NULL AND LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')))
                   OR (e.lastName  IS NOT NULL AND LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%')))
                   OR (u IS NOT NULL AND (
                       LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
                   )))
              AND (:departmentId IS NULL OR e.department.id = :departmentId)
              AND (:status       IS NULL OR e.status        = :status)
            """)
    Page<UUID> findIdsByFilters(
            @Param("keyword")      String keyword,
            @Param("departmentId") UUID departmentId,
            @Param("status")       EmployeeStatus status,
            Pageable pageable);

    /**
     * Returns a page of employee IDs matching a keyword — used as the first
     * step of the two-query pagination strategy.
     *
     * @param keyword  the search term (case-insensitive LIKE)
     * @param pageable pagination and sorting parameters
     * @return a page of UUID primary keys whose owner data matches the keyword
     */
    @Query("""
            SELECT e.id FROM Employee e
            LEFT JOIN e.user u
            WHERE LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (e.firstName IS NOT NULL AND LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')))
               OR (e.lastName  IS NOT NULL AND LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%')))
               OR (u IS NOT NULL AND (
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               ))
            """)
    Page<UUID> searchIdsByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ── Fetch queries (load full graph by IDs) ────────────────────────────────

    /**
     * Loads full {@link Employee} entities with their {@code department} and
     * {@code user} associations fetched eagerly in a single query.
     *
     * <p>This is the second step of the two-query pagination strategy.
     * The result set is deliberately unordered — the service layer re-applies
     * the original page order after loading.
     *
     * @param ids the list of employee UUIDs to fetch
     * @return employees with associations initialised — no lazy proxies
     */
    @Query("""
            SELECT DISTINCT e FROM Employee e
            LEFT JOIN FETCH e.department
            LEFT JOIN FETCH e.user
            WHERE e.id IN :ids
            """)
    List<Employee> findAllWithAssociationsByIds(@Param("ids") List<UUID> ids);

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
     * @deprecated Use {@link #searchIdsByKeyword} + {@link #findAllWithAssociationsByIds}
     *             for paginated list responses; this method is retained for
     *             single-result lookups.
     */
    @Query("""
            SELECT e FROM Employee e
            LEFT JOIN e.user u
            WHERE LOWER(e.jobTitle) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR (e.firstName IS NOT NULL AND LOWER(e.firstName) LIKE LOWER(CONCAT('%', :keyword, '%')))
               OR (e.lastName  IS NOT NULL AND LOWER(e.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%')))
               OR (u IS NOT NULL AND (
                   LOWER(u.firstName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(u.lastName)  LIKE LOWER(CONCAT('%', :keyword, '%'))
               ))
            """)
    Page<Employee> searchByKeyword(@Param("keyword") String keyword, Pageable pageable);

    // ── Dashboard aggregation queries ─────────────────────────────────────────

    /**
     * Counts employees with a specific status.
     *
     * @param status the {@link EmployeeStatus} to count
     * @return total count of employees in the given status
     */
    long countByStatus(EmployeeStatus status);

    /**
     * Counts employees whose date of joining falls on or after the given date.
     * Used to compute "new this month" (pass first day of current month).
     *
     * @param fromDate inclusive start date
     * @return count of employees who joined on or after {@code fromDate}
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.dateOfJoining >= :fromDate")
    long countByDateOfJoiningOnOrAfter(@Param("fromDate") LocalDate fromDate);

    /**
     * Counts employees whose date of joining falls within the given date range.
     * Used for month-over-month trend computation.
     *
     * @param from inclusive start date
     * @param to   inclusive end date
     * @return count of employees who joined in the range
     */
    @Query("SELECT COUNT(e) FROM Employee e WHERE e.dateOfJoining >= :from AND e.dateOfJoining <= :to")
    long countByDateOfJoiningBetween(@Param("from") LocalDate from, @Param("to") LocalDate to);

    /**
     * Returns employee counts grouped by status for the dashboard status chart.
     * Each element of the list is an {@code Object[]} with two elements:
     * {@code [0]} the status string, {@code [1]} the count as {@code Long}.
     *
     * @return list of {@code [status, count]} pairs for all statuses present
     */
    @Query("SELECT e.status, COUNT(e) FROM Employee e GROUP BY e.status")
    List<Object[]> countGroupByStatus();
}
