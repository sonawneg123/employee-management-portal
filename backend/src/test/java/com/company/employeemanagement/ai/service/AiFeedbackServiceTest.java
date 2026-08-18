package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.dto.TaskAiAnalysis;
import com.company.employeemanagement.dto.response.AiDashboardSummaryResponse;
import com.company.employeemanagement.dto.response.AiFeedbackResponse;
import com.company.employeemanagement.dto.response.AiScoreTrendResponse;
import com.company.employeemanagement.dto.response.AiScoreTrendResponse.TrendDirection;
import com.company.employeemanagement.dto.response.AiTaskInsightsResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskAiReview;
import com.company.employeemanagement.entity.TaskSubmission;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import com.company.employeemanagement.repository.TaskSubmissionRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for Phase 7D — {@link AiFeedbackService}.
 *
 * <p>Covers:
 * <ul>
 *   <li>Employee can view own AI feedback</li>
 *   <li>Employee cannot view another employee's AI feedback (IDOR / data isolation)</li>
 *   <li>Employee can view own AI history</li>
 *   <li>Employee cannot view another employee's AI history</li>
 *   <li>Manager can view task AI trend</li>
 *   <li>Employee denied from manager-only endpoints</li>
 *   <li>Score trend calculation: improving / stable / declining</li>
 *   <li>Improvement classification correctness</li>
 *   <li>Insufficient history behavior (less than 2 completed evaluations)</li>
 *   <li>Failed evaluations excluded from score trends</li>
 *   <li>Employee-facing response never exposes manager-only fields</li>
 *   <li>AI data isolation</li>
 *   <li>Dashboard summary counts</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiFeedbackService — Phase 7D")
class AiFeedbackServiceTest {

    @Mock private TaskAiReviewRepository aiReviewRepository;
    @Mock private TaskSubmissionRepository submissionRepository;
    @Mock private SecurityUtils securityUtils;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private AiFeedbackService service;

    // ── Test fixtures ──────────────────────────────────────────────────────────

    private static final UUID EMPLOYEE_A_ID    = UUID.randomUUID();
    private static final UUID EMPLOYEE_B_ID    = UUID.randomUUID();
    private static final UUID SUBMISSION_A_ID  = UUID.randomUUID();
    private static final UUID SUBMISSION_B_ID  = UUID.randomUUID();
    private static final UUID REVIEW_ID        = UUID.randomUUID();
    private static final UUID TASK_ID          = UUID.randomUUID();

    private Employee employeeA;
    private Employee employeeB;
    private Task task;
    private TaskSubmission submissionA;
    private TaskSubmission submissionB;

    @BeforeEach
    void setUp() {
        service = new AiFeedbackService(
                aiReviewRepository, submissionRepository, securityUtils, objectMapper);

        // Employee A
        User userA = User.builder().firstName("Alice").lastName("Smith")
                .email("alice@test.com").passwordHash("h").build();
        userA.setId(UUID.randomUUID());
        employeeA = Employee.builder().employeeCode("EMP-A").build();
        employeeA.setId(EMPLOYEE_A_ID);
        employeeA.setUser(userA);

        // Employee B
        User userB = User.builder().firstName("Bob").lastName("Jones")
                .email("bob@test.com").passwordHash("h").build();
        userB.setId(UUID.randomUUID());
        employeeB = Employee.builder().employeeCode("EMP-B").build();
        employeeB.setId(EMPLOYEE_B_ID);
        employeeB.setUser(userB);

        // Task
        task = Task.builder().title("Test Task").build();
        task.setId(TASK_ID);

        // Submission A (belongs to employee A)
        submissionA = TaskSubmission.builder()
                .task(task)
                .submittedBy(employeeA)
                .submissionNotes("Notes A")
                .build();
        submissionA.setId(SUBMISSION_A_ID);

        // Submission B (belongs to employee B)
        submissionB = TaskSubmission.builder()
                .task(task)
                .submittedBy(employeeB)
                .submissionNotes("Notes B")
                .build();
        submissionB.setId(SUBMISSION_B_ID);
    }

    // ── Helpers ────────────────────────────────────────────────────────────────

