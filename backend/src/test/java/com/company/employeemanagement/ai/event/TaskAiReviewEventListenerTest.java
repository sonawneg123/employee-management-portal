package com.company.employeemanagement.ai.event;

import com.company.employeemanagement.ai.service.TaskAiReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link TaskAiReviewEventListener}.
 *
 * <p>Verifies that the event listener correctly delegates to the AI review service
 * when a {@link TaskSubmissionAiEvent} is received, and that exceptions do not
 * propagate out of the handler.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Phase 7C — TaskAiReviewEventListener")
class TaskAiReviewEventListenerTest {

    @Mock
    private TaskAiReviewService aiReviewService;

    @InjectMocks
    private TaskAiReviewEventListener listener;

    @Test
    @DisplayName("1. Event received → delegates to triggerAutomaticReview with correct submissionId")
    void delegatesToService() {
        UUID submissionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskSubmissionAiEvent event = new TaskSubmissionAiEvent(submissionId, taskId);

        listener.onSubmissionCreated(event);

        verify(aiReviewService).triggerAutomaticReview(submissionId);
    }

    @Test
    @DisplayName("2. Exception in service does not propagate — swallowed silently")
    void exceptionSwallowed() {
        UUID submissionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskSubmissionAiEvent event = new TaskSubmissionAiEvent(submissionId, taskId);

        org.mockito.Mockito.doThrow(new RuntimeException("Groq down"))
                .when(aiReviewService).triggerAutomaticReview(submissionId);

        // Must not throw
        org.assertj.core.api.Assertions.assertThatCode(
                () -> listener.onSubmissionCreated(event)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("3. Event carries correct submissionId and taskId")
    void eventCarriesCorrectIds() {
        UUID submissionId = UUID.randomUUID();
        UUID taskId = UUID.randomUUID();
        TaskSubmissionAiEvent event = new TaskSubmissionAiEvent(submissionId, taskId);

        org.assertj.core.api.Assertions.assertThat(event.submissionId()).isEqualTo(submissionId);
        org.assertj.core.api.Assertions.assertThat(event.taskId()).isEqualTo(taskId);
    }
}
