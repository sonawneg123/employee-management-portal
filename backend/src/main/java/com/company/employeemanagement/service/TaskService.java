package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskStatusRequest;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskActivityResponse;
import com.company.employeemanagement.dto.response.TaskDashboardStatsResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.dto.response.WorkloadResponse;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for task management operations.
 *
 * <p>All methods return DTOs — entities are never exposed at this layer or above.
 *
 * <p>Authorization rules are enforced within the implementation:
 * <ul>
 *   <li>MANAGER / HR / ADMIN can create, assign, update, and view any task.</li>
 *   <li>EMPLOYEE can only view and update status on tasks assigned to themselves.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public interface TaskService {

    /**
     * Returns a paginated list of tasks with optional filtering.
     *
     * @param assignedEmployeeId  optional filter by assignee UUID
     * @param createdByEmployeeId optional filter by creator UUID
     * @param status              optional status filter
     * @param priority            optional priority filter
     * @param category            optional category filter
     * @param pageable            pagination and sorting parameters
     * @return page of matching tasks
     */
    PageResponse<TaskResponse> findAll(UUID assignedEmployeeId,
                                       UUID createdByEmployeeId,
                                       TaskStatus status,
                                       TaskPriority priority,
                                       TaskCategory category,
                                       Pageable pageable);

    /**
     * Returns tasks assigned to the currently authenticated employee.
     *
     * @param status   optional status filter
     * @param priority optional priority filter
     * @param pageable pagination and sorting parameters
     * @return page of the caller's assigned tasks
     */
    PageResponse<TaskResponse> findMyAssignedTasks(TaskStatus status,
                                                    TaskPriority priority,
                                                    Pageable pageable);

    /**
     * Returns tasks created by the currently authenticated principal's
     * linked employee record.
     *
     * @param status   optional status filter
     * @param priority optional priority filter
     * @param pageable pagination and sorting parameters
     * @return page of tasks created by the caller
     */
    PageResponse<TaskResponse> findMyCreatedTasks(TaskStatus status,
                                                   TaskPriority priority,
                                                   Pageable pageable);

    /**
     * Returns a single task by UUID.
     *
     * @param id the task UUID
     * @return the matching {@link TaskResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if the caller is an EMPLOYEE not assigned to this task
     */
    TaskResponse findById(UUID id);

    /**
     * Creates a new task. The caller's employee record is recorded as the creator.
     *
     * @param request the creation payload
     * @return the newly created {@link TaskResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if the referenced assignee employee does not exist
     * @throws IllegalStateException if the assignee is not checked in
     */
    TaskResponse create(CreateTaskRequest request);

    /**
     * Updates an existing task (manager operation).
     *
     * @param id      the task UUID
     * @param request the update payload
     * @return the updated {@link TaskResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if the caller is not the creator and does not hold a privileged role
     * @throws IllegalStateException if the new assignee is not checked in
     */
    TaskResponse update(UUID id, UpdateTaskRequest request);

    /**
     * Updates only the status of a task. Used by employees for workflow
     * transitions (e.g. ASSIGNED → IN_PROGRESS).
     *
     * @param id      the task UUID
     * @param request the new status
     * @return the updated {@link TaskResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if the employee is not assigned to this task
     * @throws IllegalStateException if the requested status transition is not permitted
     */
    TaskResponse updateStatus(UUID id, UpdateTaskStatusRequest request);

    /**
     * Reassigns a task to a different employee.
     *
     * @param id      the task UUID
     * @param request the reassignment payload
     * @return the updated {@link TaskResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task or employee with the given IDs exist
     * @throws IllegalStateException if the new assignee is not checked in
     */
    TaskResponse reassign(UUID id, ReassignTaskRequest request);

    /**
     * Deletes a task by UUID.
     *
     * @param id the task UUID
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if the caller is not privileged to delete the task
     */
    void delete(UUID id);

    /**
     * Returns the activity timeline for the given task.
     *
     * @param taskId the UUID of the task
     * @return list of activity entries ordered oldest-first
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if an employee tries to access activities for another employee's task
     */
    List<TaskActivityResponse> getActivityTimeline(UUID taskId);

    /**
     * Returns workload information for a single employee.
     *
     * @param employeeId the UUID of the employee
     * @return the employee's workload response
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no employee with the given ID exists
     */
    WorkloadResponse getWorkload(UUID employeeId);

    /**
     * Returns aggregated dashboard statistics for all tasks.
     *
     * @return task counts by status, overdue count, urgency count, and completion percentage
     */
    TaskDashboardStatsResponse getDashboardStats();

    /**
     * Returns a workload summary list — each active employee with their active task count.
     *
     * @return list of employee workload responses
     */
    List<WorkloadResponse> getWorkloadSummary();

    /**
     * Returns all active employees with their current check-in status and active task count.
     * Used by the task assignment form UI.
     *
     * @return list of employee availability responses
     */
    List<EmployeeAvailabilityResponse> getEmployeeAvailability();
}
