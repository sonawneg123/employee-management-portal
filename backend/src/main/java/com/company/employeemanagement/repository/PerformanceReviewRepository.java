package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.PerformanceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Spring Data JPA repository for {@link PerformanceReview} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {

    /**
     * Returns a paginated list of performance reviews for a specific employee,
     * ordered according to the supplied {@link Pageable}.
     *
     * @param employeeId the UUID of the employee
     * @param pageable   pagination and sorting parameters
     * @return a page of performance reviews for the employee
     */
    Page<PerformanceReview> findByEmployeeId(UUID employeeId, Pageable pageable);

    /**
     * Returns a paginated list of all performance reviews across all employees,
     * ordered according to the supplied {@link Pageable}.
     *
     * @param pageable pagination and sorting parameters
     * @return a page of all performance reviews
     */
    Page<PerformanceReview> findAll(Pageable pageable);
}
