package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool: get_employee_availability
 * Returns current employee availability (check-in status, active tasks, on-leave flag).
 * ADMIN, HR, MANAGER only.
 */
@Component
public class GetEmployeeAvailabilityTool extends AbstractAgentTool {

    private final TaskService taskService;

    public GetEmployeeAvailabilityTool(final TaskService taskService) {
        this.taskService = taskService;
    }

    @Override public String getName() { return "get_employee_availability"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about employee availability — "
               + "who is available to receive a new task, who is currently working, "
               + "who is checked in today, or who is not available. "
               + "Trigger phrases: 'who is available today', 'who can I assign a task to', "
               + "'who is currently working', 'who is free for a task', "
               + "'who is checked in', 'available employees'. "
               + "Returns check-in status, on-leave flag, active task count, and workload level for all employees. "
               + "This is the ONLY tool for availability — do NOT compute availability from other tools. "
               + "Never answer availability questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() { return "{}"; }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        List<EmployeeAvailabilityResponse> availability = taskService.getEmployeeAvailability();
        if (availability.isEmpty()) {
            return "No employee availability data available.";
        }

        return "Employee availability (" + availability.size() + " employees):\n" +
               availability.stream().map(this::formatAvailability).collect(Collectors.joining("\n"));
    }

    private String formatAvailability(final EmployeeAvailabilityResponse a) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("%s (%s) | Tasks: %d | Available: %s",
                a.employeeName(), a.employeeCode(),
                a.activeTasks(), a.availableToday() ? "YES" : "NO"));
        if (a.checkedIn()) {
            sb.append(" | ✓ Checked in");
        } else {
            sb.append(" | ✗ Not checked in");
        }
        if (a.onApprovedLeaveToday()) {
            sb.append(" | ON LEAVE");
        }
        if (a.disabled()) {
            sb.append(" | DISABLED");
        }
        if (a.unavailabilityReason() != null) {
            sb.append(" | Reason: ").append(a.unavailabilityReason());
        }
        return sb.toString();
    }
}
