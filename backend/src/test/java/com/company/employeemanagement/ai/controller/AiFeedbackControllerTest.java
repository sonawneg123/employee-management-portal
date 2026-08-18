package com.company.employeemanagement.ai.controller;

import com.company.employeemanagement.ai.service.AiFeedbackService;
import com.company.employeemanagement.dto.response.AiDashboardSummaryResponse;
import com.company.employeemanagement.dto.response.AiFeedbackResponse;
import com.company.employeemanagement.dto.response.AiScoreTrendResponse;
import com.company.employeemanagement.dto.response.AiTaskInsightsResponse;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiFeedbackController} (Phase 7D).
 *
 * <p>Verifies that the controller:
 * <ul>
 *   <li>Delegates to the correct service methods</li>
 *   <li>Returns correct HTTP status codes</li>
 *   <li>Propagates exceptions from the service</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiFeedbackController — Phase 7D")
class AiFeedbackControllerTest {

    @Mock private AiFeedbackService aiFeedbackService;
    @InjectMocks private AiFeedbackController controller;

    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID TASK_ID       = UUID.randomUUID();
    private static final UUID REVIEW_ID     = UUID.randomUUID();

    private AiFeedbackResponse buildFeedback(final AiReviewStatus status) {
        return new AiFeedbackResponse(
                REVIEW_ID, SUBMISSION_ID, status,
                85, 88, 85, 80,
                "Good work overall.",
                List.of("Clear naming"),
                List.of("Missing docs"),
                List.of("Add documentation"),
                LocalDateTime.now(),
                LocalDateTime.now().minusMinutes(5),
                AiFeedbackResponse.STANDARD_EVALUATION_EXPLANATION
        );
    }

    @Nested
    @DisplayName("getEmployeeAiFeedback")
    class GetEmployeeAiFeedback {

        @Test
        @DisplayName("Returns 200 with feedback for completed review")
        void returns200WithFeedback() {
            AiFeedbackResponse feedback = buildFeedback(AiReviewStatus.COMPLETED);
            when(aiFeedbackService.getEmployeeAiFeedback(SUBMISSION_ID)).thenReturn(feedback);

            ResponseEntity<AiFeedbackResponse> response = controller.getEmployeeAiFeedback(SUBMISSION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().status()).isEqualTo(AiReviewStatus.COMPLETED);
        }

        @Test
        @DisplayName("Returns 200 with pending feedback")
        void returns200WithPendingFeedback() {
            AiFeedbackResponse feedback = buildFeedback(AiReviewStatus.PENDING);
            when(aiFeedbackService.getEmployeeAiFeedback(SUBMISSION_ID)).thenReturn(feedback);

            ResponseEntity<AiFeedbackResponse> response = controller.getEmployeeAiFeedback(SUBMISSION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().status()).isEqualTo(AiReviewStatus.PENDING);
        }

        @Test
        @DisplayName("Propagates ResourceNotFoundException from service (404)")
        void propagates404() {
            when(aiFeedbackService.getEmployeeAiFeedback(SUBMISSION_ID))
                    .thenThrow(new ResourceNotFoundException("Submission not found: " + SUBMISSION_ID));

            assertThatThrownBy(() -> controller.getEmployeeAiFeedback(SUBMISSION_ID))
                    .isInstanceOf(ResourceNotFoundException.class);
        }

