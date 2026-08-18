package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.request.CreateTaskCommentRequest;
import com.company.employeemanagement.dto.response.TaskCommentResponse;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.TaskCommentService;
import com.company.employeemanagement.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tool: add_task_comment
 * Proposes adding a comment to a task. Requires confirmation.
 * All roles (scoped by task access).
 */
@Component
public class AddTaskCommentTool extends AbstractAgentTool {

    private final TaskService taskService;
    private final TaskCommentService taskCommentService;
    private final AgentConfirmationStore confirmationStore;

    public AddTaskCommentTool(final TaskService taskService,
                               final TaskCommentService taskCommentService,
                               final AgentConfirmationStore confirmationStore) {
        this.taskService = taskService;
        this.taskCommentService = taskCommentService;
        this.confirmationStore = confirmationStore;
    }

    @Override public String getName() { return "add_task_comment"; }

    @Override public String getDescription() {
        return "Proposes adding a comment to a task. "
               + "Parameters: taskId (required UUID), comment (required — the comment text). "
               + "Returns a confirmation proposal. "
               + "Use this when asked to comment on a task.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "taskId":{"type":"string","description":"Task UUID"},
               "comment":{"type":"string","description":"Comment text to add"}},
               "required":["taskId","comment"]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE");
    }

    @Override public boolean isRequiresConfirmation() { return true; }
    @Override public boolean isReadOnly() { return false; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String taskIdStr = arg(args, "taskId");
        String comment = arg(args, "comment");

        if (taskIdStr == null) { return "Parameter 'taskId' is required."; }
        if (comment == null || comment.isBlank()) { return "Parameter 'comment' is required."; }

        UUID taskId;
        try { taskId = UUID.fromString(taskIdStr); }
        catch (IllegalArgumentException e) { return "Invalid taskId format."; }

        var task = taskService.findById(taskId);

        AgentActionProposal proposal = confirmationStore.createAndStore(
                "ADD_TASK_COMMENT",
                taskIdStr,
                Map.of("taskId", taskIdStr, "comment", comment),
                String.format("Add comment to task \"%s\": \"%s\"",
                        task.title(),
                        comment.length() > 100 ? comment.substring(0, 100) + "..." : comment),
                context.userId() != null ? context.userId().toString() : ""
        );

        return "CONFIRMATION_REQUIRED:" + proposal.token() + ":"
               + String.format(
                       "Add comment to task \"%s\":\n\"%s\"",
                       task.title(),
                       comment.length() > 200 ? comment.substring(0, 200) + "..." : comment);
    }
}
