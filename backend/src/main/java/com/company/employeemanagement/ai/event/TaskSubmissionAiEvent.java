package com.company.employeemanagement.ai.event;

import java.util.UUID;

/**
 * Domain event published after a task submission is successfully persisted.
 *
 * <p>Published by {@link com.company.employeemanagement.service.impl.TaskSubmissionServiceImpl}
 * after both {@code createSubmission} and {@code resubmit} succeed and the containing
 * transaction commits. Handled asynchronously by
 * {@link com.company.employeemanagement.ai.event.TaskAiReviewEventListener}.
 *
 * <p>Carries only the submission UUID — all other context is reloaded inside the
 * async handler to avoid holding stale entity references across thread boundaries.
 *
 * @param submissionId UUID of the newly created or updated submission
 * @param taskId       UUID of the parent task (for log correlation)
 *
 * @author Employee Management Portal Team
 */
public record TaskSubmissionAiEvent(UUID submissionId, UUID taskId) {
}
