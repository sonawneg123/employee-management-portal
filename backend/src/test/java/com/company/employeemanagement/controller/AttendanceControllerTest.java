package com.company.employeemanagement.controller;

import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.GlobalExceptionHandler;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.service.AttendanceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
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
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Unit tests for {@link AttendanceController} using standalone MockMvc.
 *
 * <p>Security enforcement (JWT, @PreAuthorize) is exercised by the integration
 * suite ({@link com.company.employeemanagement.api.ApiDocumentationTest} and
 * {@link com.company.employeemanagement.security.RbacSecurityTest}). These
 * tests focus on correct HTTP semantics, service delegation, and the
 * ownership-guard error paths that the service layer raises.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceController")
class AttendanceControllerTest {

    @Mock
    private AttendanceService attendanceService;

    @InjectMocks
    private AttendanceController attendanceController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        mockMvc = MockMvcBuilders
                .standaloneSetup(attendanceController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private AttendanceResponse buildRecord(final UUID id, final UUID employeeId) {
        return new AttendanceResponse(
                id,
                employeeId,
                "EMP-001",
                "Jane Doe",
                LocalDate.of(2025, 7, 1),
                LocalTime.of(9, 0),
                LocalTime.of(17, 0),
                AttendanceStatus.PRESENT,
                null,
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }

    private PageResponse<AttendanceResponse> singlePage(final AttendanceResponse rec) {
        return new PageResponse<>(List.of(rec), 0, 20, 1L, 1, true, LocalDateTime.now());
    }

    // ── GET /attendance (admin/HR list) ──────────────────────────────────────

    @Nested
    @DisplayName("GET /attendance")
    class FindAll {

        @Test
        @DisplayName("200 OK — returns paginated attendance records")
        void returns200WithPage() throws Exception {
            UUID recId  = UUID.randomUUID();
            UUID empId  = UUID.randomUUID();
            AttendanceResponse rec = buildRecord(recId, empId);

            when(attendanceService.findAll(isNull(), isNull(), isNull(), any()))
                    .thenReturn(singlePage(rec));

            mockMvc.perform(get("/attendance"))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.totalElements").value(1))
                    .andExpect(jsonPath("$.content[0].employeeCode").value("EMP-001"))
                    .andExpect(jsonPath("$.content[0].employeeName").value("Jane Doe"))
                    .andExpect(jsonPath("$.content[0].status").value("PRESENT"));
        }

        @Test
        @DisplayName("200 OK — employeeId filter is forwarded to service")
        void forwardsEmployeeIdFilter() throws Exception {
            UUID empId = UUID.randomUUID();
            AttendanceResponse rec = buildRecord(UUID.randomUUID(), empId);

            when(attendanceService.findAll(eq(empId), isNull(), isNull(), any()))
                    .thenReturn(singlePage(rec));

            mockMvc.perform(get("/attendance").param("employeeId", empId.toString()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        @DisplayName("200 OK — date filter is forwarded to service")
        void forwardsDateFilter() throws Exception {
            UUID empId = UUID.randomUUID();
            AttendanceResponse rec = buildRecord(UUID.randomUUID(), empId);

            when(attendanceService.findAll(isNull(), eq(LocalDate.of(2025, 7, 1)), isNull(), any()))
                    .thenReturn(singlePage(rec));

            mockMvc.perform(get("/attendance").param("date", "2025-07-01"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].attendanceDate").value("2025-07-01"));
        }

        @Test
        @DisplayName("200 OK — status filter is forwarded to service")
        void forwardsStatusFilter() throws Exception {
            UUID empId = UUID.randomUUID();
            AttendanceResponse rec = buildRecord(UUID.randomUUID(), empId);

            when(attendanceService.findAll(isNull(), isNull(), eq(AttendanceStatus.PRESENT), any()))
                    .thenReturn(singlePage(rec));

            mockMvc.perform(get("/attendance").param("status", "PRESENT"))
                    .andExpect(status().isOk());
        }
    }

    // ── GET /attendance/my ───────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /attendance/my")
    class MyAttendance {

        @Test
        @DisplayName("200 OK — returns caller-scoped records")
        void returns200WithOwnRecords() throws Exception {
            UUID recId = UUID.randomUUID();
            UUID empId = UUID.randomUUID();
            AttendanceResponse rec = buildRecord(recId, empId);

            when(attendanceService.findMyAttendance(isNull(), isNull(), any()))
                    .thenReturn(singlePage(rec));

            mockMvc.perform(get("/attendance/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].employeeName").value("Jane Doe"))
                    .andExpect(jsonPath("$.content[0].status").value("PRESENT"));
        }

        @Test
        @DisplayName("200 OK — date filter is forwarded to service")
        void forwardsDateFilter() throws Exception {
            when(attendanceService.findMyAttendance(eq(LocalDate.of(2025, 7, 1)), isNull(), any()))
                    .thenReturn(singlePage(buildRecord(UUID.randomUUID(), UUID.randomUUID())));

            mockMvc.perform(get("/attendance/my").param("date", "2025-07-01"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 OK — status filter is forwarded to service")
        void forwardsStatusFilter() throws Exception {
            when(attendanceService.findMyAttendance(isNull(), eq(AttendanceStatus.WORK_FROM_HOME), any()))
                    .thenReturn(singlePage(buildRecord(UUID.randomUUID(), UUID.randomUUID())));

            mockMvc.perform(get("/attendance/my").param("status", "WORK_FROM_HOME"))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("200 OK — empty page when employee has no records")
        void returns200WithEmptyPage() throws Exception {
            PageResponse<AttendanceResponse> empty =
                    new PageResponse<>(List.of(), 0, 20, 0L, 0, true, LocalDateTime.now());

            when(attendanceService.findMyAttendance(isNull(), isNull(), any()))
                    .thenReturn(empty);

            mockMvc.perform(get("/attendance/my"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.totalElements").value(0))
                    .andExpect(jsonPath("$.content").isEmpty());
        }

        @Test
        @DisplayName("403 — service throws AccessDeniedException (no linked employee record)")
        void returns403WhenNoLinkedEmployee() throws Exception {
            when(attendanceService.findMyAttendance(isNull(), isNull(), any()))
                    .thenThrow(new AccessDeniedException(
                            "No employee record is linked to your account."));

            mockMvc.perform(get("/attendance/my"))
                    .andExpect(status().isForbidden());
        }
    }

    // ── GET /attendance/{id} ─────────────────────────────────────────────────

    @Nested
    @DisplayName("GET /attendance/{id}")
    class FindById {

        @Test
        @DisplayName("200 OK — returns attendance record")
        void returns200WhenFound() throws Exception {
            UUID recId = UUID.randomUUID();
            UUID empId = UUID.randomUUID();
            AttendanceResponse rec = buildRecord(recId, empId);

            when(attendanceService.findById(recId)).thenReturn(rec);

            mockMvc.perform(get("/attendance/{id}", recId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(recId.toString()))
                    .andExpect(jsonPath("$.employeeName").value("Jane Doe"));
        }

        @Test
        @DisplayName("404 — service throws ResourceNotFoundException")
        void returns404WhenNotFound() throws Exception {
            UUID recId = UUID.randomUUID();
            when(attendanceService.findById(recId))
                    .thenThrow(new ResourceNotFoundException("Attendance", recId));

            mockMvc.perform(get("/attendance/{id}", recId))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("403 — service throws AccessDeniedException when employee accesses another's record")
        void returns403WhenAccessingOtherEmployeeRecord() throws Exception {
            UUID recId = UUID.randomUUID();
            // Service enforces ownership: employee can only fetch their own records
            when(attendanceService.findById(recId))
                    .thenThrow(new AccessDeniedException(
                            "You may only access your own attendance records."));

            mockMvc.perform(get("/attendance/{id}", recId))
                    .andExpect(status().isForbidden());
        }
    }
}
