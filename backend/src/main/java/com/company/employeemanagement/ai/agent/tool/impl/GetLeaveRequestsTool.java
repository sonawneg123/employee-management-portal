package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.service.LeaveRequestService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tool: get_leave_requests
 * Returns leave requests with optional filtering.
 * Employees can only see their own leaves; managers/HR/admins can see all.
 */
@Component
public class GetLeaveRequestsTool extends AbstractAgentTool {

    private final LeaveRequestService leaveRequestService;

    public GetLeaveRequestsTool(final LeaveRequestService leaveRequestService) {
        this.leaveRequestService = leaveRequestService;
    }

    @Override public String getName() { return "get_leave_requests"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about leave requests — who is on leave, "
               + "pending leave approvals, an employee's leave history, or their own leave status. "
               + "Trigger phrases: 'who is on leave', 'who is on leave today', 'pending leave requests', "
               + "'my leave requests', 'show leave history for [name]', 'approved leaves', "
               + "'who has requested leave'. "
               + "Parameters: employeeId (optional UUID — managers/HR only), "
               + "status (optional: PENDING/APPROVED/REJECTED/CANCELLED). "
               + "Employees automatically see only their own leave requests. "
               + "Never answer leave questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "employeeId":{"type":"string","description":"Employee UUID"},
               "status":{"type":"string","description":"Leave status filter: PENDING/APPROVED/REJECTED/CANCELLED"}},
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
        String employeeIdStr = arg(args, "employeeId");

        LeaveStatus status = null;
        if (statusStr != null) {
            try { status = LeaveStatus.valueOf(statusStr.toUpperCase()); }
            catch (IllegalArgumentException e) { return "Invalid status: " + statusStr; }
        }

        UUID employeeId = null;
        if (employeeIdStr != null) {
            try { employeeId = UUID.fromString(employeeIdStr); }
            catch (IllegalArgumentException e) { return "Invalid employeeId: " + employeeIdStr; }
        }

        boolean isEmployee = context.roles().contains("ROLE_EMPLOYEE") &&
                             !context.roles().contains("ROLE_ADMIN") &&
                             !context.roles().contains("ROLE_HR") &&
                             !context.roles().contains("ROLE_MANAGER");

        PageResponse<LeaveRequestResponse> page;
        if (isEmployee) {
            // Employees always see only their own
            page = leaveRequestService.findMyLeaves(status, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startDate")));
        } else {
            page = leaveRequestService.findAll(employeeId, status, null,
                    PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "startDate")));
        }

        if (page.content().isEmpty()) {
            return "No leave requests found.";
        }

        return "Leave requests (" + page.totalElements() + " total):\n" +
               page.content().stream().map(this::formatLeave).collect(Collectors.joining("\n"));
    }

    private String formatLeave(final LeaveRequestResponse l) {
        return String.format("[%s] %s | Type: %s | %s → %s (%d days) | Status: %s%s",
                l.id(), l.employeeName(), l.leaveType(),
                l.startDate(), l.endDate(), l.totalDays(), l.status(),
                l.rejectionReason() != null ? " | Reason: " + l.rejectionReason() : "");
    }
}
