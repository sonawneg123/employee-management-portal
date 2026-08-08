package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link LeaveRequest} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {

    /**
     * Returns a paginated list of leave requests for a specific employee.
     *
     * @param employeeId the UUID of the employee
     * @param pageable   pagination and sorting parameters
     * @return a page of leave requests belonging to the employee
     */
    Page<LeaveRequest> findByEmployeeId(UUID employeeId, Pageable pageable);

    /**
     * Returns a paginated list of leave requests filtered by approval status.
     *
     * @param status   the {@link LeaveStatus} to filter by
     * @param pageable pagination and sorting parameters
     * @return a page of leave requests with the specified status
     */
    Page<LeaveRequest> findByStatus(LeaveStatus status, Pageable pageable);
}
