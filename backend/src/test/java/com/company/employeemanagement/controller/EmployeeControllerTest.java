package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.request.CreateEmployeeRequest;
import com.company.employeemanagement.dto.request.UpdateEmployeeRequest;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.EmployeeService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link EmployeeController} using standalone MockMvc.
 *
 * <p>The Spring Security filter chain is excluded here — authentication
 * is tested in the integration test suite. These tests focus purely on
 * request parsing, response serialisation, HTTP status codes, and service
 * delegation.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmployeeController")
class EmployeeControllerTest {

    @Mock  private EmployeeService     employeeService;
    @InjectMocks private EmployeeController employeeController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(employeeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Helpers
    // ──────────────────────────────────────────────────────────────────────────

    private EmployeeResponse buildResponse(final UUID id, final UUID deptId) {
        return new EmployeeResponse(
                id, "EMP-001", deptId, "Engineering",
                null, "John", "Doe", "john@example.com",
                "Software Engineer", null, null,
                LocalDate.of(2024, 1, 15),
                new BigDecimal("75000.00"), EmployeeStatus.ACTIVE,
                LocalDateTime.now(), LocalDateTime.now(),
                null, null
        );
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /employees
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees")
    class GetAll {

        @Test
        @DisplayName("200 OK with page of employees")
        void returns200WithPage() throws Exception {
            UUID empId  = UUID.randomUUID();
            UUID deptId = UUID.randomUUID();
            EmployeeResponse resp = buildResponse(empId, deptId);

            PageResponse<EmployeeResponse> page = new PageResponse<>(
                    List.of(resp), 0, 20, 1L, 1, true, LocalDateTime.now());

            when(employeeService.findAll(any(), any(), any(), any())).thenReturn(page);

            mockMvc.perform(get("/employees"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-001"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // GET /employees/{id}
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /employees/{id}")
    class GetById {

        @Test
        @DisplayName("200 OK when employee exists")
        void returns200WhenFound() throws Exception {
            UUID empId  = UUID.randomUUID();
            UUID deptId = UUID.randomUUID();
            EmployeeResponse resp = buildResponse(empId, deptId);

            when(employeeService.findById(empId)).thenReturn(resp);

            mockMvc.perform(get("/employees/{id}", empId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(empId.toString()))
                    .andExpect(jsonPath("$.jobTitle").value("Software Engineer"));
        }

        @Test
        @DisplayName("404 Not Found when employee does not exist")
        void returns404WhenMissing() throws Exception {
            UUID missingId = UUID.randomUUID();
            when(employeeService.findById(missingId))
                    .thenThrow(new ResourceNotFoundException("Employee", missingId));

            mockMvc.perform(get("/employees/{id}", missingId))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Resource Not Found"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // POST /employees
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("POST /employees")
    class CreateEmployee {

        @Test
        @DisplayName("201 Created when request is valid")
        void returns201OnCreate() throws Exception {
            UUID empId  = UUID.randomUUID();
            UUID deptId = UUID.randomUUID();

            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    null, "EMP-001", deptId, "Software Engineer",
                    null, null, LocalDate.of(2024, 1, 15),
                    new BigDecimal("75000.00"), EmployeeStatus.ACTIVE
            );

            EmployeeResponse response = buildResponse(empId, deptId);
            when(employeeService.create(any(CreateEmployeeRequest.class))).thenReturn(response);

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.employeeCode").value("EMP-001"));
        }

        @Test
        @DisplayName("400 Bad Request when employeeCode is blank")
        void returns400WhenCodeMissing() throws Exception {
            CreateEmployeeRequest request = new CreateEmployeeRequest(
                    null, "", UUID.randomUUID(), "Title",
                    null, null, LocalDate.now(), BigDecimal.ONE, EmployeeStatus.ACTIVE
            );

            mockMvc.perform(post("/employees")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Failed"));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // PUT /employees/{id}
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("PUT /employees/{id}")
    class UpdateEmployee {

        @Test
        @DisplayName("200 OK when update is successful")
        void returns200OnUpdate() throws Exception {
            UUID empId  = UUID.randomUUID();
            UUID deptId = UUID.randomUUID();

            UpdateEmployeeRequest request = new UpdateEmployeeRequest(
                    deptId, "Principal Engineer", null, null,
                    LocalDate.of(2024, 1, 15), new BigDecimal("90000.00"), EmployeeStatus.ACTIVE
            );

            EmployeeResponse response = buildResponse(empId, deptId);
            when(employeeService.update(eq(empId), any(UpdateEmployeeRequest.class))).thenReturn(response);

            mockMvc.perform(put("/employees/{id}", empId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(empId.toString()));
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // DELETE /employees/{id}
    // ──────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("DELETE /employees/{id}")
    class DeleteEmployee {

        @Test
        @DisplayName("204 No Content on successful deletion")
        void returns204OnDelete() throws Exception {
            UUID empId = UUID.randomUUID();
            doNothing().when(employeeService).delete(empId);

            mockMvc.perform(delete("/employees/{id}", empId))
                    .andExpect(status().isNoContent());

            verify(employeeService).delete(empId);
        }

        @Test
        @DisplayName("404 Not Found when employee does not exist")
        void returns404OnMissingEmployee() throws Exception {
            UUID missingId = UUID.randomUUID();
            doThrow(new ResourceNotFoundException("Employee", missingId))
                    .when(employeeService).delete(missingId);

            mockMvc.perform(delete("/employees/{id}", missingId))
                    .andExpect(status().isNotFound());
        }
    }
}
