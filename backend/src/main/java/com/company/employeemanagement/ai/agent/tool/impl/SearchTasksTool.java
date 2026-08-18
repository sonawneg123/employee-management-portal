package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.service.TaskService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tool: search_tasks
 * Searches for tasks with optional filters. Role-scoped.
 *
 * <h2>Employee scoping (critical)</h2>
 * <p>When the user is an EMPLOYEE (no privileged role) and no explicit
 * {@code assignedEmployeeId} is given, this tool automatically scopes the query
 * to the authenticated employee's own tasks by calling
 * {@link TaskService#findMyAssignedTasks}. This prevents the tool from
 * returning ALL tasks to a regular employee regardless of what arguments
 * the LLM passes.
 *
 * <p>When the user is a MANAGER/HR/ADMIN, the unscoped
 * {@link TaskService#findAll} is used instead, which allows optional
 * filtering by any employee ID.
 */
@Component
public class SearchTasksTool extends AbstractAgentTool {

    private final TaskService taskService;

    public SearchTasksTool(final TaskService taskService) {
        this.taskService = taskService;
    }

    @Override public String getName() { return "search_tasks"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about actual tasks in the system — "
               + "their own assigned tasks, tasks by status, overdue tasks, task lists, "
               + "or tasks belonging to a specific employee. "
               + "Trigger phrases: 'what tasks are assigned to me', 'my tasks', 'show me my tasks', "
               + "'what are my current tasks', 'overdue tasks', 'tasks in progress', "
               + "'tasks assigned to [name]', 'pending tasks'. "
               + "Parameters: status (optional: DRAFT/ASSIGNED/IN_PROGRESS/SUBMITTED/COMPLETED/REJECTED), "
               + "assignedEmployeeId (optional UUID — for managers/HR/admin only; "
               + "employees always see only their own tasks regardless of this parameter). "
               + "Never answer task questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "status":{"type":"string","description":"Task status filter: DRAFT/ASSIGNED/IN_PROGRESS/SUBMITTED/COMPLETED/REJECTED"},
               "assignedEmployeeId":{"type":"string","description":"Employee UUID to filter by (managers/HR/admin only)"}},
               "required":[]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String statusStr = arg(args, "status");
        String assignedEmployeeIdStr = arg(args, "assignedEmployeeId");

        TaskStatus status = null;
        if (statusStr != null) {
            try {
                status = TaskStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Invalid status: " + statusStr
                        + ". Valid values: DRAFT, ASSIGNED, IN_PROGRESS, SUBMITTED, COMPLETED, REJECTED";
            }
        }

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dueDate"));

        boolean isPrivileged = context.roles().contains("ROLE_ADMIN")
                || context.roles().contains("ROLE_HR")
                || context.roles().contains("ROLE_MANAGER");

        PageResponse<TaskResponse> page;

        if (!isPrivileged) {
            // EMPLOYEE: always scope to own tasks — ignore any assignedEmployeeId the LLM may have sent,
            // and do NOT fall through to findAll which might return all tasks if employee check fails.
            // findMyAssignedTasks uses SecurityContext to enforce correct scoping.
            log.info("SEARCH_TASKS employee-scoped query for user={} employeeId={}",
                    context.username(),
                    context.currentEmployee() != null ? context.currentEmployee().getId() : "null");
            page = taskService.findMyAssignedTasks(status, null, pageable);
        } else {
            // MANAGER/HR/ADMIN: allow optional filter by assignedEmployeeId
            UUID assignedId = null;
            if (assignedEmployeeIdStr != null) {
                try {
                    assignedId = UUID.fromString(assignedEmployeeIdStr);
                } catch (IllegalArgumentException e) {
                    return "Invalid assignedEmployeeId format: " + assignedEmployeeIdStr;
                }
            }
            log.info("SEARCH_TASKS privileged query user={} assignedEmployeeId={} status={}",
                    context.username(), assignedId, status);
            page = taskService.findAll(assignedId, null, status, null, null, pageable);
        }

        if (page.content().isEmpty()) {
            return "No tasks found matching the criteria.";
        }

        return "Found " + page.totalElements() + " task(s):\n" +
               page.content().stream().map(SearchTasksTool::formatTask).collect(Collectors.joining("\n"));
    }

    public static String formatTask(final TaskResponse t) {
        return String.format(
                "[%s] %s | Status: %s | Priority: %s | Due: %s | Assigned to: %s%s",
                t.id(), t.title(), t.status(), t.priority(),
                t.dueDate() != null ? t.dueDate() : "N/A",
                t.assignedEmployeeName() != null ? t.assignedEmployeeName() : "Unassigned",
                t.overdue() ? " ⚠ OVERDUE" : ""
        );
    }
}
