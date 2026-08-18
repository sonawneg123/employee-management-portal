package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.service.LeaveRequestService;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Tool: reject_leave
 * Proposes rejecting a leave request. Requires confirmation.
 * ADMIN, HR, MANAGER only.
 */
@Component
public class RejectLeaveRequestTool extends AbstractAgentTool {

    private final LeaveRequestService leaveRequestService;
    private final AgentConfirmationStore confirmationStore;

    public RejectLeaveRequestTool(final LeaveRequestService leaveRequestService,
                                   final AgentConfirmationStore confirmationStore) {
        this.leaveRequestService = leaveRequestService;
        this.confirmationStore = confirmationStore;
    }

    @Override public String getName() { return "reject_leave"; }

    @Override public String getDescription() {
        return "Proposes rejecting a leave request. "
               + "Parameters: leaveId (required UUID), reason (required — must provide a business reason). "
               + "Returns a confirmation proposal. "
               + "Use this when asked to reject a leave request.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "leaveId":{"type":"string","description":"Leave request UUID"},
               "reason":{"type":"string","description":"Business reason for rejection"}},
               "required":["leaveId","reason"]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER");
    }

    @Override public boolean isRequiresConfirmation() { return true; }
    @Override public boolean isReadOnly() { return false; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String leaveIdStr = arg(args, "leaveId");
        String reason = arg(args, "reason");

        if (leaveIdStr == null) { return "Parameter 'leaveId' is required."; }
        if (reason == null || reason.isBlank()) { return "Parameter 'reason' is required for rejection."; }

        UUID leaveId;
        try { leaveId = UUID.fromString(leaveIdStr); }
        catch (IllegalArgumentException e) { return "Invalid leaveId format."; }

        LeaveRequestResponse leave = leaveRequestService.findById(leaveId);

        if (!"PENDING".equals(leave.status().name())) {
            return String.format(
                    "Leave request for %s is already in status %s and cannot be rejected.",
                    leave.employeeName(), leave.status());
        }

        AgentActionProposal proposal = confirmationStore.createAndStore(
                "REJECT_LEAVE",
                leaveIdStr,
                Map.of("leaveId", leaveIdStr, "reason", reason),
                String.format("Reject leave request for %s (%s: %s → %s). Reason: %s",
                        leave.employeeName(), leave.leaveType(), leave.startDate(), leave.endDate(), reason),
                context.userId() != null ? context.userId().toString() : ""
        );

        return "CONFIRMATION_REQUIRED:" + proposal.token() + ":"
               + String.format(
                       "Reject %s leave for %s (%s to %s)? Reason: %s",
                       leave.leaveType(), leave.employeeName(),
                       leave.startDate(), leave.endDate(), reason);
    }
}
