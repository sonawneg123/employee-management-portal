package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.TaskComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TaskComment} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface TaskCommentRepository extends JpaRepository<TaskComment, UUID> {

    /**
     * Returns all comments for the given task, ordered by creation time ascending,
     * with the author and their user association eagerly loaded.
     *
     * @param taskId the UUID of the task
     * @return ordered list of comments with author associations loaded
     */
    @Query("""
            SELECT c FROM TaskComment c
            LEFT JOIN FETCH c.author a
            LEFT JOIN FETCH a.user
            WHERE c.task.id = :taskId
            ORDER BY c.createdAt ASC
            """)
    List<TaskComment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") UUID taskId);

    /**
     * Loads a single comment with its author association.
     *
     * @param id the comment UUID
     * @return optional comment with author loaded
     */
    @Query("""
            SELECT c FROM TaskComment c
            LEFT JOIN FETCH c.author a
            LEFT JOIN FETCH a.user
            WHERE c.id = :id
            """)
    Optional<TaskComment> findByIdWithAuthor(@Param("id") UUID id);
}
