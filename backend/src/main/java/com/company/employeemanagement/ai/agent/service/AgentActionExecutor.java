package com.company.employeemanagement.ai.agent.service;

import com.company.employeemanagement.ai.agent.dto.AgentActionProposal;
import com.company.employeemanagement.dto.request.CreateTaskCommentRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.ReviewLeaveRequest;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.TaskCommentResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.service.LeaveRequestService;
import com.company.employeemanagement.service.TaskCommentService;
import com.company.employeemanagement.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Executes confirmed agent action proposals by delegating to existing application services.
 *
 * <p>This class is the ONLY place where confirmed AI-agent actions are dispatched.
 * Every method delegates to the existing service layer with full validation,
 * authorization, and business-rule enforcement.
 *
 * <p>The AI can NEVER bypass these methods to touch the database directly.
 *
 * @author Employee Management Portal Team
 */
@Service
public class AgentActionExecutor {

    private static final Logger log = LoggerFactory.getLogger(AgentActionExecutor.class);

    private final TaskService taskService;
    private final LeaveRequestService leaveRequestService;
    private final TaskCommentService taskCommentService;

    public AgentActionExecutor(final TaskService taskService,
                                final LeaveRequestService leaveRequestService,
                                final TaskCommentService taskCommentService) {
        this.taskService = taskService;
        this.leaveRequestService = leaveRequestService;
        this.taskCommentService = taskCommentService;
    }

    /**
     * Executes a confirmed action proposal.
     *
     * @param proposal the confirmed action proposal
     * @return a human-readable result summary
     */
    public String execute(final AgentActionProposal proposal) {
        log.info("ACTION EXECUTE action={} resource={}", proposal.actionType(), proposal.resourceId());
        return switch (proposal.actionType()) {
            case "REASSIGN_TASK" -> executeReassignTask(proposal);
            case "APPROVE_LEAVE" -> executeApproveLeave(proposal);
            case "REJECT_LEAVE" -> executeRejectLeave(proposal);
            case "ADD_TASK_COMMENT" -> executeAddTaskComment(proposal);
            default -> {
                log.warn("Unknown action type: {}", proposal.actionType());
                yield "Unknown action type: " + proposal.actionType();
            }
        };
    }

    private String executeReassignTask(final AgentActionProposal proposal) {
        UUID taskId = UUID.fromString(proposal.resourceId());
        UUID newEmployeeId = UUID.fromString(proposal.parameters().get("newEmployeeId"));
        String reason = proposal.parameters().getOrDefault("reason", "Reassigned via AI Copilot");

        TaskResponse result = taskService.reassign(taskId,
                new ReassignTaskRequest(newEmployeeId, reason));

        log.info("ACTION COMPLETED REASSIGN_TASK taskId={} newAssignee={}",
                taskId, result.assignedEmployeeName());
        return String.format(
                "Task \"%s\" has been reassigned to %s. Notifications have been sent.",
                result.title(), result.assignedEmployeeName());
    }

    private String executeApproveLeave(final AgentActionProposal proposal) {
        UUID leaveId = UUID.fromString(proposal.resourceId());

        // ReviewLeaveRequest uses rejectionReason for reject; for approve we pass null
        LeaveRequestResponse result = leaveRequestService.approve(leaveId, new ReviewLeaveRequest(null));

        log.info("ACTION COMPLETED APPROVE_LEAVE leaveId={} employee={}",
                leaveId, result.employeeName());
        return String.format(
                "Leave request for %s (%s, %s to %s) has been approved. The employee has been notified.",
                result.employeeName(), result.leaveType(), result.startDate(), result.endDate());
    }

    private String executeRejectLeave(final AgentActionProposal proposal) {
        UUID leaveId = UUID.fromString(proposal.resourceId());
        String reason = proposal.parameters().getOrDefault("reason", "Rejected via AI Copilot");

        LeaveRequestResponse result = leaveRequestService.reject(leaveId, new ReviewLeaveRequest(reason)); // rejectionReason

        log.info("ACTION COMPLETED REJECT_LEAVE leaveId={} employee={}",
                leaveId, result.employeeName());
        return String.format(
                "Leave request for %s (%s, %s to %s) has been rejected. The employee has been notified.",
                result.employeeName(), result.leaveType(), result.startDate(), result.endDate());
    }

    private String executeAddTaskComment(final AgentActionProposal proposal) {
        UUID taskId = UUID.fromString(proposal.resourceId());
        String comment = proposal.parameters().get("comment");

        TaskCommentResponse result = taskCommentService.create(taskId,
                new CreateTaskCommentRequest(comment));

        log.info("ACTION COMPLETED ADD_TASK_COMMENT taskId={}", taskId);
        return String.format("Comment added to task. Comment ID: %s", result.id());
    }
}
