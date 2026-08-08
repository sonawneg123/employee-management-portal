package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
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
}
