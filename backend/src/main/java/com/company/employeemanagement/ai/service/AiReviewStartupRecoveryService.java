package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Startup recovery component for stale AI reviews (Phase 7C).
 *
 * <p>On application startup, finds any {@link TaskAiReview} records that are still in
 * {@code PENDING} or {@code PROCESSING} state — i.e., reviews that were in-flight when
 * the application last crashed or was restarted. These are re-queued for processing so
 * that they eventually complete rather than remaining stuck indefinitely.
 *
 * <p>The recovery runs asynchronously after the application context is fully ready to
 * avoid delaying startup and to ensure all Spring beans are available.
 *
 * <h2>Safety</h2>
 * <ul>
 *   <li>Reviews in {@code COMPLETED} or {@code FAILED} state are never re-processed.</li>
 *   <li>If a review was left in PROCESSING (i.e., mid-call), it will be re-run. The
 *       completed result from the previous run (if any) is not lost because a new review
 *       record is only created once — the existing PENDING/PROCESSING record is reused.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Component
public class AiReviewStartupRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(AiReviewStartupRecoveryService.class);

    private final TaskAiReviewRepository aiReviewRepository;
    private final TaskAiReviewService    aiReviewService;

    /**
     * Constructs the recovery service.
     *
     * @param aiReviewRepository repository for finding stale reviews
     * @param aiReviewService    service for processing reviews
     */
    public AiReviewStartupRecoveryService(final TaskAiReviewRepository aiReviewRepository,
                                           final TaskAiReviewService aiReviewService) {
        this.aiReviewRepository = aiReviewRepository;
        this.aiReviewService    = aiReviewService;
    }

    /**
     * Triggered when the application context is fully ready.
     *
     * <p>Finds all reviews stuck in PENDING or PROCESSING and re-queues them for
     * evaluation via the existing async service. Runs on the {@code aiReviewExecutor}
     * thread pool so that startup is not blocked.
     */
    @Async("aiReviewExecutor")
    @EventListener(ApplicationReadyEvent.class)
    public void recoverStaleReviews() {
        try {
            List<TaskAiReview> stale = aiReviewRepository.findAllByStatusIn(
                    List.of(AiReviewStatus.PENDING, AiReviewStatus.PROCESSING));

            if (stale.isEmpty()) {
                log.info("AI REVIEW RECOVERY — no stale reviews found.");
                return;
            }

            log.warn("AI REVIEW RECOVERY — found {} stale review(s) to recover.", stale.size());
            for (TaskAiReview review : stale) {
                try {
                    log.info("AI REVIEW RECOVERY — re-processing reviewId={} submissionId={} status={}",
                            review.getId(),
                            review.getSubmission() != null ? review.getSubmission().getId() : null,
                            review.getStatus());
                    // Reset to PENDING so the analysis re-runs cleanly
                    review.setStatus(AiReviewStatus.PENDING);
                    aiReviewRepository.save(review);
                    // Re-trigger via submission ID
                    if (review.getSubmission() != null) {
                        aiReviewService.triggerAutomaticReview(review.getSubmission().getId());
                    }
                } catch (Exception e) {
                    log.error("AI REVIEW RECOVERY — failed to recover reviewId={}: {}",
                            review.getId(), e.getMessage(), e);
                }
            }
        } catch (Exception e) {
            log.error("AI REVIEW RECOVERY — startup recovery failed: {}", e.getMessage(), e);
        }
    }
}
