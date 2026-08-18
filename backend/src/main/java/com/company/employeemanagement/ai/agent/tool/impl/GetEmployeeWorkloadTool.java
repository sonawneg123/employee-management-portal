package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.WorkloadResponse;
import com.company.employeemanagement.service.TaskService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tool: get_employee_workload
 * Returns the workload summary for all employees or a specific employee.
 * ADMIN, HR, MANAGER only.
 */
@Component
public class GetEmployeeWorkloadTool extends AbstractAgentTool {

    private final TaskService taskService;

    public GetEmployeeWorkloadTool(final TaskService taskService) {
        this.taskService = taskService;
    }

    @Override public String getName() { return "get_employee_workload"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about employee workload, task load, "
               + "who is overloaded, who has capacity, or who has the most/least tasks. "
               + "Trigger phrases: 'who has the highest workload', 'who is overloaded', "
               + "'who has capacity for a new task', 'what is [employee]'s workload', "
               + "'who has the most active tasks', 'workload summary'. "
               + "Returns active tasks, overdue tasks, pending reviews, and workload level per employee. "
               + "Parameters: employeeId (optional UUID — omit to get all employees). "
               + "Never guess workload from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{"employeeId":{"type":"string","description":"Employee UUID (optional, omit for all)"}},
               "required":[]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String employeeId = arg(args, "employeeId");

        if (employeeId != null) {
            try {
                java.util.UUID empId = java.util.UUID.fromString(employeeId);
                WorkloadResponse w = taskService.getWorkload(empId);
                return formatWorkload(w);
            } catch (IllegalArgumentException e) {
                return "Invalid employeeId format: " + employeeId;
            }
        }

        // All employees
        List<WorkloadResponse> all = taskService.getWorkloadSummary();
        if (all.isEmpty()) {
            return "No workload data available.";
        }
        return "Workload summary (" + all.size() + " employees):\n" +
               all.stream().map(this::formatWorkload).collect(Collectors.joining("\n"));
    }

    private String formatWorkload(final WorkloadResponse w) {
        return String.format(
                "%s (id=%s) — Active: %d, Overdue: %d, Pending review: %d, Level: %s",
                w.employeeName(), w.employeeId(),
                w.activeTasks(), w.pendingReview(), w.overdue(),
                w.workloadLevel()
        );
    }
}
