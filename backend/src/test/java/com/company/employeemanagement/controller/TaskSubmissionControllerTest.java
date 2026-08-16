package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateTaskSubmissionRequest;
import com.company.employeemanagement.dto.request.RequestChangesRequest;
import com.company.employeemanagement.dto.request.UpdateTaskSubmissionRequest;
import com.company.employeemanagement.dto.response.TaskSubmissionResponse;
import com.company.employeemanagement.entity.enums.SubmissionStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.TaskSubmissionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link TaskSubmissionController} using standalone MockMvc.
 *
 * <p>Spring Security is excluded — these tests focus on request parsing,
 * response serialisation, HTTP status codes, and service delegation.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskSubmissionController")
class TaskSubmissionControllerTest {

    @Mock
    private TaskSubmissionService submissionService;

    private TaskSubmissionController controller;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        controller = new TaskSubmissionController(submissionService, objectMapper);
        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TaskSubmissionResponse buildResponse(final UUID subId, final UUID taskId,
                                                  final SubmissionStatus status) {
        return new TaskSubmissionResponse(
                subId, taskId, "Test Task",
                UUID.randomUUID(), "Jane Doe",
                "Did the work", "Completed feature", null,
                LocalDateTime.now(),
                status,
                status == SubmissionStatus.CHANGES_REQUESTED ? "Please add tests" : null,
                status == SubmissionStatus.APPROVED ? UUID.randomUUID() : null,
                status == SubmissionStatus.APPROVED ? "John Manager" : null,
                status == SubmissionStatus.APPROVED ? LocalDateTime.now() : null,
                LocalDateTime.now(), LocalDateTime.now(),
                // Attachment fields
                null, null, null, null, false
        );
    }

    /**
     * Builds a MockMultipartFile for the 'submission' JSON part.
     */
    private MockMultipartFile submissionPart(final Object payload) throws Exception {
        return new MockMultipartFile(
                "submission",
                "submission.json",
                MediaType.APPLICATION_JSON_VALUE,
                objectMapper.writeValueAsBytes(payload));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /tasks/{taskId}/submissions (multipart)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /tasks/{taskId}/submissions")
    class CreateSubmission {

        @Test
        @DisplayName("returns 201 when submission created successfully (text-only)")
        void returns201TextOnly() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.PENDING_REVIEW);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Did the work", "Completed feature", null);

            when(submissionService.createSubmission(eq(taskId), any(), isNull()))
                    .thenReturn(response);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/tasks/{taskId}/submissions", taskId)
                            .file(submissionPart(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(subId.toString()))
                    .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"));
        }

        @Test
        @DisplayName("returns 201 when submission created with file attachment")
        void returns201WithFile() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.PENDING_REVIEW);

            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest(
                    "Did the work", null, null);

            MockMultipartFile pdfFile = new MockMultipartFile(
                    "file", "report.pdf", "application/pdf", new byte[1024]);

