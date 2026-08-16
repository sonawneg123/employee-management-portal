package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.TaskActivity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link TaskActivity} entities.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface TaskActivityRepository extends JpaRepository<TaskActivity, UUID> {

    /**
     * Returns all activity records for a given task, ordered by creation time ascending.
     *
     * @param taskId the UUID of the task
     * @return list of activity records for that task
     */
    List<TaskActivity> findByTaskIdOrderByCreatedAtAsc(UUID taskId);
}