    private TaskAiReview buildCompletedReview(final UUID id,
                                               final TaskSubmission submission,
                                               final int score,
                                               final int qualityScore,
                                               final LocalDateTime completedAt) {
        TaskAiReview review = TaskAiReview.builder()
                .task(task)
                .submission(submission)
                .requestedBy(employeeA)
                .status(AiReviewStatus.COMPLETED)
                .completionScore(score)
                .qualityScore(qualityScore)
                .confidence(80)
                .structuredAnalysisJson(buildAnalysisJson(score, qualityScore))
                .build();
        review.setId(id);
        review.setCompletedAt(completedAt);
        return review;
    }

    private TaskAiReview buildPendingReview(final TaskSubmission submission) {
        TaskAiReview review = TaskAiReview.builder()
                .task(task)
                .submission(submission)
                .requestedBy(employeeA)
                .status(AiReviewStatus.PENDING)
                .build();
        review.setId(UUID.randomUUID());
        return review;
    }

    private TaskAiReview buildFailedReview(final TaskSubmission submission) {
        TaskAiReview review = TaskAiReview.builder()
                .task(task)
                .submission(submission)
                .requestedBy(employeeA)
                .status(AiReviewStatus.FAILED)
                .errorMessage("GroqClientException: Timeout")
                .build();
        review.setId(UUID.randomUUID());
        review.setCompletedAt(LocalDateTime.now());
        return review;
    }

    private String buildAnalysisJson(final int completionScore, final int qualityScore) {
        return String.format(
                """
                {
                  "completionScore": %d,
                  "overallAssessment": "Good work overall.",
                  "requirements": [],
                  "completedItems": ["Item 1"],
                  "missingItems": ["Missing doc"],
                  "partialItems": [],
                  "qualityAssessment": {
                    "score": %d,
                    "summary": "Quality summary",
                    "strengths": ["Clear naming", "Good coverage"],
                    "weaknesses": ["Missing edge cases"]
                  },
                  "issues": ["Style issue"],
                  "modificationSuggestions": ["Add more tests", "Improve docs"],
                  "managerSummary": "Manager sees this — not employee",
                  "recommendedAction": "APPROVE",
                  "confidence": 85
                }
                """, completionScore, qualityScore);
    }

    private void setupEmployeeContext(final UUID employeeId) {
        when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(true);
        when(securityUtils.isPrivileged()).thenReturn(false);
        Employee emp = employeeId.equals(EMPLOYEE_A_ID) ? employeeA : employeeB;
        when(securityUtils.getCurrentEmployee()).thenReturn(Optional.of(emp));
    }

    private void setupManagerContext() {
        when(securityUtils.hasRole("ROLE_EMPLOYEE")).thenReturn(false);
        when(securityUtils.isPrivileged()).thenReturn(true);
    }

    // ── Employee AI Feedback ───────────────────────────────────────────────────

    @Nested
    @DisplayName("getEmployeeAiFeedback")
    class GetEmployeeAiFeedback {

        @Test
        @DisplayName("Employee can view own AI feedback")
        void employeeCanViewOwnFeedback() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildCompletedReview(REVIEW_ID, submissionA,
                    85, 88, LocalDateTime.now());
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            assertThat(response).isNotNull();
            assertThat(response.id()).isEqualTo(REVIEW_ID);
            assertThat(response.status()).isEqualTo(AiReviewStatus.COMPLETED);
            assertThat(response.overallScore()).isEqualTo(85);
            assertThat(response.workQualityScore()).isEqualTo(88);
        }

