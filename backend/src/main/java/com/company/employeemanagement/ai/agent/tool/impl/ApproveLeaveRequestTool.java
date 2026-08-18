package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.service.LeaveRequestService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tool: approve_leave
 * Proposes approving a leave request. Requires confirmation.
 * ADMIN, HR, MANAGER only.
 */
@Component
public class ApproveLeaveRequestTool extends AbstractAgentTool {

    private final LeaveRequestService leaveRequestService;
    private final AgentConfirmationStore confirmationStore;

    public ApproveLeaveRequestTool(final LeaveRequestService leaveRequestService,
                                    final AgentConfirmationStore confirmationStore) {
        this.leaveRequestService = leaveRequestService;
        this.confirmationStore = confirmationStore;
    }

    @Override public String getName() { return "approve_leave"; }

    @Override public String getDescription() {
        return "Proposes approving a leave request. "
               + "Parameters: leaveId (required UUID), comment (optional). "
               + "Returns a confirmation proposal. "
               + "Use this when asked to approve a leave request.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "leaveId":{"type":"string","description":"Leave request UUID"},
               "comment":{"type":"string","description":"Optional approval comment"}},
               "required":["leaveId"]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER");
    }

    @Override public boolean isRequiresConfirmation() { return true; }
    @Override public boolean isReadOnly() { return false; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String leaveIdStr = arg(args, "leaveId");
        String comment = arg(args, "comment", "Approved via AI Copilot");

        if (leaveIdStr == null) {
            return "Parameter 'leaveId' is required.";
        }
        UUID leaveId;
        try { leaveId = UUID.fromString(leaveIdStr); }
        catch (IllegalArgumentException e) { return "Invalid leaveId format."; }

        LeaveRequestResponse leave = leaveRequestService.findById(leaveId);

        if (!"PENDING".equals(leave.status().name())) {
            return String.format(
                    "Leave request for %s is already in status %s and cannot be approved.",
                    leave.employeeName(), leave.status());
        }

        AgentActionProposal proposal = confirmationStore.createAndStore(
                "APPROVE_LEAVE",
                leaveIdStr,
                Map.of("leaveId", leaveIdStr, "comment", comment),
                String.format("Approve leave request for %s (%s: %s → %s)",
                        leave.employeeName(), leave.leaveType(), leave.startDate(), leave.endDate()),
                context.userId() != null ? context.userId().toString() : ""
        );

        return "CONFIRMATION_REQUIRED:" + proposal.token() + ":"
               + String.format(
                       "Approve %s leave for %s (%s to %s, %d days)?",
                       leave.leaveType(), leave.employeeName(),
                       leave.startDate(), leave.endDate(), leave.totalDays());
    }
}
