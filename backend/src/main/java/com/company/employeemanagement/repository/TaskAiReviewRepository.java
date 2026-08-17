package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TaskAiReview} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface TaskAiReviewRepository extends JpaRepository<TaskAiReview, UUID> {

    /**
     * Returns all AI reviews for the given submission, newest first.
     *
     * @param submissionId the submission UUID
     * @return list of AI reviews
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.requestedBy rb
            LEFT JOIN FETCH rb.user
            WHERE r.submission.id = :submissionId
            ORDER BY r.createdAt DESC
            """)
    List<TaskAiReview> findAllBySubmissionIdOrderByCreatedAtDesc(
            @Param("submissionId") UUID submissionId);

    /**
     * Returns the most recent AI review for a submission.
     *
     * @param submissionId the submission UUID
     * @return the latest review, or empty
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.requestedBy rb
            LEFT JOIN FETCH rb.user
            WHERE r.submission.id = :submissionId
            ORDER BY r.createdAt DESC
            LIMIT 1
            """)
    Optional<TaskAiReview> findLatestBySubmissionId(@Param("submissionId") UUID submissionId);

    /**
     * Returns a review by ID with all associations loaded.
     *
     * @param id the review UUID
     * @return the review with associations, or empty
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.task t
            LEFT JOIN FETCH r.submission s
            LEFT JOIN FETCH s.submittedBy sb
            LEFT JOIN FETCH sb.user
            LEFT JOIN FETCH r.requestedBy rb
            LEFT JOIN FETCH rb.user
            WHERE r.id = :id
            """)
    Optional<TaskAiReview> findByIdWithAssociations(@Param("id") UUID id);

    /**
     * Returns whether a PENDING or PROCESSING review exists for the given submission.
     * Used to prevent duplicate simultaneous review requests.
     *
     * @param submissionId the submission UUID
     * @param status       the status to check
     * @return true if such a review exists
     */
    boolean existsBySubmissionIdAndStatus(UUID submissionId, AiReviewStatus status);

    /**
     * Returns all AI reviews for the given task.
     *
     * @param taskId the task UUID
     * @return list of AI reviews
     */
    List<TaskAiReview> findAllByTaskId(UUID taskId);
}
