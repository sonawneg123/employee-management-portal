package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;


/**
 * Spring Data JPA repository for {@link Attendance} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {

    /**
     * Returns a paginated list of attendance records filtered by optional
     * employee UUID, date, and status.  Null parameters are skipped (no filter).
     *
     * @param employeeId optional employee UUID; pass {@code null} to skip
     * @param date       optional attendance date; pass {@code null} to skip
     * @param status     optional {@link AttendanceStatus}; pass {@code null} to skip
     * @param pageable   pagination and sorting parameters
     * @return a page of matching attendance records
     */
    @Query(value = """
            SELECT a FROM Attendance a
            JOIN FETCH a.employee e
            LEFT JOIN FETCH e.user
            WHERE (:employeeId IS NULL OR a.employee.id   = :employeeId)
              AND (:date       IS NULL OR a.attendanceDate = :date)
              AND (:status     IS NULL OR a.status         = :status)
            """,
           countQuery = """
            SELECT COUNT(a) FROM Attendance a
            WHERE (:employeeId IS NULL OR a.employee.id   = :employeeId)
              AND (:date       IS NULL OR a.attendanceDate = :date)
              AND (:status     IS NULL OR a.status         = :status)
            """)
    Page<Attendance> findByFilters(
            @Param("employeeId") UUID employeeId,
            @Param("date")       LocalDate date,
            @Param("status")     AttendanceStatus status,
            Pageable pageable);

    /**
     * Returns a paginated list of attendance records for a specific employee.
     *
     * @param employeeId the UUID of the employee
     * @param pageable   pagination and sorting parameters
     * @return a page of attendance records for the employee
     */
    Page<Attendance> findByEmployeeId(UUID employeeId, Pageable pageable);

    /**
     * Finds the attendance record for a specific employee on a specific date.
     * Used to enforce the uniqueness constraint at the service layer before
     * persisting a new record.
     *
     * @param employeeId     the UUID of the employee
     * @param attendanceDate the date to look up
     * @return an {@link Optional} containing the attendance record if found
     */
    Optional<Attendance> findByEmployeeIdAndAttendanceDate(UUID employeeId, LocalDate attendanceDate);

    // ── Dashboard aggregation queries ─────────────────────────────────────────

    /**
     * Counts attendance records for the given date with the given status.
     * Used to compute "present today" and "present yesterday" KPIs.
     *
     * @param attendanceDate the date to query
     * @param status         the {@link AttendanceStatus} to filter by
     * @return count of records matching the date and status
     */
    long countByAttendanceDateAndStatus(LocalDate attendanceDate, AttendanceStatus status);

    /**
     * Returns daily attendance counts (present / total-per-day) over the
     * given date range, grouped by date.  Each element of the result is an
     * {@code Object[]} with:
     * <ul>
     *   <li>{@code [0]} — {@link LocalDate} the attendance date</li>
     *   <li>{@code [1]} — {@code Long} count of PRESENT records</li>
     *   <li>{@code [2]} — {@code Long} total records for that day</li>
     * </ul>
     *
     * @param from  inclusive start date
     * @param to    inclusive end date
     * @param status the status to count as "present"
     * @return list of {@code [date, presentCount, totalCount]} rows
     */
    @Query("""
            SELECT a.attendanceDate,
                   SUM(CASE WHEN a.status = :status THEN 1 ELSE 0 END),
                   COUNT(a)
            FROM Attendance a
            WHERE a.attendanceDate >= :from AND a.attendanceDate <= :to
            GROUP BY a.attendanceDate
            ORDER BY a.attendanceDate ASC
            """)
    List<Object[]> countPresentAndTotalByDateRange(
            @Param("from")   LocalDate from,
            @Param("to")     LocalDate to,
            @Param("status") AttendanceStatus status);
}
