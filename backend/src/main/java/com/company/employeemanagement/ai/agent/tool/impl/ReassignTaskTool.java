package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.EmployeeService;
import com.company.employeemanagement.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tool: reassign_task
 * Proposes reassigning a task to a different employee.
 * Requires confirmation before execution.
 * ADMIN, HR, MANAGER only.
 *
 * <p>The tool validates the proposal using existing TaskService rules.
 * If the target employee is not available (not checked in, on leave, disabled),
 * the proposal is rejected before confirmation is even offered.
 */
@Component
public class ReassignTaskTool extends AbstractAgentTool {

    private final TaskService taskService;
    private final EmployeeService employeeService;
    private final AgentConfirmationStore confirmationStore;

    public ReassignTaskTool(final TaskService taskService,
                             final EmployeeService employeeService,
                             final AgentConfirmationStore confirmationStore) {
        this.taskService = taskService;
        this.employeeService = employeeService;
        this.confirmationStore = confirmationStore;
    }

    @Override public String getName() { return "reassign_task"; }

    @Override public String getDescription() {
        return "Proposes reassigning a task to a different employee. "
               + "Parameters: taskId (required UUID), newEmployeeId (required UUID), reason (optional string). "
               + "Returns a confirmation proposal — the user must confirm before the reassignment is executed. "
               + "Use this when asked to reassign a task. Always check employee availability first.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "taskId":{"type":"string","description":"Task UUID"},
               "newEmployeeId":{"type":"string","description":"New assignee employee UUID"},
               "reason":{"type":"string","description":"Optional reason for reassignment"}},
               "required":["taskId","newEmployeeId"]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER");
    }

    @Override public boolean isRequiresConfirmation() { return true; }
    @Override public boolean isReadOnly() { return false; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String taskIdStr = arg(args, "taskId");
        String newEmployeeIdStr = arg(args, "newEmployeeId");
        String reason = arg(args, "reason", "Reassigned via AI Copilot");

        if (taskIdStr == null || newEmployeeIdStr == null) {
            return "Parameters 'taskId' and 'newEmployeeId' are required.";
        }

        UUID taskId, newEmployeeId;
        try {
            taskId = UUID.fromString(taskIdStr);
            newEmployeeId = UUID.fromString(newEmployeeIdStr);
        } catch (IllegalArgumentException e) {
            return "Invalid UUID format in taskId or newEmployeeId.";
        }

        // Validate task exists and caller can access it
        TaskResponse task = taskService.findById(taskId);

        // Validate new employee exists
        var newEmployee = employeeService.findById(newEmployeeId);

        // Check availability to give meaningful feedback before proposing
        List<EmployeeAvailabilityResponse> availability = taskService.getEmployeeAvailability();
        EmployeeAvailabilityResponse targetAvailability = availability.stream()
                .filter(a -> a.employeeId().equals(newEmployeeId))
                .findFirst()
                .orElse(null);

        if (targetAvailability != null && !targetAvailability.availableToday()) {
            String reason2 = targetAvailability.unavailabilityReason() != null
                    ? targetAvailability.unavailabilityReason()
                    : "not currently available";
            return String.format(
                    "Cannot reassign to %s %s — they are %s today. "
                    + "Please choose an available employee.",
                    newEmployee.firstName(), newEmployee.lastName(), reason2);
        }

        // Create a structured confirmation proposal
        AgentActionProposal proposal = confirmationStore.createAndStore(
                "REASSIGN_TASK",
                taskIdStr,
                Map.of(
                        "taskId", taskIdStr,
                        "newEmployeeId", newEmployeeIdStr,
                        "reason", reason
                ),
                String.format("Reassign task \"%s\" from %s to %s %s",
                        task.title(),
                        task.assignedEmployeeName() != null ? task.assignedEmployeeName() : "Unassigned",
                        newEmployee.firstName(), newEmployee.lastName()),
                context.userId() != null ? context.userId().toString() : ""
        );

        return "CONFIRMATION_REQUIRED:" + proposal.token() + ":"
               + String.format(
                       "Reassign task \"%s\" (currently assigned to: %s) to %s %s (%s)?",
                       task.title(),
                       task.assignedEmployeeName() != null ? task.assignedEmployeeName() : "Unassigned",
                       newEmployee.firstName(), newEmployee.lastName(), newEmployee.employeeCode());
    }
}
