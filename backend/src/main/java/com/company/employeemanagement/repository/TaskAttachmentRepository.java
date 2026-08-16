package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.TaskAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TaskAttachment} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface TaskAttachmentRepository extends JpaRepository<TaskAttachment, UUID> {

    /**
     * Returns all attachments for the given task, with uploader associations loaded.
     *
     * @param taskId the UUID of the task
     * @return list of attachments
     */
    @Query("""
            SELECT a FROM TaskAttachment a
            LEFT JOIN FETCH a.uploader u
            LEFT JOIN FETCH u.user
            WHERE a.task.id = :taskId
            ORDER BY a.createdAt ASC
            """)
    List<TaskAttachment> findByTaskIdOrderByCreatedAtAsc(@Param("taskId") UUID taskId);

    /**
     * Loads a single attachment with its uploader association.
     *
     * @param id the attachment UUID
     * @return optional attachment with uploader loaded
     */
    @Query("""
            SELECT a FROM TaskAttachment a
            LEFT JOIN FETCH a.uploader u
            LEFT JOIN FETCH u.user
            WHERE a.id = :id
            """)
    Optional<TaskAttachment> findByIdWithUploader(@Param("id") UUID id);
}
