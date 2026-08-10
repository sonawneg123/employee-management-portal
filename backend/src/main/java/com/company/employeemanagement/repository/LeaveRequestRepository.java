package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LeaveRequest} entities.
 *
 * <p>List-oriented queries use a two-step ID + fetch approach to prevent
 * N+1 selects. The {@link LeaveRequestMapper} accesses
 * {@code leaveRequest.getEmployee().getDepartment().getName()} and
 * {@code leaveRequest.getEmployee().getUser()}, all of which are lazily
 * loaded. Without JOIN FETCH, each leave in a page would trigger two to
 * three additional round-trips per row.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    // ── Scalar ID queries (pagination happens at the database) ────────────────

    /**
     * Returns a page of leave request IDs for a specific employee.
     *
     * @param employeeId the UUID of the employee
     * @param pageable   pagination and sorting parameters
     * @return a page of leave request UUIDs
     */
    @Query("SELECT lr.id FROM LeaveRequest lr WHERE lr.employee.id = :employeeId")
    Page<UUID> findIdsByEmployeeId(@Param("employeeId") UUID employeeId, Pageable pageable);

    /**
     * Returns a page of leave request IDs filtered by approval status.
     *
     * @param status   the {@link LeaveStatus} to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of leave request UUIDs with the specified status
     */
    @Query("SELECT lr.id FROM LeaveRequest lr WHERE lr.status = :status")
    Page<UUID> findIdsByStatus(@Param("status") LeaveStatus status, Pageable pageable);

    /**
     * Returns a page of leave request IDs filtered by employee and/or status and/or type.
     * Null parameters are treated as "no filter" (wildcard).
     *
     * @param employeeId optional employee UUID filter; pass {@code null} to skip
     * @param status     optional {@link LeaveStatus} filter; pass {@code null} to skip
     * @param leaveType  optional {@link LeaveType} filter; pass {@code null} to skip
     * @param pageable   pagination and sorting parameters
     * @return a page of matching leave request UUIDs
     */
    @Query("""
            SELECT lr.id FROM LeaveRequest lr
            WHERE (:employeeId IS NULL OR lr.employee.id = :employeeId)
              AND (:status     IS NULL OR lr.status    = :status)
              AND (:leaveType  IS NULL OR lr.leaveType = :leaveType)
            """)
    Page<UUID> findIdsByFilters(
            @Param("employeeId") UUID employeeId,
            @Param("status")     LeaveStatus status,
            @Param("leaveType")  LeaveType leaveType,
            Pageable pageable);

    /**
     * Returns a page of all leave request IDs.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of leave request UUIDs
     */
    @Query("SELECT lr.id FROM LeaveRequest lr")
    Page<UUID> findAllIds(Pageable pageable);

    // ── Fetch queries (load full graph by IDs in one round-trip) ─────────────

    /**
     * Loads full {@link LeaveRequest} entities with their {@code employee},
     * the employee's {@code department}, and the employee's {@code user}
     * associations eagerly fetched in a single query.
     *
     * <p>This is the second step of the two-query pagination strategy and
     * ensures the {@link com.company.employeemanagement.mapper.LeaveRequestMapper}
     * never triggers lazy loads.
     *
     * @param ids the list of leave request UUIDs to fetch
     * @return leave requests with associations initialised
     */
    @Query("""
            SELECT DISTINCT lr FROM LeaveRequest lr
            JOIN FETCH lr.employee e
            JOIN FETCH e.department
            LEFT JOIN FETCH e.user
            WHERE lr.id IN :ids
            """)
    List<LeaveRequest> findAllWithAssociationsByIds(@Param("ids") List<UUID> ids);

    // ── Legacy Spring Data derived queries (kept for single-record lookups) ───

    /**
     * Returns a paginated list of leave requests for a specific employee.
     *
     * <p>Prefer {@link #findIdsByEmployeeId} + {@link #findAllWithAssociationsByIds}
     * for list endpoints; this method is used internally or for single-entity
     * ownership checks.
     *
     * @param employeeId the UUID of the employee
     * @param pageable   pagination and sorting parameters
     * @return a page of leave requests belonging to the employee
     */
    Page<LeaveRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

    /**
     * Returns a paginated list of leave requests filtered by approval status.
     *
     * <p>Prefer {@link #findIdsByStatus} + {@link #findAllWithAssociationsByIds}
     * for list endpoints.
     *
     * @param status   the {@link LeaveStatus} to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of leave requests with the specified status
     */
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);

    // ── Dashboard aggregation queries ─────────────────────────────────────────

    /**
     * Counts leave requests with the given status.
     * Used to compute the "pending leaves" KPI.
     *
     * @param status the {@link LeaveStatus} to count
     * @return total count of leave requests in the given status
     */
    long countByStatus(LeaveStatus status);

    /**
     * Counts approved leave requests whose date range covers the given date.
     * Used to compute "on leave today".
     *
     * @param status the status to filter on (use {@link LeaveStatus#APPROVED})
     * @param date   the date to check coverage for
     * @return count of approved leaves spanning the given date
     */
    @Query("""
            SELECT COUNT(lr) FROM LeaveRequest lr
            WHERE lr.status = :status
              AND lr.startDate <= :date
              AND lr.endDate   >= :date
            """)
    long countByStatusAndDateRange(
            @Param("status") LeaveStatus status,
            @Param("date")   LocalDate date);

    /**
     * Counts PENDING leave requests created on or before the given cutoff
     * date-time. Used to calculate the "pending leaves trend" vs 7 days ago.
     *
     * @param status  the status to filter on (use {@link LeaveStatus#PENDING})
     * @param cutoff  only count requests created before this date
     * @return count of PENDING requests that existed as of the cutoff
     */
    @Query("""
            SELECT COUNT(lr) FROM LeaveRequest lr
            WHERE lr.status = :status
              AND CAST(lr.createdAt AS date) <= :cutoff
            """)
    long countByStatusCreatedOnOrBefore(
            @Param("status") LeaveStatus status,
            @Param("cutoff") LocalDate cutoff);

    /**
     * Returns recent leave requests for the activity feed, ordered by
     * creation time descending. Uses a JOIN FETCH to avoid N+1 on employee/user.
     *
     * @param pageable pagination (use {@code PageRequest.of(0, limit)})
     * @return a page of leave requests with employee and user loaded
     */
    @Query("""
            SELECT lr FROM LeaveRequest lr
            JOIN FETCH lr.employee e
            LEFT JOIN FETCH e.user
            ORDER BY lr.createdAt DESC
            """)
    Page<LeaveRequest> findRecentWithEmployee(Pageable pageable);
}
