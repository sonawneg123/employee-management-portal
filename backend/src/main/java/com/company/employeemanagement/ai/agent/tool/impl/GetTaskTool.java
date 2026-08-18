package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tool: get_task
 * Returns detailed information about a specific task by ID.
 */
@Component
public class GetTaskTool extends AbstractAgentTool {

    private final TaskService taskService;

    public GetTaskTool(final TaskService taskService) {
        this.taskService = taskService;
    }

    @Override public String getName() { return "get_task"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about a specific task by ID. "
               + "Returns the full details of a task: title, description, status, priority, "
               + "assignee, due date, and overdue flag. "
               + "Trigger phrases: 'tell me about task [id]', 'what is the status of task [id]', "
               + "'show task details for [id]', 'get task [id]'. "
               + "Parameters: taskId (required — the task UUID). "
               + "If you have a task ID from a previous search, use this tool to get full details. "
               + "Never fabricate task details — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{"taskId":{"type":"string","description":"Task UUID"}},
               "required":["taskId"]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String taskIdStr = arg(args, "taskId");
        if (taskIdStr == null) {
            return "Parameter 'taskId' is required.";
        }
        UUID taskId;
        try {
            taskId = UUID.fromString(taskIdStr);
        } catch (IllegalArgumentException e) {
            return "Invalid taskId format: " + taskIdStr;
        }

        TaskResponse t = taskService.findById(taskId);
        return String.format(
                "Task [%s]:\n  Title: %s\n  Status: %s%s\n  Priority: %s\n  Due: %s\n"
                + "  Assigned to: %s\n  Created by: %s\n  Description: %s",
                t.id(), t.title(), t.status(),
                t.overdue() ? " ⚠ OVERDUE" : "",
                t.priority(),
                t.dueDate() != null ? t.dueDate() : "N/A",
                t.assignedEmployeeName() != null ? t.assignedEmployeeName() : "Unassigned",
                t.createdByEmployeeName() != null ? t.createdByEmployeeName() : "N/A",
                t.description() != null ? t.description().substring(0, Math.min(200, t.description().length())) : "N/A"
        );
    }
}