            when(submissionService.createSubmission(eq(taskId), any(), any()))
                    .thenReturn(response);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/tasks/{taskId}/submissions", taskId)
                            .file(submissionPart(request))
                            .file(pdfFile))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(subId.toString()));
        }

        @Test
        @DisplayName("returns 400 when submissionNotes is blank")
        void returns400WhenNotesBlank() throws Exception {
            UUID taskId = UUID.randomUUID();
            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest("", null, null);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/tasks/{taskId}/submissions", taskId)
                            .file(submissionPart(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when task is not IN_PROGRESS")
        void returns409WhenTaskNotInProgress() throws Exception {
            UUID taskId = UUID.randomUUID();
            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest("Did it", null, null);

            doThrow(new IllegalStateException("Task must be IN_PROGRESS"))
                    .when(submissionService).createSubmission(eq(taskId), any(), any());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/tasks/{taskId}/submissions", taskId)
                            .file(submissionPart(request)))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 403 when employee not assigned to task")
        void returns403WhenNotAssigned() throws Exception {
            UUID taskId = UUID.randomUUID();
            CreateTaskSubmissionRequest request = new CreateTaskSubmissionRequest("Notes", null, null);

            doThrow(new AccessDeniedException("You may only submit tasks that are assigned to you."))
                    .when(submissionService).createSubmission(eq(taskId), any(), any());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/tasks/{taskId}/submissions", taskId)
                            .file(submissionPart(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks/{taskId}/submissions
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks/{taskId}/submissions")
    class GetSubmissions {

        @Test
        @DisplayName("returns 200 with list of submissions")
        void returns200() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.PENDING_REVIEW);

            when(submissionService.getSubmissionsForTask(taskId)).thenReturn(List.of(response));

            mockMvc.perform(get("/tasks/{taskId}/submissions", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value(subId.toString()));
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void returns404() throws Exception {
            UUID missing = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Task", missing))
                    .when(submissionService).getSubmissionsForTask(missing);

            mockMvc.perform(get("/tasks/{taskId}/submissions", missing))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks/{taskId}/submissions/latest
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks/{taskId}/submissions/latest")
    class GetLatestSubmission {

        @Test
        @DisplayName("returns 200 with the latest submission")
        void returns200() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID subId  = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.PENDING_REVIEW);

            when(submissionService.getLatestSubmission(taskId)).thenReturn(response);

            mockMvc.perform(get("/tasks/{taskId}/submissions/latest", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(subId.toString()))
                    .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"));
        }

        @Test
        @DisplayName("returns 404 when no submission exists")
        void returns404() throws Exception {
            UUID taskId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("No submission found for task " + taskId))
                    .when(submissionService).getLatestSubmission(taskId);

            mockMvc.perform(get("/tasks/{taskId}/submissions/latest", taskId))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /task-submissions/{submissionId}/approve
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /task-submissions/{submissionId}/approve")
    class Approve {

        @Test
        @DisplayName("returns 200 when submission approved")
        void returns200() throws Exception {
            UUID subId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.APPROVED);

            when(submissionService.approve(subId)).thenReturn(response);

            mockMvc.perform(post("/task-submissions/{submissionId}/approve", subId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewStatus").value("APPROVED"));
        }

        @Test
        @DisplayName("returns 409 when submission not in PENDING_REVIEW state")
        void returns409() throws Exception {
            UUID subId = UUID.randomUUID();
            doThrow(new IllegalStateException("Only PENDING_REVIEW submissions can be approved."))
                    .when(submissionService).approve(subId);

            mockMvc.perform(post("/task-submissions/{submissionId}/approve", subId))
                    .andExpect(status().isConflict());
        }

        @Test
        @DisplayName("returns 403 when employee tries to approve")
        void returns403ForEmployee() throws Exception {
            UUID subId = UUID.randomUUID();
            doThrow(new AccessDeniedException("Only managers, HR, or admins can perform this action."))
                    .when(submissionService).approve(subId);

            mockMvc.perform(post("/task-submissions/{submissionId}/approve", subId))
                    .andExpect(status().isForbidden());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /task-submissions/{submissionId}/request-changes
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /task-submissions/{submissionId}/request-changes")
    class RequestChanges {

        @Test
        @DisplayName("returns 200 when changes requested")
        void returns200() throws Exception {
            UUID subId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.CHANGES_REQUESTED);
            RequestChangesRequest request = new RequestChangesRequest("Please add tests");

            when(submissionService.requestChanges(eq(subId), any(RequestChangesRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(post("/task-submissions/{submissionId}/request-changes", subId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewStatus").value("CHANGES_REQUESTED"));
        }

        @Test
        @DisplayName("returns 400 when reviewComment is blank")
        void returns400WhenCommentBlank() throws Exception {
            UUID subId = UUID.randomUUID();
            RequestChangesRequest request = new RequestChangesRequest("");

            mockMvc.perform(post("/task-submissions/{submissionId}/request-changes", subId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /task-submissions/{submissionId}/resubmit (multipart)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /task-submissions/{submissionId}/resubmit")
    class Resubmit {

        @Test
        @DisplayName("returns 200 when resubmit successful (text-only)")
        void returns200TextOnly() throws Exception {
            UUID subId  = UUID.randomUUID();
            UUID taskId = UUID.randomUUID();
            TaskSubmissionResponse response = buildResponse(subId, taskId, SubmissionStatus.PENDING_REVIEW);
            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest(
                    "Updated notes", "More work", null);

            when(submissionService.resubmit(eq(subId), any(), isNull()))
                    .thenReturn(response);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/task-submissions/{submissionId}/resubmit", subId)
                            .file(submissionPart(request))
                            .with(r -> { r.setMethod("PUT"); return r; }))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.reviewStatus").value("PENDING_REVIEW"));
        }

        @Test
        @DisplayName("returns 400 when submissionNotes blank")
        void returns400WhenNotesBlank() throws Exception {
            UUID subId = UUID.randomUUID();
            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest("", null, null);

            mockMvc.perform(MockMvcRequestBuilders.multipart("/task-submissions/{submissionId}/resubmit", subId)
                            .file(submissionPart(request))
                            .with(r -> { r.setMethod("PUT"); return r; }))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 when submission not in CHANGES_REQUESTED state")
        void returns409() throws Exception {
            UUID subId = UUID.randomUUID();
            UpdateTaskSubmissionRequest request = new UpdateTaskSubmissionRequest("Updated notes", null, null);

            doThrow(new IllegalStateException("You can only resubmit when changes have been requested."))
                    .when(submissionService).resubmit(eq(subId), any(), any());

            mockMvc.perform(MockMvcRequestBuilders.multipart("/task-submissions/{submissionId}/resubmit", subId)
                            .file(submissionPart(request))
                            .with(r -> { r.setMethod("PUT"); return r; }))
                    .andExpect(status().isConflict());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /task-submissions/{submissionId}/attachment
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /task-submissions/{submissionId}/attachment")
    class DownloadAttachment {

        @Test
        @DisplayName("returns 200 with file stream when attachment exists")
        void returns200WithStream() throws Exception {
            UUID subId = UUID.randomUUID();
            TaskSubmissionService.AttachmentDownload dl = new TaskSubmissionService.AttachmentDownload(
                    new java.io.ByteArrayInputStream(new byte[]{1, 2, 3}),
                    "application/pdf",
                    "report.pdf",
                    3L
            );

            when(submissionService.downloadAttachment(subId)).thenReturn(dl);

            mockMvc.perform(get("/task-submissions/{submissionId}/attachment", subId))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 404 when submission has no attachment")
        void returns404WhenNoAttachment() throws Exception {
            UUID subId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Submission " + subId + " has no file attachment."))
                    .when(submissionService).downloadAttachment(subId);

            mockMvc.perform(get("/task-submissions/{submissionId}/attachment", subId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("returns 403 when employee tries to download another employee's attachment")
        void returns403ForUnauthorizedEmployee() throws Exception {
            UUID subId = UUID.randomUUID();
            doThrow(new AccessDeniedException("You may only download attachments from your own submissions."))
                    .when(submissionService).downloadAttachment(subId);

            mockMvc.perform(get("/task-submissions/{submissionId}/attachment", subId))
                    .andExpect(status().isForbidden());
        }
    }
}
