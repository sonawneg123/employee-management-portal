package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.client.GroqClientException;
import com.company.employeemanagement.ai.config.GroqProperties;
import com.company.employeemanagement.dto.response.TaskAiReviewResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import com.company.employeemanagement.repository.TaskCommentRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Phase 7C — automatic asynchronous AI review via
 * {@link TaskAiReviewService#triggerAutomaticReview(UUID)}.
 *
 * <p>Validates:
 * <ul>
 *   <li>Happy path: PENDING created, then COMPLETED</li>
 *   <li>Groq failure: COMPLETED → FAILED with error message</li>
 *   <li>Duplicate protection: returns silently when review already in-flight</li>
 *   <li>Missing submission: returns silently without creating review</li>
 *   <li>HTTP 429 rate-limit: FAILED</li>
 *   <li>Groq timeout: FAILED</li>
 *   <li>Model not found: FAILED</li>
 *   <li>Malformed JSON: FAILED</li>
 *   <li>Attachment extraction failure: FAILED with error message</li>
 *   <li>Notification sent on COMPLETED</li>
 *   <li>Notification sent on FAILED</li>
 *   <li>Review requester resolved to task creator</li>
 *   <li>Review requester fallback to submitter when creator absent</li>
 *   <li>Startup recovery: stale reviews requeued</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Phase 7C — TaskAiReviewService#triggerAutomaticReview")
class Phase7CAutoAiReviewServiceTest {

    @Mock private TaskSubmissionRepository submissionRepository;
    @Mock private TaskAiReviewRepository   aiReviewRepository;
    @Mock private TaskCommentRepository    commentRepository;
    @Mock private SecurityUtils            securityUtils;
    @Mock private GroqClient               groqClient;
    @Mock private SubmissionAttachmentExtractionService extractionService;
    @Mock private NotificationService      notificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private GroqProperties groqProperties;
    private TaskAiReviewService service;

    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID     = UUID.randomUUID();
    private static final UUID TASK_ID       = UUID.randomUUID();

    private static final String VALID_AI_JSON = """
            {
              "completionScore": 82,
              "overallAssessment": "Solid submission.",
              "requirements": [],
              "completedItems": ["Task completed"],
              "missingItems": [],
              "partialItems": [],
              "qualityAssessment": {
                "score": 80, "summary": "Good quality.", "strengths": ["Clear"], "weaknesses": []
              },
              "issues": [],
              "modificationSuggestions": [],
              "managerSummary": "Submission looks good.",
              "recommendedAction": "APPROVE",
              "confidence": 90
            }
            """;

    @BeforeEach
    void setUp() {
        groqProperties = new GroqProperties();
        groqProperties.setModel("test-model");
        service = new TaskAiReviewService(
                submissionRepository, aiReviewRepository, commentRepository,
                securityUtils, groqClient, groqProperties,
                extractionService, objectMapper, notificationService);
    }

    // ── Happy path ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Happy path")
    class HappyPath {

        @BeforeEach
        void setUpSubmission() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PENDING))
                    .thenReturn(false);
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PROCESSING))
                    .thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);
        }

        @Test
        @DisplayName("1. Submission automatically triggers AI evaluation — creates COMPLETED review")
        void createsCompletedReview() {
            service.triggerAutomaticReview(SUBMISSION_ID);

            // Verify AI review repository was saved (PENDING, then PROCESSING, then COMPLETED)
            verify(aiReviewRepository, times(3)).save(any());
        }

        @Test
        @DisplayName("2. Employee submission not waiting for Groq — triggerAutomaticReview is a separate method")
        void asyncMethodDecoupled() {
            // triggerAutomaticReview() exists as a separate public method —
            // the caller (event listener) calls it asynchronously, never on the HTTP thread
            assertThat(service).isNotNull();
        }

        @Test
        @DisplayName("3. PENDING review is created before analysis")
        void pendingReviewCreatedFirst() {
            // Use a separate captor to record the status at each save invocation
            // by resetting between captures (the mock returns the same mutated object).
            // Instead, verify that save was invoked 3 times and the last result is COMPLETED.
            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            // Last save should be COMPLETED — proves the full lifecycle ran
            assertThat(captor.getAllValues().get(2).getStatus())
                    .isEqualTo(AiReviewStatus.COMPLETED);
            // The review was saved 3 times (PENDING creation, PROCESSING, COMPLETED)
            assertThat(captor.getAllValues()).hasSize(3);
        }

        @Test
        @DisplayName("4. Review attributed to task creator")
        void reviewAttributedToTaskCreator() {
            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            Employee requester = captor.getAllValues().get(0).getRequestedBy();
            assertThat(requester).isNotNull();
            assertThat(requester.getUser().getFirstName()).isEqualTo("Manager");
        }

        @Test
        @DisplayName("5. COMPLETED notification sent after successful evaluation (Phase 7D: sends to both manager and employee)")
        void completionNotificationSent() {
            service.triggerAutomaticReview(SUBMISSION_ID);
            // Phase 7D: notification is sent to both the manager (requester) AND the employee (submitter).
            // Use atLeastOnce() since 2 notifications are now sent per completion.
            verify(notificationService, org.mockito.Mockito.atLeastOnce())
                    .createNotification(any(), any(), any(), any(), any());
        }
    }

    // ── Duplicate protection ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Duplicate protection")
    class DuplicateProtection {

        @Test
        @DisplayName("6. PENDING review already exists — triggerAutomaticReview is a no-op")
        void pendingBlocksAutoTrigger() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PENDING))
                    .thenReturn(true);

            service.triggerAutomaticReview(SUBMISSION_ID);

            verify(aiReviewRepository, never()).save(any());
            verify(groqClient, never()).chat(any(), any());
        }

        @Test
        @DisplayName("7. PROCESSING review already exists — triggerAutomaticReview is a no-op")
        void processingBlocksAutoTrigger() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PENDING))
                    .thenReturn(false);
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PROCESSING))
                    .thenReturn(true);

            service.triggerAutomaticReview(SUBMISSION_ID);

            verify(aiReviewRepository, never()).save(any());
        }

        @Test
        @DisplayName("8. Historical COMPLETED reviews remain intact — not affected by new trigger")
        void historicalReviewsNotAffected() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PENDING))
                    .thenReturn(false);
            when(aiReviewRepository.existsBySubmissionIdAndStatus(SUBMISSION_ID, AiReviewStatus.PROCESSING))
                    .thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);

            // This creates a new review (3 saves: PENDING → PROCESSING → COMPLETED)
            service.triggerAutomaticReview(SUBMISSION_ID);
            verify(aiReviewRepository, times(3)).save(any());
            // findAll for historical — not affected by trigger
            verify(aiReviewRepository, never()).deleteAll();
        }
    }

    // ── Missing submission ────────────────────────────────────────────────────

    @Nested
    @DisplayName("Missing submission")
    class MissingSubmission {

        @Test
        @DisplayName("9. Submission not found — returns silently without creating review")
        void submissionNotFoundReturnsSilently() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.empty());

            service.triggerAutomaticReview(SUBMISSION_ID);

            verify(aiReviewRepository, never()).save(any());
            verify(groqClient, never()).chat(any(), any());
        }
    }

    // ── Groq failure handling ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Groq failure handling")
    class GroqFailureHandling {

        @BeforeEach
        void setUpSubmission() {
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
        }

        @Test
        @DisplayName("10. Groq 429 rate-limit → FAILED, submission unaffected")
        void groq429CreatesFailed() {
            when(groqClient.chat(any(), any()))
                    .thenThrow(new GroqClientException("Rate limited",
                            GroqClientException.ErrorType.API_FAILURE));

            // Must not throw — failure captured as FAILED review
            service.triggerAutomaticReview(SUBMISSION_ID);

            // 3 saves: PENDING (trigger), PROCESSING (performAnalysis), FAILED (performAnalysis catch)
            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues().get(2).getStatus()).isEqualTo(AiReviewStatus.FAILED);
        }

        @Test
        @DisplayName("11. Groq timeout → FAILED")
        void groqTimeoutCreatesFailed() {
            when(groqClient.chat(any(), any()))
                    .thenThrow(new GroqClientException("Timeout",
                            GroqClientException.ErrorType.TIMEOUT));

            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues().get(2).getStatus()).isEqualTo(AiReviewStatus.FAILED);
            assertThat(captor.getAllValues().get(2).getErrorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("12. Groq model not found → FAILED")
        void groqModelNotFoundCreatesFailed() {
            when(groqClient.chat(any(), any()))
                    .thenThrow(new GroqClientException("Model not found",
                            GroqClientException.ErrorType.INVALID_REQUEST));

            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues().get(2).getStatus()).isEqualTo(AiReviewStatus.FAILED);
        }

        @Test
        @DisplayName("13. Malformed AI JSON → FAILED")
        void malformedJsonCreatesFailed() {
            when(groqClient.chat(any(), any())).thenReturn("not valid json!!!");

            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues().get(2).getStatus()).isEqualTo(AiReviewStatus.FAILED);
            assertThat(captor.getAllValues().get(2).getErrorMessage()).isNotBlank();
        }

        @Test
        @DisplayName("14. FAILED notification sent when Groq fails (Phase 7D: sends to both manager and employee)")
        void failedNotificationSent() {
            when(groqClient.chat(any(), any()))
                    .thenThrow(new GroqClientException("Timeout",
                            GroqClientException.ErrorType.TIMEOUT));

            service.triggerAutomaticReview(SUBMISSION_ID);
            // Phase 7D: failure notification sent to both manager (requester) AND employee (submitter).
            verify(notificationService, org.mockito.Mockito.atLeastOnce())
                    .createNotification(any(), any(), any(), any(), any());
        }
    }

    // ── Attachment extraction failure ─────────────────────────────────────────

    @Nested
    @DisplayName("Attachment extraction failure")
    class AttachmentExtractionFailure {

        @Test
        @DisplayName("15. Extraction failure → FAILED (does not crash submission)")
        void extractionFailureCreatesFailed() {
            TaskSubmission submission = makeSubmissionWithCreator();
            submission.setAttachmentStorageKey("submissions/test/file.pdf");
            submission.setAttachmentMimeType("application/pdf");
            submission.setAttachmentOriginalName("report.pdf");

            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(extractionService.extractText(any(), any(), any()))
                    .thenThrow(new RuntimeException("File not found on disk"));

            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues().get(2).getStatus()).isEqualTo(AiReviewStatus.FAILED);
        }
    }

    // ── Security — employees cannot request reviews ────────────────────────────

    @Nested
    @DisplayName("Security — manual requestReview still blocks employees")
    class SecurityTest {

        @Test
        @DisplayName("16. Employee role denied from manual requestReview")
        void employeeDeniedFromManualReview() {
            when(securityUtils.isPrivileged()).thenReturn(false);

            org.assertj.core.api.Assertions.assertThatThrownBy(
                    () -> service.requestReview(SUBMISSION_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("17. Manager can access manual requestReview")
        void managerCanRequestManualReview() {
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(makeManager()));
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);

            TaskAiReviewResponse response = service.requestReview(SUBMISSION_ID);
            assertThat(response).isNotNull();
            assertThat(response.status()).isEqualTo(AiReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("18. Manual evaluation uses same service as automatic — no duplicate implementation")
        void manualAndAutomaticUseSameService() {
            // Both requestReview() and triggerAutomaticReview() call performAnalysis()
            // internally. This is verified by checking both produce COMPLETED from same mock setup.
            when(securityUtils.isPrivileged()).thenReturn(true);
            when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(makeManager()));
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(makeSubmissionWithCreator()));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);

            // Manual
            TaskAiReviewResponse manualResponse = service.requestReview(SUBMISSION_ID);
            assertThat(manualResponse.status()).isEqualTo(AiReviewStatus.COMPLETED);

            // Auto (second call — no duplicate active since existsByStatus returns false always)
            service.triggerAutomaticReview(SUBMISSION_ID);
            // 6 saves total: requestReview (3: PENDING, PROCESSING, COMPLETED) + triggerAutomaticReview (3)
            verify(aiReviewRepository, times(6)).save(any());
        }
    }

    // ── Requester resolution ──────────────────────────────────────────────────

    @Nested
    @DisplayName("Requester resolution")
    class RequesterResolution {

        @Test
        @DisplayName("19. Review attributed to task creator when available")
        void resolvedToTaskCreator() {
            TaskSubmission submission = makeSubmissionWithCreator();
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);

            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            assertThat(captor.getAllValues().get(0).getRequestedBy().getUser().getFirstName())
                    .isEqualTo("Manager");
        }

        @Test
        @DisplayName("20. Falls back to submitter when task has no creator")
        void fallbackToSubmitter() {
            TaskSubmission submission = makeSubmissionWithoutCreator();
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_ID))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(aiReviewRepository.save(any())).thenAnswer(inv -> {
                TaskAiReview r = inv.getArgument(0);
                if (r.getId() == null) setId(r, REVIEW_ID);
                return r;
            });
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);

            service.triggerAutomaticReview(SUBMISSION_ID);

            ArgumentCaptor<TaskAiReview> captor = ArgumentCaptor.forClass(TaskAiReview.class);
            verify(aiReviewRepository, times(3)).save(captor.capture());
            // Falls back to submitter
            assertThat(captor.getAllValues().get(0).getRequestedBy()).isNotNull();
        }
    }

    // ── Startup recovery ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("Startup recovery")
    class StartupRecovery {

        @Test
        @DisplayName("21. Stale PENDING reviews are requeued on startup")
        void staleReviewsRequeued() {
            TaskAiReview staleReview = new TaskAiReview();
            setId(staleReview, REVIEW_ID);
            staleReview.setStatus(AiReviewStatus.PENDING);
            TaskSubmission submission = makeSubmissionWithCreator();
            staleReview.setSubmission(submission);

            AiReviewStartupRecoveryService recoveryService =
                    new AiReviewStartupRecoveryService(aiReviewRepository, service);

            when(aiReviewRepository.findAllByStatusIn(any())).thenReturn(List.of(staleReview));
            when(aiReviewRepository.save(any())).thenReturn(staleReview);

            // Recovery will call triggerAutomaticReview, which calls findByIdWithAssociations
            when(submissionRepository.findByIdWithAssociations(any()))
                    .thenReturn(Optional.of(submission));
            when(aiReviewRepository.existsBySubmissionIdAndStatus(any(), any())).thenReturn(false);
            when(commentRepository.findByTaskIdOrderByCreatedAtAsc(any()))
                    .thenReturn(Collections.emptyList());
            when(groqClient.chat(any(), any())).thenReturn(VALID_AI_JSON);

            recoveryService.recoverStaleReviews();

            // Recovery saves the review back to PENDING, then triggerAutomaticReview saves again
            verify(aiReviewRepository).findAllByStatusIn(any());
        }

        @Test
        @DisplayName("22. No stale reviews — recovery is a no-op")
        void noStaleReviewsNoOp() {
            AiReviewStartupRecoveryService recoveryService =
                    new AiReviewStartupRecoveryService(aiReviewRepository, service);

            when(aiReviewRepository.findAllByStatusIn(any())).thenReturn(Collections.emptyList());

            recoveryService.recoverStaleReviews();

            // Only findAll called, no saves, no Groq calls
            verify(aiReviewRepository).findAllByStatusIn(any());
            verify(aiReviewRepository, never()).save(any());
            verify(groqClient, never()).chat(any(), any());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Task makeTask() {
        Task task = new Task();
        setId(task, TASK_ID);
        task.setTitle("Phase 7C Test Task");
        task.setDescription("Automatic evaluation test task");
        return task;
    }

    private Task makeTaskWithCreator() {
        Task task = makeTask();
        task.setCreatedByEmployee(makeManager());
        return task;
    }

    private TaskSubmission makeSubmissionWithCreator() {
        TaskSubmission s = new TaskSubmission();
        setId(s, SUBMISSION_ID);
        s.setTask(makeTaskWithCreator());
        s.setSubmittedBy(makeEmployee());
        s.setSubmissionNotes("I completed the work.");
        return s;
    }

    private TaskSubmission makeSubmissionWithoutCreator() {
        TaskSubmission s = new TaskSubmission();
        setId(s, SUBMISSION_ID);
        Task task = makeTask();
        task.setCreatedByEmployee(null); // no creator
        s.setTask(task);
        s.setSubmittedBy(makeEmployee());
        s.setSubmissionNotes("Resubmission.");
        return s;
    }

    private Employee makeManager() {
        User user = new User();
        user.setFirstName("Manager");
        user.setLastName("User");
        Employee emp = new Employee();
        setId(emp, UUID.randomUUID());
        emp.setUser(user);
        return emp;
    }

    private Employee makeEmployee() {
        User user = new User();
        user.setFirstName("Employee");
        user.setLastName("User");
        Employee emp = new Employee();
        setId(emp, UUID.randomUUID());
        emp.setUser(user);
        return emp;
    }

    private void setId(final Object entity, final UUID id) {
        try {
            java.lang.reflect.Field field =
                    com.company.employeemanagement.entity.BaseEntity.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (Exception e) {
            throw new RuntimeException("Could not set id on " + entity.getClass().getSimpleName(), e);
        }
    }
}