        @Test
        @DisplayName("Propagates AccessDeniedException from service (403)")
        void propagates403() {
            when(aiFeedbackService.getEmployeeAiFeedback(SUBMISSION_ID))
                    .thenThrow(new AccessDeniedException("Access denied"));

            assertThatThrownBy(() -> controller.getEmployeeAiFeedback(SUBMISSION_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("getEmployeeAiHistory")
    class GetEmployeeAiHistory {

        @Test
        @DisplayName("Returns 200 with history list")
        void returns200WithHistory() {
            List<AiFeedbackResponse> history = List.of(
                    buildFeedback(AiReviewStatus.COMPLETED),
                    buildFeedback(AiReviewStatus.COMPLETED)
            );
            when(aiFeedbackService.getEmployeeAiHistory(SUBMISSION_ID)).thenReturn(history);

            ResponseEntity<List<AiFeedbackResponse>> response = controller.getEmployeeAiHistory(SUBMISSION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).hasSize(2);
        }

        @Test
        @DisplayName("Returns 200 with empty list when no history")
        void returns200WithEmptyList() {
            when(aiFeedbackService.getEmployeeAiHistory(SUBMISSION_ID))
                    .thenReturn(Collections.emptyList());

            ResponseEntity<List<AiFeedbackResponse>> response = controller.getEmployeeAiHistory(SUBMISSION_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getScoreTrend")
    class GetScoreTrend {

        @Test
        @DisplayName("Returns 200 with score trend")
        void returns200WithTrend() {
            AiScoreTrendResponse trend = new AiScoreTrendResponse(
                    TASK_ID,
                    List.of(
                            new AiScoreTrendResponse.ScorePoint(UUID.randomUUID(), 1, 70, 72, LocalDateTime.now().minusDays(1)),
                            new AiScoreTrendResponse.ScorePoint(UUID.randomUUID(), 2, 85, 88, LocalDateTime.now())
                    ),
                    AiScoreTrendResponse.TrendDirection.IMPROVING,
                    15, 85, 70, 15, true
            );
            when(aiFeedbackService.getScoreTrend(TASK_ID)).thenReturn(trend);

            ResponseEntity<AiScoreTrendResponse> response = controller.getScoreTrend(TASK_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().trendDirection())
                    .isEqualTo(AiScoreTrendResponse.TrendDirection.IMPROVING);
        }

        @Test
        @DisplayName("Returns insufficient data trend when less than 2 evaluations")
        void returnsInsufficientDataTrend() {
            AiScoreTrendResponse trend = new AiScoreTrendResponse(
                    TASK_ID, Collections.emptyList(),
                    AiScoreTrendResponse.TrendDirection.INSUFFICIENT_DATA,
                    null, null, null, null, false
            );
            when(aiFeedbackService.getScoreTrend(TASK_ID)).thenReturn(trend);

            ResponseEntity<AiScoreTrendResponse> response = controller.getScoreTrend(TASK_ID);

            assertThat(response.getBody().hasTrendData()).isFalse();
            assertThat(response.getBody().trendDirection())
                    .isEqualTo(AiScoreTrendResponse.TrendDirection.INSUFFICIENT_DATA);
        }
    }

    @Nested
    @DisplayName("getTaskInsights")
    class GetTaskInsights {

        @Test
        @DisplayName("Returns 200 with task insights")
        void returns200WithInsights() {
            AiTaskInsightsResponse insights = new AiTaskInsightsResponse(
                    TASK_ID, 3, 2, 82.5,
                    List.of("Style issue"),
                    List.of("Missing docs"),
                    List.of("Clear naming"),
                    List.of("Add more tests"),
                    AiScoreTrendResponse.TrendDirection.IMPROVING
            );
            when(aiFeedbackService.getTaskInsights(TASK_ID)).thenReturn(insights);

            ResponseEntity<AiTaskInsightsResponse> response = controller.getTaskInsights(TASK_ID);

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().completedEvaluations()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("getDashboardSummary")
    class GetDashboardSummary {

        @Test
        @DisplayName("Returns 200 with AI dashboard summary")
        void returns200WithDashboardSummary() {
            AiDashboardSummaryResponse summary = new AiDashboardSummaryResponse(
                    12, 84.0, 3, 1, 5, 2
            );
            when(aiFeedbackService.getDashboardSummary()).thenReturn(summary);

            ResponseEntity<AiDashboardSummaryResponse> response = controller.getDashboardSummary();

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody().totalEvaluated()).isEqualTo(12);
            assertThat(response.getBody().averageScore()).isEqualTo(84.0);
            assertThat(response.getBody().employeesImproving()).isEqualTo(3);
            assertThat(response.getBody().employeesNeedingAttention()).isEqualTo(1);
            assertThat(response.getBody().submissionsAwaitingEvaluation()).isEqualTo(5);
        }

        @Test
        @DisplayName("Employee denied from dashboard summary")
        void employeeDeniedFromDashboard() {
            when(aiFeedbackService.getDashboardSummary())
                    .thenThrow(new AccessDeniedException("Access denied"));

            assertThatThrownBy(() -> controller.getDashboardSummary())
                    .isInstanceOf(AccessDeniedException.class);
        }
    }
}
