package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.config.GroqProperties;
import com.company.employeemanagement.ai.dto.TaskAiAnalysis;
import com.company.employeemanagement.dto.response.TaskAiReviewResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.TaskComment;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.enums.AiRecommendedAction;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.SubmissionStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import com.company.employeemanagement.repository.TaskCommentRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates AI-powered analysis of task submissions (Phase 7A).
 *
 * <h2>Flow</h2>
 * <ol>
 *   <li>Manager requests analysis for a submission ID.</li>
 *   <li>Service validates: submission exists, requester is privileged, no duplicate in-flight.</li>
 *   <li>A {@link TaskAiReview} record is created with status PENDING.</li>
 *   <li>Attachment text is extracted (if present).</li>
 *   <li>Prompt is constructed with all task/submission context, clearly labelled.</li>
 *   <li>Groq AI is called; response is parsed to {@link TaskAiAnalysis}.</li>
 *   <li>Review is updated with COMPLETED status and stored JSON.</li>
 *   <li>On any failure: review is marked FAILED with error details.</li>
 * </ol>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>Only ADMIN/HR/MANAGER may request or view AI reviews.</li>
 *   <li>EMPLOYEE may not request AI reviews.</li>
 *   <li>IDOR: submission must belong to the task (cross-submission access blocked).</li>
 *   <li>All employee-controlled content is labelled UNTRUSTED in the prompt.</li>
 *   <li>AI recommendation is advisory only — never auto-approves or rejects.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class TaskAiReviewService {

    private static final Logger log = LoggerFactory.getLogger(TaskAiReviewService.class);

    private final TaskSubmissionRepository submissionRepository;
    private final TaskAiReviewRepository  aiReviewRepository;
    private final TaskCommentRepository   commentRepository;
    private final SecurityUtils           securityUtils;
    private final GroqClient              groqClient;
    private final GroqProperties          groqProperties;
    private final SubmissionAttachmentExtractionService extractionService;
    private final ObjectMapper            objectMapper;
    private final NotificationService     notificationService;

    /**
     * Constructs the service with all required collaborators.
     */
    public TaskAiReviewService(
            final TaskSubmissionRepository submissionRepository,
            final TaskAiReviewRepository aiReviewRepository,
            final TaskCommentRepository commentRepository,
            final SecurityUtils securityUtils,
            final GroqClient groqClient,
            final GroqProperties groqProperties,
            final SubmissionAttachmentExtractionService extractionService,
            final ObjectMapper objectMapper,
            final NotificationService notificationService) {
        this.submissionRepository = submissionRepository;
        this.aiReviewRepository   = aiReviewRepository;
        this.commentRepository    = commentRepository;
        this.securityUtils        = securityUtils;
        this.groqClient           = groqClient;
        this.groqProperties       = groqProperties;
        this.extractionService    = extractionService;
        this.objectMapper         = objectMapper;
        this.notificationService  = notificationService;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Triggers an AI evaluation asynchronously for the given submission after
     * the transaction has committed.
     *
     * <p>This is the entry point for the automatic pipeline (Phase 7C). It differs
     * from {@link #requestReview(UUID)} in two ways:
     * <ol>
     *   <li>No security check — the caller (event listener) runs in a background
     *       thread after the submission transaction has committed; there is no
     *       HTTP security context at that point.</li>
     *   <li>The review is attributed to the task creator rather than the current
     *       HTTP user, since there is no HTTP user in the background thread.</li>
     * </ol>
     *
     * <p>Duplicate protection: if a PENDING or PROCESSING review already exists
     * for this submission, the method returns silently without creating a new one.
     *
     * @param submissionId UUID of the submission to evaluate
     */
    @Transactional
    public void triggerAutomaticReview(final UUID submissionId) {
        log.info("AI REVIEW QUEUED — automatic trigger: submissionId={}", submissionId);

        // Load the submission
        Optional<TaskSubmission> optionalSubmission =
                submissionRepository.findByIdWithAssociations(submissionId);
        if (optionalSubmission.isEmpty()) {
            log.warn("AI REVIEW QUEUED — submission not found (may have been deleted): submissionId={}",
                    submissionId);
            return;
        }
        TaskSubmission submission = optionalSubmission.get();

        // Duplicate protection: skip if already in-flight
        if (aiReviewRepository.existsBySubmissionIdAndStatus(submissionId, AiReviewStatus.PENDING)
                || aiReviewRepository.existsBySubmissionIdAndStatus(submissionId, AiReviewStatus.PROCESSING)) {
            log.info("AI REVIEW QUEUED — skipped, review already in-flight: submissionId={}", submissionId);
            return;
        }

        // Attribute the review to the task creator (manager who created the task)
        Employee requestedBy = resolveReviewRequester(submission);
        if (requestedBy == null) {
            log.error("AI REVIEW QUEUED — cannot create review: no requestedBy employee resolved for submissionId={}",
                    submissionId);
            return;
        }

        // Create PENDING review record
        TaskAiReview review = TaskAiReview.builder()
                .task(submission.getTask())
                .submission(submission)
                .requestedBy(requestedBy)
                .status(AiReviewStatus.PENDING)
                .aiProvider("groq")
                .aiModel(groqProperties.getModel())
                .promptVersion("v1")
                .build();
        review = aiReviewRepository.save(review);
        log.info("AI REVIEW QUEUED — created: reviewId={} submissionId={} taskId={}",
                review.getId(), submissionId,
                submission.getTask() != null ? submission.getTask().getId() : null);

        // Perform the analysis (updates status to PROCESSING → COMPLETED/FAILED)
        performAnalysis(review, submission);
    }


    /**
     * Requests an AI analysis for the given submission.
     *
     * <p>Blocks if a PENDING/PROCESSING review already exists for this submission,
     * returning a 409 conflict via {@link IllegalStateException}.
     *
     * @param submissionId the UUID of the submission to analyse
     * @return the newly created (PENDING or immediately COMPLETED) AI review response
     * @throws ResourceNotFoundException if the submission does not exist
     * @throws AccessDeniedException     if the current user is not ADMIN/HR/MANAGER
     * @throws IllegalStateException     if a review is already in progress for this submission
     */
    @Transactional
    public TaskAiReviewResponse requestReview(final UUID submissionId) {
        // ── 1. Validate access FIRST (before any data access) ────────────────
        Employee requester = requirePrivilegedEmployee();

        // ── 2. Load submission ───────────────────────────────────────────────
        TaskSubmission submission = loadSubmission(submissionId);

        // ── 3. Prevent duplicate in-flight reviews ───────────────────────────
        if (aiReviewRepository.existsBySubmissionIdAndStatus(submissionId, AiReviewStatus.PENDING)
                || aiReviewRepository.existsBySubmissionIdAndStatus(submissionId, AiReviewStatus.PROCESSING)) {
            throw new IllegalStateException(
                    "An AI review is already in progress for this submission. "
                    + "Please wait for it to complete before requesting a new one.");
        }

        // ── 3. Create the review record (PENDING) ────────────────────────────
        TaskAiReview review = TaskAiReview.builder()
                .task(submission.getTask())
                .submission(submission)
                .requestedBy(requester)
                .status(AiReviewStatus.PENDING)
                .aiProvider("groq")
                .aiModel(groqProperties.getModel())
                .promptVersion("v1")
                .build();
        review = aiReviewRepository.save(review);
        log.info("AI review requested: reviewId={} submissionId={} by={}",
                review.getId(), submissionId, requester.getId());

        // ── 4. Perform the analysis (synchronous for Phase 7A) ───────────────
        review = performAnalysis(review, submission);

        return toResponse(review);
    }

    /**
     * Returns the most recent AI review for the given submission.
     *
     * @param submissionId the submission UUID
     * @return the latest AI review
     * @throws AccessDeniedException     if the current user is not ADMIN/HR/MANAGER
     * @throws ResourceNotFoundException if no review exists for this submission
     */
    @Transactional(readOnly = true)
    public TaskAiReviewResponse getLatestReviewForSubmission(final UUID submissionId) {
        requirePrivilegedEmployee();

        // Verify submission exists
        if (!submissionRepository.existsById(submissionId)) {
            throw new ResourceNotFoundException("Task submission not found: " + submissionId);
        }

        TaskAiReview review = aiReviewRepository.findLatestBySubmissionId(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No AI review found for submission: " + submissionId));
        return toResponse(review);
    }

    /**
     * Returns all AI reviews for the given submission.
     *
     * @param submissionId the submission UUID
     * @return list of AI reviews, newest first
     * @throws AccessDeniedException if the current user is not ADMIN/HR/MANAGER
     */
    @Transactional(readOnly = true)
    public List<TaskAiReviewResponse> getAllReviewsForSubmission(final UUID submissionId) {
        requirePrivilegedEmployee();

        if (!submissionRepository.existsById(submissionId)) {
            throw new ResourceNotFoundException("Task submission not found: " + submissionId);
        }

        return aiReviewRepository
                .findAllBySubmissionIdOrderByCreatedAtDesc(submissionId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a specific AI review by its own UUID.
     *
     * @param reviewId the AI review UUID
     * @return the AI review
     * @throws AccessDeniedException     if the current user is not ADMIN/HR/MANAGER
     * @throws ResourceNotFoundException if the review does not exist
     */
    @Transactional(readOnly = true)
    public TaskAiReviewResponse getReviewById(final UUID reviewId) {
        requirePrivilegedEmployee();

        TaskAiReview review = aiReviewRepository.findByIdWithAssociations(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "AI review not found: " + reviewId));
        return toResponse(review);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Performs the AI analysis on the given review, updating it to COMPLETED or FAILED.
     */
    private TaskAiReview performAnalysis(TaskAiReview review, final TaskSubmission submission) {
        try {
            // ── Mark as PROCESSING ────────────────────────────────────────────
            review.setStatus(AiReviewStatus.PROCESSING);
            review = aiReviewRepository.save(review);

            // ── Build context ─────────────────────────────────────────────────
            String attachmentText = extractAttachmentText(submission);
            List<TaskComment> comments = commentRepository.findByTaskIdOrderByCreatedAtAsc(
                    submission.getTask().getId());

            Task task = submission.getTask();
            String systemPrompt = TaskReviewPromptBuilder.buildSystemPrompt();
            String contextMessage = TaskReviewPromptBuilder.buildContextMessage(
                    task, submission, attachmentText, comments);

            log.debug("AI review context built: approx {} chars (submission {})",
                    contextMessage.length(), submission.getId());

            // ── Call Groq ─────────────────────────────────────────────────────
            String rawJson = groqClient.chat(systemPrompt, contextMessage);
            log.debug("AI response received: approx {} chars", rawJson.length());

            // ── Parse response ────────────────────────────────────────────────
            TaskAiAnalysis analysis = parseAnalysis(rawJson);

            // ── Persist results ───────────────────────────────────────────────
            review.setStatus(AiReviewStatus.COMPLETED);
            review.setCompletionScore(clampScore(analysis.completionScore()));
            review.setQualityScore(
                    analysis.qualityAssessment() != null
                            ? clampScore(analysis.qualityAssessment().score())
                            : null);
            review.setConfidence(clampScore(analysis.confidence()));
            review.setRecommendedAction(parseRecommendedAction(analysis.recommendedAction()));
            review.setStructuredAnalysisJson(rawJson);
            review.setManagerSummary(analysis.managerSummary());
            review.setCompletedAt(LocalDateTime.now());
            review = aiReviewRepository.save(review);

            log.info("AI review completed: reviewId={} completionScore={} confidence={} action={}",
                    review.getId(), review.getCompletionScore(),
                    review.getConfidence(), review.getRecommendedAction());

            // ── Notify the requester ──────────────────────────────────
            sendCompletionNotification(review);

        } catch (Exception e) {
            log.error("AI review failed for reviewId={}: {}", review.getId(), e.getMessage(), e);
            review.setStatus(AiReviewStatus.FAILED);
            review.setErrorMessage(sanitiseError(e));
            review.setCompletedAt(LocalDateTime.now());
            review = aiReviewRepository.save(review);

            // ── Notify the requester of failure ───────────────────────
            sendFailureNotification(review);
        }
        return review;
    }

    /**
     * Extracts attachment text if the submission has one.
     */
    private String extractAttachmentText(final TaskSubmission submission) {
        if (!submission.hasAttachment()) {
            return null;
        }
        return extractionService.extractText(
                submission.getAttachmentStorageKey(),
                submission.getAttachmentMimeType(),
                submission.getAttachmentOriginalName());
    }

    /**
     * Parses the raw JSON string from Groq into a {@link TaskAiAnalysis}.
     * Attempts to strip markdown code fences if the model wrapped the JSON.
     */
    private TaskAiAnalysis parseAnalysis(final String rawJson) throws JsonProcessingException {
        String cleaned = rawJson.trim();
        // Strip common markdown code fence wrapping
        if (cleaned.startsWith("```")) {
            int firstNewline = cleaned.indexOf('\n');
            if (firstNewline >= 0) {
                cleaned = cleaned.substring(firstNewline + 1);
            }
            if (cleaned.endsWith("```")) {
                cleaned = cleaned.substring(0, cleaned.length() - 3).trim();
            }
        }
        return objectMapper.readValue(cleaned, TaskAiAnalysis.class);
    }

    /**
     * Maps the raw recommended action string to the enum, defaulting to MANUAL_REVIEW.
     */
    private AiRecommendedAction parseRecommendedAction(final String raw) {
        if (raw == null) return AiRecommendedAction.MANUAL_REVIEW;
        try {
            return AiRecommendedAction.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Unrecognised recommendedAction '{}' — defaulting to MANUAL_REVIEW", raw);
            return AiRecommendedAction.MANUAL_REVIEW;
        }
    }

    /** Clamps a score to the [0, 100] range. */
    private Integer clampScore(final int score) {
        return Math.max(0, Math.min(100, score));
    }

    /**
     * Loads a submission by ID, throwing {@link ResourceNotFoundException} if absent.
     */
    private TaskSubmission loadSubmission(final UUID submissionId) {
        return submissionRepository.findByIdWithAssociations(submissionId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Task submission not found: " + submissionId));
    }

    /**
     * Resolves the current employee and ensures they are ADMIN/HR/MANAGER.
     *
     * @throws AccessDeniedException if not privileged
     */
    private Employee requirePrivilegedEmployee() {
        if (!securityUtils.isPrivileged()) {
            throw new AccessDeniedException(
                    "Only managers, HR, and administrators may request or view AI task reviews.");
        }
        return securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record found for the current user."));
    }

    /**
     * Resolves the employee to attribute an automatic review to.
     *
     * <p>Prefers the task creator (manager who created the task). Falls back to
     * the submission's submitter in case the creator is not set. If neither is
     * available, returns {@code null} and logs a warning.
     *
     * @param submission the submission being reviewed
     * @return the employee to record as {@code requestedBy}, or {@code null}
     */
    private Employee resolveReviewRequester(final TaskSubmission submission) {
        if (submission.getTask() != null && submission.getTask().getCreatedByEmployee() != null) {
            return submission.getTask().getCreatedByEmployee();
        }
        if (submission.getSubmittedBy() != null) {
            log.warn("AI review requester fallback: task has no creator — using submitter for submissionId={}",
                    submission.getId());
            return submission.getSubmittedBy();
        }
        log.warn("AI review requester: neither task creator nor submitter is available — submissionId={}",
                submission.getId());
        return null;
    }

    /**
     * Produces a safe, single-line error description for persistence.
     */
    private String sanitiseError(final Exception e) {
        String type = e.getClass().getSimpleName();
        String message = e.getMessage() != null
                ? e.getMessage().lines().findFirst().orElse("no details")
                : "no details";
        // Limit length to avoid oversized DB column
        String full = type + ": " + message;
        return full.length() > 2000 ? full.substring(0, 2000) : full;
    }

    /**
     * Sends AI_REVIEW_COMPLETED notifications.
     *
     * <p>Phase 7D: Sends to BOTH:
     * <ol>
     *   <li>The requester (manager/HR/admin) — existing behaviour from Phase 7A/7C.</li>
     *   <li>The employee who submitted the task — new in Phase 7D, using employee-safe message.</li>
     * </ol>
     *
     * <p>Duplicate notifications are prevented: each recipient receives at most one notification
     * per review. The employee message is intentionally different from the manager message and
     * does NOT expose the recommended action or any manager-only details.
     *
     * <p>Failures here are logged but swallowed so they don't affect the main flow.
     */
    private void sendCompletionNotification(final TaskAiReview review) {
        try {
            if (review.getTask() == null) return;
            UUID taskId = review.getTask().getId();
            String taskTitle = review.getTask().getTitle();

            // Notify the requester (manager/HR/admin)
            if (review.getRequestedBy() != null) {
                String managerMsg = "AI evaluation for \"" + taskTitle + "\" is ready for review.";
                notificationService.createNotification(
                        review.getRequestedBy(),
                        NotificationType.AI_REVIEW_COMPLETED,
                        "AI Evaluation Completed",
                        managerMsg,
                        taskId);
            }

            // Notify the employee who submitted the task (Phase 7D)
            // Only notify if the submitter is different from the requester (avoid duplicate)
            Employee submitter = review.getSubmission() != null
                    ? review.getSubmission().getSubmittedBy() : null;
            if (submitter != null) {
                boolean sameAsRequester = review.getRequestedBy() != null
                        && submitter.getId().equals(review.getRequestedBy().getId());
                if (!sameAsRequester) {
                    String employeeMsg = "Your task submission has been evaluated by the AI assistant. "
                            + "View the feedback to see your strengths and areas for improvement.";
                    notificationService.createNotification(
                            submitter,
                            NotificationType.AI_REVIEW_COMPLETED,
                            "AI Feedback Available",
                            employeeMsg,
                            taskId);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to send AI review completion notification for reviewId={}: {}",
                    review.getId(), ex.getMessage());
        }
    }

    /**
     * Sends AI_REVIEW_FAILED notifications.
     *
     * <p>Phase 7D: Notifies the requester. Also notifies the employee with a friendly,
     * non-technical message (does NOT expose stack traces or API errors).
     *
     * <p>Failures here are logged but swallowed so they don't affect the main flow.
     */
    private void sendFailureNotification(final TaskAiReview review) {
        try {
            if (review.getTask() == null) return;
            UUID taskId = review.getTask().getId();
            String taskTitle = review.getTask().getTitle();

            // Notify the requester (manager/HR/admin)
            if (review.getRequestedBy() != null) {
                String managerMsg = "AI evaluation for \"" + taskTitle + "\" failed. You can retry from the task.";
                notificationService.createNotification(
                        review.getRequestedBy(),
                        NotificationType.AI_REVIEW_FAILED,
                        "AI Evaluation Failed",
                        managerMsg,
                        taskId);
            }

            // Notify the employee with a friendly, non-technical message (Phase 7D)
            // Does NOT expose: error details, stack traces, API provider messages
            Employee submitter = review.getSubmission() != null
                    ? review.getSubmission().getSubmittedBy() : null;
            if (submitter != null) {
                boolean sameAsRequester = review.getRequestedBy() != null
                        && submitter.getId().equals(review.getRequestedBy().getId());
                if (!sameAsRequester) {
                    String employeeMsg = "The AI evaluation of your submission could not be completed at this time. "
                            + "Your manager has been notified.";
                    notificationService.createNotification(
                            submitter,
                            NotificationType.AI_REVIEW_FAILED,
                            "AI Evaluation Unavailable",
                            employeeMsg,
                            taskId);
                }
            }
        } catch (Exception ex) {
            log.warn("Failed to send AI review failure notification for reviewId={}: {}",
                    review.getId(), ex.getMessage());
        }
    }

    /** Maps a {@link TaskAiReview} to its response DTO. */
    private TaskAiReviewResponse toResponse(final TaskAiReview review) {
        String requestedByName = "";
        if (review.getRequestedBy() != null && review.getRequestedBy().getUser() != null) {
            var user = review.getRequestedBy().getUser();
            requestedByName = user.getFirstName() + " " + user.getLastName();
        }
        return new TaskAiReviewResponse(
                review.getId(),
                review.getTask() != null ? review.getTask().getId() : null,
                review.getSubmission() != null ? review.getSubmission().getId() : null,
                review.getRequestedBy() != null ? review.getRequestedBy().getId() : null,
                requestedByName,
                review.getStatus(),
                review.getAiProvider(),
                review.getAiModel(),
                review.getPromptVersion(),
                review.getCompletionScore(),
                review.getQualityScore(),
                review.getConfidence(),
                review.getRecommendedAction(),
                review.getStructuredAnalysisJson(),
                review.getManagerSummary(),
                review.getErrorMessage(),
                review.getCreatedAt(),
                review.getCompletedAt()
        );
    }
}
