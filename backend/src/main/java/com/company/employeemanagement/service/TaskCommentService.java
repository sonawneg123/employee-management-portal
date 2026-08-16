package com.company.employeemanagement.service;

import com.company.employeemanagement.dto.request.CreateTaskCommentRequest;
import com.company.employeemanagement.dto.response.TaskCommentResponse;

import java.util.List;
import java.util.UUID;

/**
 * Service contract for task comment operations.
 *
 * <p>Authorization rules are enforced within the implementation:
 * <ul>
 *   <li>EMPLOYEE can only comment on tasks assigned to them.</li>
 *   <li>MANAGER / HR / ADMIN can comment on any task.</li>
 *   <li>IDOR protection: employees cannot see comments on tasks that are not theirs.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public interface TaskCommentService {

    /**
     * Returns all comments for the given task, ordered oldest-first.
     *
     * @param taskId UUID of the task
     * @return ordered list of comment responses
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if an employee tries to view comments on another employee's task
     */
    List<TaskCommentResponse> findByTaskId(UUID taskId);

    /**
     * Posts a new comment on the given task.
     *
     * @param taskId  UUID of the task to comment on
     * @param request the comment payload
     * @return the saved {@link TaskCommentResponse}
     * @throws com.company.employeemanagement.exception.ResourceNotFoundException
     *         if no task with the given ID exists
     * @throws com.company.employeemanagement.exception.AccessDeniedException
     *         if an employee tries to comment on another employee's task
     */
    TaskCommentResponse create(UUID taskId, CreateTaskCommentRequest request);
}