        @Test
        @DisplayName("Employee feedback contains strengths and areas to improve")
        void feedbackContainsStrengthsAndAreasToImprove() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildCompletedReview(REVIEW_ID, submissionA,
                    85, 88, LocalDateTime.now());
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            assertThat(response.strengths()).isNotEmpty();
            assertThat(response.areasToImprove()).isNotEmpty();
            assertThat(response.suggestionsForNextSubmission()).isNotEmpty();
        }

        @Test
        @DisplayName("Employee feedback does NOT expose managerSummary")
        void feedbackDoesNotExposeManagerSummary() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildCompletedReview(REVIEW_ID, submissionA,
                    85, 88, LocalDateTime.now());
            review.setManagerSummary("Manager-only summary: recommend approval");
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            // AiFeedbackResponse does NOT have a managerSummary field
            // The record fields are: id, submissionId, status, overallScore, workQualityScore,
            // completenessScore, relevanceScore, summary, strengths, areasToImprove,
            // suggestionsForNextSubmission, evaluatedAt, requestedAt, evaluationExplanation
            // Verify the summary does NOT contain the manager summary value
            assertThat(response.summary()).doesNotContain("Manager-only summary: recommend approval");
        }

        @Test
        @DisplayName("Employee feedback does NOT expose recommendedAction")
        void feedbackDoesNotExposeRecommendedAction() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildCompletedReview(REVIEW_ID, submissionA,
                    85, 88, LocalDateTime.now());
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            // AiFeedbackResponse record has no recommendedAction field at all
            // We verify no approval decision leaks into the safe-facing fields
            assertThat(response.evaluationExplanation())
                    .doesNotContain("APPROVE")
                    .doesNotContain("REQUEST_CHANGES");
        }

        @Test
        @DisplayName("Employee CANNOT view another employee's AI feedback — returns 404")
        void employeeCannotViewOtherEmployeeFeedback() {
            setupEmployeeContext(EMPLOYEE_A_ID); // Alice logged in
            // But we're requesting submission B (belongs to Bob)
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(submissionB));

            assertThatThrownBy(() -> service.getEmployeeAiFeedback(SUBMISSION_B_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(SUBMISSION_B_ID.toString());
        }

        @Test
        @DisplayName("PENDING review shows pending message in summary")
        void pendingReviewShowsPendingMessage() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildPendingReview(submissionA);
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            assertThat(response.status()).isEqualTo(AiReviewStatus.PENDING);
            assertThat(response.summary()).contains("queue");
        }

        @Test
        @DisplayName("FAILED review shows friendly message — does NOT expose raw error")
        void failedReviewShowsFriendlyMessage() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildFailedReview(submissionA);
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            assertThat(response.status()).isEqualTo(AiReviewStatus.FAILED);
            // Friendly message — not raw error
            assertThat(response.summary()).doesNotContain("GroqClientException");
            assertThat(response.summary()).doesNotContain("Timeout");
            // Contains something human-readable
            assertThat(response.summary()).isNotBlank();
        }

        @Test
        @DisplayName("404 when no AI review exists for submission")
        void throwsWhenNoReviewExists() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getEmployeeAiFeedback(SUBMISSION_A_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Manager can view any employee's AI feedback")
        void managerCanViewAnyFeedback() {
            setupManagerContext();
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(submissionB));

            TaskAiReview review = buildCompletedReview(REVIEW_ID, submissionB,
                    78, 80, LocalDateTime.now());
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_B_ID);
            assertThat(response).isNotNull();
            assertThat(response.overallScore()).isEqualTo(78);
        }
    }

    // ── Employee AI History ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getEmployeeAiHistory")
    class GetEmployeeAiHistory {

        @Test
        @DisplayName("Employee can view own AI history")
        void employeeCanViewOwnHistory() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            List<TaskAiReview> history = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 85, 88, LocalDateTime.now()),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 78, 75, LocalDateTime.now().minusDays(1))
            );
            when(aiReviewRepository.findAllBySubmissionIdOrderByCreatedAtDesc(SUBMISSION_A_ID))
                    .thenReturn(history);

            List<AiFeedbackResponse> responses = service.getEmployeeAiHistory(SUBMISSION_A_ID);

            assertThat(responses).hasSize(2);
            assertThat(responses.get(0).overallScore()).isEqualTo(85);
            assertThat(responses.get(1).overallScore()).isEqualTo(78);
        }

        @Test
        @DisplayName("Employee CANNOT view another employee's AI history — returns 404")
        void employeeCannotViewOtherEmployeeHistory() {
            setupEmployeeContext(EMPLOYEE_A_ID); // Alice logged in
            // But requesting submission B (Bob's)
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(submissionB));

            assertThatThrownBy(() -> service.getEmployeeAiHistory(SUBMISSION_B_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Empty list when no reviews exist")
        void returnsEmptyListWhenNoReviews() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));
            when(aiReviewRepository.findAllBySubmissionIdOrderByCreatedAtDesc(SUBMISSION_A_ID))
                    .thenReturn(Collections.emptyList());

            List<AiFeedbackResponse> responses = service.getEmployeeAiHistory(SUBMISSION_A_ID);

            assertThat(responses).isEmpty();
        }
    }

    // ── Score Trend ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getScoreTrend")
    class GetScoreTrend {

        @Test
        @DisplayName("Employee denied from score trend endpoint")
        void employeeDeniedFromTrend() {
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> service.getScoreTrend(TASK_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Insufficient data when fewer than 2 completed evaluations")
        void insufficientDataWhenFewerThanTwo() {
            setupManagerContext();
            when(aiReviewRepository.findAllByTaskId(TASK_ID))
                    .thenReturn(Collections.emptyList());

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.hasTrendData()).isFalse();
            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.INSUFFICIENT_DATA);
        }

        @Test
        @DisplayName("Single evaluation returns INSUFFICIENT_DATA")
        void singleEvaluationReturnsInsufficientData() {
            setupManagerContext();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 80, 85, LocalDateTime.now())
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.hasTrendData()).isFalse();
            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.INSUFFICIENT_DATA);
            assertThat(trend.latestScore()).isEqualTo(80);
        }

        @Test
        @DisplayName("Score improving when latest > previous by more than 5 points")
        void scoreTrendImproving() {
            setupManagerContext();
            LocalDateTime older = LocalDateTime.now().minusDays(2);
            LocalDateTime newer = LocalDateTime.now();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 70, 72, older),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 85, 88, newer)
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.hasTrendData()).isTrue();
            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.IMPROVING);
            assertThat(trend.latestScore()).isEqualTo(85);
            assertThat(trend.previousScore()).isEqualTo(70);
            assertThat(trend.latestScoreChange()).isEqualTo(15);
        }

        @Test
        @DisplayName("Score stable when change is 5 points or less")
        void scoreTrendStable() {
            setupManagerContext();
            LocalDateTime older = LocalDateTime.now().minusDays(2);
            LocalDateTime newer = LocalDateTime.now();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 80, 82, older),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 82, 84, newer)
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.hasTrendData()).isTrue();
            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.STABLE);
        }

        @Test
        @DisplayName("Score declining when latest < previous by more than 5 points")
        void scoreTrendDeclining() {
            setupManagerContext();
            LocalDateTime older = LocalDateTime.now().minusDays(2);
            LocalDateTime newer = LocalDateTime.now();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 88, 90, older),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 70, 68, newer)
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.hasTrendData()).isTrue();
            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.DECLINING);
        }

        @Test
        @DisplayName("Failed evaluations are excluded from score trend")
        void failedEvaluationsExcludedFromTrend() {
            setupManagerContext();
            LocalDateTime older = LocalDateTime.now().minusDays(2);
            LocalDateTime newer = LocalDateTime.now();

            TaskAiReview failedReview = buildFailedReview(submissionA);
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 70, 72, older),
                    failedReview, // Should be excluded
                    buildCompletedReview(UUID.randomUUID(), submissionA, 85, 88, newer)
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            // Only 2 completed reviews should be in the trend
            assertThat(trend.scoreHistory()).hasSize(2);
            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.IMPROVING);
        }

        @Test
        @DisplayName("Single FAILED evaluation does NOT produce DECLINING classification")
        void singleFailedEvaluationDoesNotClassifyDeclining() {
            setupManagerContext();
            List<TaskAiReview> reviews = List.of(
                    buildFailedReview(submissionA) // Only one, and failed
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.trendDirection()).isEqualTo(TrendDirection.INSUFFICIENT_DATA);
            assertThat(trend.hasTrendData()).isFalse();
        }

        @Test
        @DisplayName("Score points are ordered oldest to newest")
        void scorePointsOrderedOldestToNewest() {
            setupManagerContext();
            LocalDateTime day1 = LocalDateTime.now().minusDays(2);
            LocalDateTime day2 = LocalDateTime.now().minusDays(1);
            LocalDateTime day3 = LocalDateTime.now();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 70, 72, day1),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 80, 82, day2),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 90, 92, day3)
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiScoreTrendResponse trend = service.getScoreTrend(TASK_ID);

            assertThat(trend.scoreHistory()).hasSize(3);
            assertThat(trend.scoreHistory().get(0).overallScore()).isEqualTo(70);
            assertThat(trend.scoreHistory().get(1).overallScore()).isEqualTo(80);
            assertThat(trend.scoreHistory().get(2).overallScore()).isEqualTo(90);
        }
    }

    // ── Task Insights ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("getTaskInsights")
    class GetTaskInsights {

        @Test
        @DisplayName("Employee denied from task insights endpoint")
        void employeeDeniedFromInsights() {
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> service.getTaskInsights(TASK_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Empty insights when no reviews exist")
        void emptyInsightsWhenNoReviews() {
            setupManagerContext();
            when(aiReviewRepository.findAllByTaskId(TASK_ID))
                    .thenReturn(Collections.emptyList());

            AiTaskInsightsResponse insights = service.getTaskInsights(TASK_ID);

            assertThat(insights.totalEvaluations()).isZero();
            assertThat(insights.completedEvaluations()).isZero();
            assertThat(insights.averageScore()).isNull();
            assertThat(insights.commonIssues()).isEmpty();
            assertThat(insights.mostRecentStrengths()).isEmpty();
        }

        @Test
        @DisplayName("Average score is calculated across completed evaluations only")
        void averageScoreFromCompletedOnly() {
            setupManagerContext();
            LocalDateTime t1 = LocalDateTime.now().minusDays(2);
            LocalDateTime t2 = LocalDateTime.now();
            List<TaskAiReview> reviews = new ArrayList<>();
            reviews.add(buildCompletedReview(UUID.randomUUID(), submissionA, 80, 82, t1));
            reviews.add(buildCompletedReview(UUID.randomUUID(), submissionA, 90, 92, t2));
            reviews.add(buildFailedReview(submissionA)); // Should not affect average

            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiTaskInsightsResponse insights = service.getTaskInsights(TASK_ID);

            assertThat(insights.completedEvaluations()).isEqualTo(2);
            assertThat(insights.averageScore()).isEqualTo(85.0);
        }

        @Test
        @DisplayName("Most recent strengths come from latest completed review")
        void mostRecentStrengthsFromLatestReview() {
            setupManagerContext();
            LocalDateTime t1 = LocalDateTime.now().minusDays(1);
            LocalDateTime t2 = LocalDateTime.now();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 80, 82, t1),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 90, 92, t2)
            );
            when(aiReviewRepository.findAllByTaskId(TASK_ID)).thenReturn(reviews);

            AiTaskInsightsResponse insights = service.getTaskInsights(TASK_ID);

            assertThat(insights.mostRecentStrengths()).isNotEmpty();
            assertThat(insights.mostRecentStrengths()).contains("Clear naming");
        }
    }

    // ── Dashboard Summary ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("getDashboardSummary")
    class GetDashboardSummary {

        @Test
        @DisplayName("Employee denied from dashboard summary")
        void employeeDeniedFromDashboard() {
            when(securityUtils.isPrivileged()).thenReturn(false);

            assertThatThrownBy(() -> service.getDashboardSummary())
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Empty dashboard when no reviews exist")
        void emptyDashboardWhenNoReviews() {
            setupManagerContext();
            when(aiReviewRepository.findAll()).thenReturn(Collections.emptyList());

            AiDashboardSummaryResponse summary = service.getDashboardSummary();

            assertThat(summary.totalEvaluated()).isZero();
            assertThat(summary.averageScore()).isNull();
            assertThat(summary.employeesImproving()).isZero();
            assertThat(summary.employeesNeedingAttention()).isZero();
            assertThat(summary.submissionsAwaitingEvaluation()).isZero();
            assertThat(summary.failedEvaluations()).isZero();
        }

        @Test
        @DisplayName("Counts pending + processing in awaiting evaluation")
        void countsAwaitingEvaluation() {
            setupManagerContext();
            TaskAiReview pending = buildPendingReview(submissionA);
            TaskAiReview processing = buildPendingReview(submissionA);
            processing.setStatus(AiReviewStatus.PROCESSING);

            when(aiReviewRepository.findAll()).thenReturn(List.of(pending, processing));

            AiDashboardSummaryResponse summary = service.getDashboardSummary();

            assertThat(summary.submissionsAwaitingEvaluation()).isEqualTo(2);
        }

        @Test
        @DisplayName("Counts failed evaluations")
        void countsFailedEvaluations() {
            setupManagerContext();
            TaskAiReview failed1 = buildFailedReview(submissionA);
            TaskAiReview failed2 = buildFailedReview(submissionB);

            when(aiReviewRepository.findAll()).thenReturn(List.of(failed1, failed2));

            AiDashboardSummaryResponse summary = service.getDashboardSummary();

            assertThat(summary.failedEvaluations()).isEqualTo(2);
        }

        @Test
        @DisplayName("Average score computed from completed evaluations only")
        void averageScoreFromCompleted() {
            setupManagerContext();
            LocalDateTime t1 = LocalDateTime.now().minusDays(1);
            LocalDateTime t2 = LocalDateTime.now();
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 80, 82, t1),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 90, 92, t2)
            );
            when(aiReviewRepository.findAll()).thenReturn(reviews);

            AiDashboardSummaryResponse summary = service.getDashboardSummary();

            assertThat(summary.totalEvaluated()).isEqualTo(1); // 1 unique submission
            assertThat(summary.averageScore()).isEqualTo(85.0);
        }

        @Test
        @DisplayName("Improving count correct — task with two completed evaluations improving")
        void improvingCountCorrect() {
            setupManagerContext();
            LocalDateTime older = LocalDateTime.now().minusDays(2);
            LocalDateTime newer = LocalDateTime.now();
            // Two evaluations for the same task: 70 → 85 (+15 > 5 = IMPROVING)
            List<TaskAiReview> reviews = List.of(
                    buildCompletedReview(UUID.randomUUID(), submissionA, 70, 72, older),
                    buildCompletedReview(UUID.randomUUID(), submissionA, 85, 88, newer)
            );
            when(aiReviewRepository.findAll()).thenReturn(reviews);

            AiDashboardSummaryResponse summary = service.getDashboardSummary();

            assertThat(summary.employeesImproving()).isEqualTo(1);
            assertThat(summary.employeesNeedingAttention()).isZero();
        }
    }

    // ── AI Data Isolation ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("AI data isolation")
    class AiDataIsolation {

        @Test
        @DisplayName("Employee A cannot access Employee B's submission feedback — 404 returned")
        void employeeACannotAccessEmployeeBFeedback() {
            // Employee A is logged in
            setupEmployeeContext(EMPLOYEE_A_ID);
            // But submissionB belongs to Employee B
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(submissionB));

            assertThatThrownBy(() -> service.getEmployeeAiFeedback(SUBMISSION_B_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining(SUBMISSION_B_ID.toString());
        }

        @Test
        @DisplayName("Employee A cannot access Employee B's AI history — 404 returned")
        void employeeACannotAccessEmployeeBHistory() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(submissionB));

            assertThatThrownBy(() -> service.getEmployeeAiHistory(SUBMISSION_B_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("404 is returned (not 403) to avoid leaking existence of other employees' submissions")
        void returns404NotForbiddenForIsolation() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_B_ID))
                    .thenReturn(Optional.of(submissionB));

            // Must be ResourceNotFoundException (404), NOT AccessDeniedException (403)
            // This prevents info leakage about whether the resource exists
            assertThatThrownBy(() -> service.getEmployeeAiFeedback(SUBMISSION_B_ID))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .isNotInstanceOf(AccessDeniedException.class);
        }

        @Test
        @DisplayName("Submission not found returns 404")
        void submissionNotFoundReturns404() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(any(UUID.class)))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getEmployeeAiFeedback(UUID.randomUUID()))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    // ── Evaluation Explanation ─────────────────────────────────────────────────

    @Nested
    @DisplayName("Evaluation explanation")
    class EvaluationExplanation {

        @Test
        @DisplayName("Explanation contains expected safe content")
        void explanationContainsSafeContent() {
            setupEmployeeContext(EMPLOYEE_A_ID);
            when(submissionRepository.findByIdWithAssociations(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(submissionA));

            TaskAiReview review = buildCompletedReview(REVIEW_ID, submissionA,
                    85, 88, LocalDateTime.now());
            when(aiReviewRepository.findLatestBySubmissionId(SUBMISSION_A_ID))
                    .thenReturn(Optional.of(review));

            AiFeedbackResponse response = service.getEmployeeAiFeedback(SUBMISSION_A_ID);

            assertThat(response.evaluationExplanation())
                    .contains("task title")
                    .contains("advisory")
                    .doesNotContain("system prompt")
                    .doesNotContain("API")
                    .doesNotContain("groq")
                    .doesNotContain("GROQ");
        }
    }
}
