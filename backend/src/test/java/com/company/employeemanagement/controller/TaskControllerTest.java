package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskStatusRequest;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.TaskService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link TaskController} using standalone MockMvc.
 *
 * <p>Spring Security is excluded — these tests focus on request parsing,
 * response serialisation, HTTP status codes, and service delegation.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaskController")
class TaskControllerTest {

    @Mock
    private TaskService taskService;

    @InjectMocks
    private TaskController taskController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(taskController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private TaskResponse buildResponse(final UUID taskId, final UUID empId) {
        return new TaskResponse(
                taskId, "Test Task", "description", null, null,
                empId, "Jane Doe", "EMP-001",
                UUID.randomUUID(), "Manager Name",
                TaskPriority.MEDIUM, TaskStatus.ASSIGNED, false,
                LocalDate.now().plusDays(7), BigDecimal.valueOf(8),
                TaskCategory.DEVELOPMENT,
                LocalDateTime.now(), LocalDateTime.now(),
                "manager@test.com", "manager@test.com"
        );
    }

    private PageResponse<TaskResponse> buildPage(final TaskResponse response) {
        return new PageResponse<>(
                List.of(response), 0, 20, 1L, 1, true, LocalDateTime.now()
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks")
    class GetAll {

        @Test
        @DisplayName("returns 200 with page of tasks")
        void returns200() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            TaskResponse response = buildResponse(taskId, empId);
            PageResponse<TaskResponse> page = buildPage(response);

            when(taskService.findAll(any(), any(), any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/tasks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(taskId.toString()))
                    .andExpect(jsonPath("$.content[0].title").value("Test Task"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // GET /tasks/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /tasks/{id}")
    class GetById {

        @Test
        @DisplayName("returns 200 when task found")
        void returns200() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            TaskResponse response = buildResponse(taskId, empId);

            when(taskService.findById(taskId)).thenReturn(response);

            mockMvc.perform(get("/tasks/{id}", taskId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(taskId.toString()))
                    .andExpect(jsonPath("$.title").value("Test Task"));
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void returns404() throws Exception {
            UUID missing = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Task", missing))
                    .when(taskService).findById(missing);

            mockMvc.perform(get("/tasks/{id}", missing))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /tasks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /tasks")
    class Create {

        @Test
        @DisplayName("returns 201 on successful creation")
        void returns201() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            TaskResponse response = buildResponse(taskId, empId);

            CreateTaskRequest request = new CreateTaskRequest(
                    "Test Task", "description", null, null,
                    empId, TaskPriority.MEDIUM,
                    LocalDate.now().plusDays(7), BigDecimal.valueOf(8), TaskCategory.DEVELOPMENT
            );

            when(taskService.create(any(CreateTaskRequest.class))).thenReturn(response);

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(taskId.toString()))
                    .andExpect(jsonPath("$.status").value("ASSIGNED"));
        }

        @Test
        @DisplayName("returns 400 when title is blank")
        void returns400WhenTitleBlank() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "", null, null, null, null, null,
                    LocalDate.now().plusDays(1), null, null
            );

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when due date is null")
        void returns400WhenDueDateNull() throws Exception {
            CreateTaskRequest request = new CreateTaskRequest(
                    "Valid Title", null, null, null, null, null,
                    null, null, null
            );

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 404 when assignee employee not found")
        void returns404WhenAssigneeNotFound() throws Exception {
            UUID missingEmpId = UUID.randomUUID();
            CreateTaskRequest request = new CreateTaskRequest(
                    "Title", null, null, null, missingEmpId, null,
                    LocalDate.now().plusDays(1), null, null
            );

            doThrow(new ResourceNotFoundException("Employee", missingEmpId))
                    .when(taskService).create(any());

            mockMvc.perform(post("/tasks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PUT /tasks/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /tasks/{id}")
    class Update {

        @Test
        @DisplayName("returns 200 on successful update")
        void returns200() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            TaskResponse response = buildResponse(taskId, empId);

            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Updated Title", "updated desc", null, null,
                    empId, TaskPriority.HIGH, TaskStatus.IN_PROGRESS,
                    LocalDate.now().plusDays(14), BigDecimal.valueOf(16), TaskCategory.SUPPORT
            );

            when(taskService.update(eq(taskId), any(UpdateTaskRequest.class))).thenReturn(response);

            mockMvc.perform(put("/tasks/{id}", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(taskId.toString()));
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void returns404() throws Exception {
            UUID missing = UUID.randomUUID();
            UpdateTaskRequest request = new UpdateTaskRequest(
                    "Title", null, null, null, null, null, null,
                    LocalDate.now().plusDays(1), null, null
            );

            doThrow(new ResourceNotFoundException("Task", missing))
                    .when(taskService).update(eq(missing), any());

            mockMvc.perform(put("/tasks/{id}", missing)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isNotFound());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PATCH /tasks/{id}/status
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PATCH /tasks/{id}/status")
    class UpdateStatus {

        @Test
        @DisplayName("returns 200 on valid status update")
        void returns200() throws Exception {
            UUID taskId = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            TaskResponse response = buildResponse(taskId, empId);

            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.IN_PROGRESS);

            when(taskService.updateStatus(eq(taskId), any(UpdateTaskStatusRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/tasks/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 400 when status is null")
        void returns400WhenStatusNull() throws Exception {
            UUID taskId = UUID.randomUUID();
            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(null);

            mockMvc.perform(patch("/tasks/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 409 on invalid status transition")
        void returns409OnInvalidTransition() throws Exception {
            UUID taskId = UUID.randomUUID();
            UpdateTaskStatusRequest request = new UpdateTaskStatusRequest(TaskStatus.COMPLETED);

            doThrow(new IllegalStateException("Status transition from ASSIGNED to COMPLETED is not permitted"))
                    .when(taskService).updateStatus(eq(taskId), any());

            mockMvc.perform(patch("/tasks/{id}/status", taskId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isConflict());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE /tasks/{id}
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /tasks/{id}")
    class DeleteTask {

        @Test
        @DisplayName("returns 204 on successful deletion")
        void returns204() throws Exception {
            UUID taskId = UUID.randomUUID();
            doNothing().when(taskService).delete(taskId);

            mockMvc.perform(delete("/tasks/{id}", taskId))
                    .andExpect(status().isNoContent());

            verify(taskService).delete(taskId);
        }

        @Test
        @DisplayName("returns 404 when task not found")
        void returns404() throws Exception {
            UUID missing = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Task", missing))
                    .when(taskService).delete(missing);

            mockMvc.perform(delete("/tasks/{id}", missing))
                    .andExpect(status().isNotFound());
        }
    }
}
