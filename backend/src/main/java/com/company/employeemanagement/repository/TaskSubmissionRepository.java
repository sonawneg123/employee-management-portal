package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.enums.SubmissionStatus;
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
 * Spring Data JPA repository for {@link TaskSubmission} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, UUID> {

    /**
     * Returns all submissions for the given task, ordered by submission time descending.
     *
     * @param taskId the task UUID
     * @return list of submissions
     */
    @Query("""
            SELECT ts FROM TaskSubmission ts
            LEFT JOIN FETCH ts.submittedBy sb
            LEFT JOIN FETCH sb.user
            LEFT JOIN FETCH ts.reviewedBy rb
            LEFT JOIN FETCH rb.user
            WHERE ts.task.id = :taskId
            ORDER BY ts.submittedAt DESC
            """)
    List<TaskSubmission> findAllByTaskIdOrderBySubmittedAtDesc(@Param("taskId") UUID taskId);

    /**
     * Returns the most recent submission for the given task.
     *
     * @param taskId the task UUID
     * @return the latest submission, or empty
     */
    @Query("""
            SELECT ts FROM TaskSubmission ts
            LEFT JOIN FETCH ts.submittedBy sb
            LEFT JOIN FETCH sb.user
            LEFT JOIN FETCH ts.reviewedBy rb
            LEFT JOIN FETCH rb.user
            WHERE ts.task.id = :taskId
            ORDER BY ts.submittedAt DESC
            LIMIT 1
            """)
    Optional<TaskSubmission> findLatestByTaskId(@Param("taskId") UUID taskId);

    /**
     * Returns a submission by ID with all associations loaded.
     *
     * @param id the submission UUID
     * @return the submission with associations, or empty
     */
    @Query("""
            SELECT ts FROM TaskSubmission ts
            LEFT JOIN FETCH ts.task t
            LEFT JOIN FETCH t.assignedEmployee ae
            LEFT JOIN FETCH ae.user
            LEFT JOIN FETCH t.createdByEmployee ce
            LEFT JOIN FETCH ce.user
            LEFT JOIN FETCH ts.submittedBy sb
            LEFT JOIN FETCH sb.user
            LEFT JOIN FETCH ts.reviewedBy rb
            LEFT JOIN FETCH rb.user
            WHERE ts.id = :id
            """)
    Optional<TaskSubmission> findByIdWithAssociations(@Param("id") UUID id);

    /**
     * Returns submissions for the given task filtered by review status.
     *
     * @param taskId       the task UUID
     * @param reviewStatus the status to filter by
     * @return list of matching submissions
     */
    List<TaskSubmission> findAllByTaskIdAndReviewStatus(UUID taskId, SubmissionStatus reviewStatus);

    /**
     * Returns submissions by a specific employee (own submissions only).
     *
     * @param submittedById the employee UUID
     * @param pageable      pagination parameters
     * @return page of submissions
     */
    Page<TaskSubmission> findAllBySubmittedById(UUID submittedById, Pageable pageable);

    /**
     * Returns whether a PENDING_REVIEW submission exists for the given task.
     *
     * @param taskId       the task UUID
     * @param reviewStatus the review status to check
     * @return true if such a submission exists
     */
    boolean existsByTaskIdAndReviewStatus(UUID taskId, SubmissionStatus reviewStatus);
}
