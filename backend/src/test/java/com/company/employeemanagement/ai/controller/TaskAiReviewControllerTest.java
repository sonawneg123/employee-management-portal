package com.company.employeemanagement.ai.controller;

import com.company.employeemanagement.ai.service.TaskAiReviewService;
import com.company.employeemanagement.dto.response.TaskAiReviewResponse;
import com.company.employeemanagement.entity.enums.AiRecommendedAction;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link TaskAiReviewController}.
 *
 * <p>Uses standalone MockMvc (no Spring Security filter) — the service layer
 * enforces authorization; security integration tests are in
 * {@link TaskAiReviewSecurityTest}.
 *
 * <p>Tests:
 * <ul>
 *   <li>201 Created on successful review request</li>
 *   <li>200 OK on GET latest review</li>
 *   <li>200 OK on GET all reviews</li>
 *   <li>200 OK on GET review by ID</li>
 *   <li>404 → HTTP 404 via GlobalExceptionHandler</li>
 *   <li>409 → HTTP 409 via GlobalExceptionHandler</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskAiReviewController")
class TaskAiReviewControllerTest {

    @Mock
    private TaskAiReviewService aiReviewService;

    @InjectMocks
    private TaskAiReviewController controller;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    private static final UUID SUBMISSION_ID = UUID.randomUUID();
    private static final UUID REVIEW_ID     = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ── POST /task-submissions/{id}/ai-review ─────────────────────────────────

    @Nested
    @DisplayName("POST /task-submissions/{id}/ai-review")
    class RequestReview {

        @Test
        @DisplayName("returns 201 with review response on success")
        void returns201OnSuccess() throws Exception {
            when(aiReviewService.requestReview(SUBMISSION_ID)).thenReturn(buildResponse());

            mockMvc.perform(post("/task-submissions/{id}/ai-review", SUBMISSION_ID)
                           .contentType(MediaType.APPLICATION_JSON))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status").value("COMPLETED"))
                   .andExpect(jsonPath("$.completionScore").value(75));
        }

        @Test
        @DisplayName("returns 404 when service throws ResourceNotFoundException")
        void returns404WhenNotFound() throws Exception {
            when(aiReviewService.requestReview(SUBMISSION_ID))
                    .thenThrow(new ResourceNotFoundException("Submission not found"));

            mockMvc.perform(post("/task-submissions/{id}/ai-review", SUBMISSION_ID))
                   .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 409 when service throws IllegalStateException (duplicate)")
        void returns409WhenDuplicate() throws Exception {
            when(aiReviewService.requestReview(SUBMISSION_ID))
                    .thenThrow(new IllegalStateException("An AI review is already in progress"));

            mockMvc.perform(post("/task-submissions/{id}/ai-review", SUBMISSION_ID))
                   .andExpect(status().isConflict());
        }
    }

    // ── GET /task-submissions/{id}/ai-review ──────────────────────────────────

    @Nested
    @DisplayName("GET /task-submissions/{id}/ai-review")
    class GetLatestReview {

        @Test
        @DisplayName("returns 200 with latest review")
        void returns200() throws Exception {
            when(aiReviewService.getLatestReviewForSubmission(SUBMISSION_ID))
                    .thenReturn(buildResponse());

            mockMvc.perform(get("/task-submissions/{id}/ai-review", SUBMISSION_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.status").value("COMPLETED"))
                   .andExpect(jsonPath("$.confidence").value(85));
        }

        @Test
        @DisplayName("returns 404 when no review exists")
        void returns404WhenNotFound() throws Exception {
            when(aiReviewService.getLatestReviewForSubmission(SUBMISSION_ID))
                    .thenThrow(new ResourceNotFoundException("No review found"));

            mockMvc.perform(get("/task-submissions/{id}/ai-review", SUBMISSION_ID))
                   .andExpect(status().isNotFound());
        }
    }

    // ── GET /task-submissions/{id}/ai-reviews ─────────────────────────────────

    @Nested
    @DisplayName("GET /task-submissions/{id}/ai-reviews")
    class GetAllReviews {

        @Test
        @DisplayName("returns 200 with list of reviews")
        void returns200WithList() throws Exception {
            when(aiReviewService.getAllReviewsForSubmission(SUBMISSION_ID))
                    .thenReturn(List.of(buildResponse()));

            mockMvc.perform(get("/task-submissions/{id}/ai-reviews", SUBMISSION_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$[0].completionScore").value(75));
        }
    }

    // ── GET /task-ai-reviews/{id} ─────────────────────────────────────────────

    @Nested
    @DisplayName("GET /task-ai-reviews/{id}")
    class GetReviewById {

        @Test
        @DisplayName("returns 200 with review")
        void returns200() throws Exception {
            when(aiReviewService.getReviewById(REVIEW_ID)).thenReturn(buildResponse());

            mockMvc.perform(get("/task-ai-reviews/{id}", REVIEW_ID))
                   .andExpect(status().isOk())
                   .andExpect(jsonPath("$.managerSummary").value("Looks good."));
        }

        @Test
        @DisplayName("returns 404 when review not found")
        void returns404WhenNotFound() throws Exception {
            when(aiReviewService.getReviewById(REVIEW_ID))
                    .thenThrow(new ResourceNotFoundException("Review not found"));

            mockMvc.perform(get("/task-ai-reviews/{id}", REVIEW_ID))
                   .andExpect(status().isNotFound());
        }
    }

    // ── FAILED review in response ─────────────────────────────────────────────

    @Nested
    @DisplayName("FAILED review response")
    class FailedReviewResponse {

        @Test
        @DisplayName("returns 201 with FAILED status when AI call failed")
        void returns201WithFailedStatus() throws Exception {
            TaskAiReviewResponse failedResponse = new TaskAiReviewResponse(
                    REVIEW_ID, UUID.randomUUID(), SUBMISSION_ID, UUID.randomUUID(),
                    "Jane Manager",
                    AiReviewStatus.FAILED, "groq", "groq/compound-mini", "v1",
                    null, null, null, null,
                    null, null,
                    "GroqClientException: Timeout",
                    LocalDateTime.now(), LocalDateTime.now()
            );
            when(aiReviewService.requestReview(SUBMISSION_ID)).thenReturn(failedResponse);

            mockMvc.perform(post("/task-submissions/{id}/ai-review", SUBMISSION_ID))
                   .andExpect(status().isCreated())
                   .andExpect(jsonPath("$.status").value("FAILED"))
                   .andExpect(jsonPath("$.errorMessage").exists());
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private TaskAiReviewResponse buildResponse() {
        return new TaskAiReviewResponse(
                REVIEW_ID,
                UUID.randomUUID(),
                SUBMISSION_ID,
                UUID.randomUUID(),
                "Jane Manager",
                AiReviewStatus.COMPLETED,
                "groq",
                "groq/compound-mini",
                "v1",
                75,
                80,
                85,
                AiRecommendedAction.APPROVE,
                "{\"completionScore\":75}",
                "Looks good.",
                null,
                LocalDateTime.now().minusMinutes(1),
                LocalDateTime.now()
        );
    }
}
