package com.company.employeemanagement.repository;

import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link Task} entities.
 *
 * <p>List queries use the two-step ID + fetch approach consistent with
 * other repositories in this project to prevent N+1 selects.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, UUID> {

    // ── ID queries (pagination at the database layer) ─────────────────────────

    /**
     * Returns a page of task IDs with optional filtering by assignee, creator,
     * status, priority, and category.
     *
     * @param assignedEmployeeId optional assignee UUID filter
     * @param createdByEmployeeId optional creator UUID filter
     * @param status             optional status filter
     * @param priority           optional priority filter
     * @param category           optional category filter
     * @param pageable           pagination and sorting parameters
     * @return page of matching task UUIDs
     */
    @Query("""
            SELECT t.id FROM Task t
            WHERE (:assignedEmployeeId  IS NULL OR t.assignedEmployee.id  = :assignedEmployeeId)
              AND (:createdByEmployeeId IS NULL OR t.createdByEmployee.id = :createdByEmployeeId)
              AND (:status              IS NULL OR t.status               = :status)
              AND (:priority            IS NULL OR t.priority             = :priority)
              AND (:category            IS NULL OR t.category             = :category)
            """)
    Page<UUID> findIdsByFilters(
            @Param("assignedEmployeeId")  UUID assignedEmployeeId,
            @Param("createdByEmployeeId") UUID createdByEmployeeId,
            @Param("status")              TaskStatus status,
            @Param("priority")            TaskPriority priority,
            @Param("category")            TaskCategory category,
            Pageable pageable);

    // ── Fetch queries (load full graph for a batch of IDs) ────────────────────

    /**
     * Loads full {@link Task} entities with their {@code assignedEmployee},
     * the employee's {@code user}, and the {@code createdByEmployee} association
     * eagerly fetched in a single query.
     *
     * @param ids the list of task UUIDs to fetch
     * @return tasks with associations initialised
     */
    @Query("""
            SELECT DISTINCT t FROM Task t
            LEFT JOIN FETCH t.assignedEmployee ae
            LEFT JOIN FETCH ae.user
            LEFT JOIN FETCH t.createdByEmployee ce
            LEFT JOIN FETCH ce.user
            WHERE t.id IN :ids
            """)
    List<Task> findAllWithAssociationsByIds(@Param("ids") List<UUID> ids);

    // ── Single-record fetch with associations (for detail/update) ─────────────

    /**
     * Loads a single {@link Task} with all its associations for display or
     * update operations.
     *
     * @param id the task UUID
     * @return an {@link java.util.Optional} containing the task with associations loaded
     */
    @Query("""
            SELECT t FROM Task t
            LEFT JOIN FETCH t.assignedEmployee ae
            LEFT JOIN FETCH ae.user
            LEFT JOIN FETCH t.createdByEmployee ce
            LEFT JOIN FETCH ce.user
            WHERE t.id = :id
            """)
    java.util.Optional<Task> findByIdWithAssociations(@Param("id") UUID id);

    // ── Aggregation queries (dashboard KPIs) ──────────────────────────────────

    /** Counts tasks with the given status. */
    long countByStatus(TaskStatus status);

    /**
     * Counts tasks whose status is not in {@code statuses} and whose
     * {@code dueDate} is before the given date (i.e. overdue).
     *
     * @param statuses the statuses to exclude from the overdue check
     * @param today    the current date
     * @return count of overdue tasks
     */
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.status NOT IN :statuses
              AND t.dueDate IS NOT NULL
              AND t.dueDate < :today
            """)
    long countOverdue(
            @Param("statuses") List<TaskStatus> statuses,
            @Param("today")    LocalDate today);

    /**
     * Counts tasks assigned to a specific employee with the given status.
     *
     * @param assignedEmployeeId the UUID of the employee
     * @param status             the status to count
     * @return matching task count
     */
    long countByAssignedEmployeeIdAndStatus(UUID assignedEmployeeId, TaskStatus status);

    // ── Workload queries ──────────────────────────────────────────────────────

    /**
     * Counts tasks for the given employee in active statuses
     * (ASSIGNED or IN_PROGRESS).
     *
     * @param employeeId the UUID of the employee
     * @return number of active tasks
     */
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.status IN (
                  com.company.employeemanagement.entity.enums.TaskStatus.ASSIGNED,
                  com.company.employeemanagement.entity.enums.TaskStatus.IN_PROGRESS
              )
            """)
    long countActiveTasksByEmployeeId(@Param("employeeId") UUID employeeId);

    /**
     * Counts overdue (past due date, not completed) tasks for the given employee.
     *
     * @param employeeId the UUID of the employee
     * @param today      the current date
     * @param excluded   statuses that are considered "done" and should be excluded
     * @return number of overdue tasks for this employee
     */
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.status NOT IN :excluded
              AND t.dueDate IS NOT NULL
              AND t.dueDate < :today
            """)
    long countOverdueTasksByEmployeeId(
            @Param("employeeId") UUID employeeId,
            @Param("today")      LocalDate today,
            @Param("excluded")   List<TaskStatus> excluded);

    /**
     * Counts tasks for the given employee that are in the SUBMITTED state
     * (pending review by a manager).
     *
     * @param employeeId the UUID of the employee
     * @return number of tasks pending review
     */
    @Query("""
            SELECT COUNT(t) FROM Task t
            WHERE t.assignedEmployee.id = :employeeId
              AND t.status = com.company.employeemanagement.entity.enums.TaskStatus.SUBMITTED
            """)
    long countPendingReviewByEmployeeId(@Param("employeeId") UUID employeeId);

    // ── Reminder scheduler queries ────────────────────────────────────────────

    /**
     * Returns all non-completed tasks with a due date set, for use by the
     * reminder scheduler. Loads the assigned employee and their user association.
     *
     * @param excludedStatuses statuses to exclude (typically COMPLETED, REJECTED)
     * @return list of tasks needing reminder evaluation
     */
    @Query("""
            SELECT t FROM Task t
            LEFT JOIN FETCH t.assignedEmployee ae
            LEFT JOIN FETCH ae.user
            WHERE t.status NOT IN :excludedStatuses
              AND t.dueDate IS NOT NULL
              AND t.assignedEmployee IS NOT NULL
            """)
    List<Task> findNonCompletedTasksWithDueDate(
            @Param("excludedStatuses") List<TaskStatus> excludedStatuses);

    // ── Workload summary query ────────────────────────────────────────────────

    /**
     * Returns the active task count per employee (ASSIGNED or IN_PROGRESS).
     * Each element is {@code Object[] { employeeId (String), count (Long) }}.
     *
     * @return list of [employeeId, activeTaskCount] pairs
     */
    @Query("""
            SELECT t.assignedEmployee.id, COUNT(t)
            FROM Task t
            WHERE t.assignedEmployee IS NOT NULL
              AND t.status IN (
                  com.company.employeemanagement.entity.enums.TaskStatus.ASSIGNED,
                  com.company.employeemanagement.entity.enums.TaskStatus.IN_PROGRESS
              )
            GROUP BY t.assignedEmployee.id
            """)
    List<Object[]> countActiveTasksGroupByEmployee();
}
