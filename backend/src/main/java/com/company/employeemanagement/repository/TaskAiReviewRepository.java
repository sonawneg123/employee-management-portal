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
     * Returns all AI reviews for the given task, with submission's submittedBy loaded.
     *
     * @param taskId the task UUID
     * @return list of AI reviews
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.submission s
            LEFT JOIN FETCH s.submittedBy sb
            LEFT JOIN FETCH sb.user
            LEFT JOIN FETCH r.task t
            LEFT JOIN FETCH r.requestedBy rb
            LEFT JOIN FETCH rb.user
            WHERE r.task.id = :taskId
            ORDER BY r.createdAt DESC
            """)
    List<TaskAiReview> findAllByTaskId(@Param("taskId") UUID taskId);

    /**
     * Returns all AI reviews for the given task with only COMPLETED status,
     * ordered by completedAt ascending for trend computation.
     *
     * @param taskId the task UUID
     * @return list of completed AI reviews, oldest first
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.submission s
            LEFT JOIN FETCH s.submittedBy sb
            LEFT JOIN FETCH r.task t
            WHERE r.task.id = :taskId
              AND r.status = 'COMPLETED'
            ORDER BY r.completedAt ASC
            """)
    List<TaskAiReview> findCompletedByTaskIdOrderByCompletedAtAsc(@Param("taskId") UUID taskId);

    /**
     * Returns the latest completed AI review for the given submission.
     * Used for employee AI history queries (includes submittedBy for authorization).
     *
     * @param submissionId the submission UUID
     * @return reviews with submission associations
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.submission s
            LEFT JOIN FETCH s.submittedBy sb
            LEFT JOIN FETCH sb.user
            LEFT JOIN FETCH r.task t
            WHERE r.submission.id = :submissionId
            ORDER BY r.createdAt DESC
            """)
    List<TaskAiReview> findAllBySubmissionIdWithSubmitter(
            @Param("submissionId") UUID submissionId);

    /**
     * Returns all reviews stuck in PENDING or PROCESSING state.
     *
     * <p>Used by the startup recovery mechanism to detect reviews that were
     * interrupted by an application restart before they could complete.
     *
     * @param statuses the statuses to search for
     * @return list of stale reviews
     */
    @Query("""
            SELECT r FROM TaskAiReview r
            LEFT JOIN FETCH r.task
            LEFT JOIN FETCH r.submission s
            LEFT JOIN FETCH s.task
            LEFT JOIN FETCH r.requestedBy rb
            LEFT JOIN FETCH rb.user
            WHERE r.status IN :statuses
            """)
    List<TaskAiReview> findAllByStatusIn(@Param("statuses") List<AiReviewStatus> statuses);

    // ── Analytics queries ─────────────────────────────────────────────────────

    /**
     * Counts AI reviews by status.
     *
     * @param status the status to count
     * @return count of reviews in the given status
     */
    long countByStatus(AiReviewStatus status);

    /**
     * Returns average completion and quality scores for completed AI reviews,
     * grouped by completion date, for trend computation.
     *
     * <p>Each element is {@code Object[] { date (LocalDate), avgCompletionScore (Double),
     * avgQualityScore (Double), count (Long) }}.
     *
     * @param from inclusive start date (completedAt)
     * @param to   inclusive end date (completedAt)
     * @return list of [date, avgCompletionScore, avgQualityScore, count] rows
     */
    @Query("""
            SELECT CAST(r.completedAt AS date),
                   AVG(r.completionScore),
                   AVG(r.qualityScore),
                   COUNT(r)
            FROM TaskAiReview r
            WHERE r.status = com.company.employeemanagement.entity.enums.AiReviewStatus.COMPLETED
              AND r.completedAt IS NOT NULL
              AND CAST(r.completedAt AS date) >= :from
              AND CAST(r.completedAt AS date) <= :to
            GROUP BY CAST(r.completedAt AS date)
            ORDER BY CAST(r.completedAt AS date) ASC
            """)
    List<Object[]> findScoreTrendByDateRange(
            @Param("from") java.time.LocalDate from,
            @Param("to")   java.time.LocalDate to);

    /**
     * Computes aggregate average scores across all completed AI reviews.
     *
     * <p>Returns a single {@code Object[]} with:
     * <ul>
     *   <li>{@code [0]} — average completion score ({@code Double})</li>
     *   <li>{@code [1]} — average quality score ({@code Double})</li>
     *   <li>{@code [2]} — count of completed reviews ({@code Long})</li>
     * </ul>
     *
     * @return aggregated stats, or single row with nulls if no data
     */
    @Query("""
            SELECT AVG(r.completionScore), AVG(r.qualityScore), COUNT(r)
            FROM TaskAiReview r
            WHERE r.status = com.company.employeemanagement.entity.enums.AiReviewStatus.COMPLETED
            """)
    Object[] findAggregateScores();
}
