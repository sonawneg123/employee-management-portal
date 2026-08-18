package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.service.TaskService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool: search_tasks
 * Searches for tasks with optional filters. Role-scoped.
 * Employees see only their own tasks.
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
               + "assignedEmployeeId (optional UUID). "
               + "Employees automatically see only their own tasks. "
               + "Never answer task questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "status":{"type":"string","description":"Task status filter"},
               "assignedEmployeeId":{"type":"string","description":"Employee UUID to filter by"}},
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
        String assignedEmployeeId = arg(args, "assignedEmployeeId");

        TaskStatus status = null;
        if (statusStr != null) {
            try {
                status = TaskStatus.valueOf(statusStr.toUpperCase());
            } catch (IllegalArgumentException e) {
                return "Invalid status: " + statusStr + ". Valid values: DRAFT, ASSIGNED, IN_PROGRESS, SUBMITTED, COMPLETED, REJECTED";
            }
        }

        PageRequest pageable = PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "dueDate"));

        java.util.UUID assignedId = null;
        if (assignedEmployeeId != null) {
            try {
                assignedId = java.util.UUID.fromString(assignedEmployeeId);
            } catch (IllegalArgumentException e) {
                return "Invalid assignedEmployeeId format: " + assignedEmployeeId;
            }
        }

        PageResponse<TaskResponse> page = taskService.findAll(assignedId, null, status, null, null, pageable);

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
