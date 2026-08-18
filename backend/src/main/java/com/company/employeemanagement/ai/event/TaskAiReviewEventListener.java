package com.company.employeemanagement.ai.event;

import com.company.employeemanagement.ai.service.TaskAiReviewService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listens for {@link TaskSubmissionAiEvent} and asynchronously triggers AI evaluation.
 *
 * <h2>Transaction safety</h2>
 * <p>The listener is bound to {@link TransactionPhase#AFTER_COMMIT}, which means it
 * only fires after the submission transaction has successfully committed. If the
 * submission transaction rolls back, no AI evaluation is started — guaranteeing that
 * the database always has a committed submission record before the AI pipeline reads it.
 *
 * <h2>Asynchronous execution</h2>
 * <p>The {@code @Async("aiReviewExecutor")} annotation causes Spring to execute the
 * handler on the dedicated {@code aiReviewExecutor} thread pool rather than the
 * calling HTTP thread. The employee's HTTP response is returned immediately; the
 * Groq call runs in the background.
 *
 * <h2>Duplicate protection</h2>
 * <p>Duplicate protection is enforced inside
 * {@link TaskAiReviewService#triggerAutomaticReview(java.util.UUID)}: if a PENDING or
 * PROCESSING review already exists for the submission, the method returns silently.
 *
 * @author Employee Management Portal Team
 */
@Component
public class TaskAiReviewEventListener {

    private static final Logger log = LoggerFactory.getLogger(TaskAiReviewEventListener.class);

    private final TaskAiReviewService aiReviewService;

    /**
     * Constructs the listener with its required service dependency.
     *
     * @param aiReviewService the AI review orchestration service
     */
    public TaskAiReviewEventListener(final TaskAiReviewService aiReviewService) {
        this.aiReviewService = aiReviewService;
    }

    /**
     * Handles a {@link TaskSubmissionAiEvent} after the submission transaction commits.
     *
     * <p>Executes on the {@code aiReviewExecutor} thread pool, never on the HTTP
     * request thread. Any exception thrown here is logged but does not affect
     * the employee's submission response.
     *
     * @param event the event carrying the submission and task UUIDs
     */
    @Async("aiReviewExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSubmissionCreated(final TaskSubmissionAiEvent event) {
        log.info("AI REVIEW EVENT — received: submissionId={} taskId={}",
                event.submissionId(), event.taskId());
        try {
            aiReviewService.triggerAutomaticReview(event.submissionId());
        } catch (Exception e) {
            // Exceptions here must never propagate back to the employee request
            log.error("AI REVIEW EVENT — unhandled error for submissionId={}: {}",
                    event.submissionId(), e.getMessage(), e);
        }
    }
}
