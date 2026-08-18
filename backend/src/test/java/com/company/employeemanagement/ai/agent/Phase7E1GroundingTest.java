package com.company.employeemanagement.ai.agent;

import com.company.employeemanagement.ai.agent.dto.AgentChatRequest;
import com.company.employeemanagement.ai.agent.dto.AgentChatResponse;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
import com.company.employeemanagement.ai.agent.service.AgentActionExecutor;
import com.company.employeemanagement.ai.agent.service.AiAgentService;
import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.ai.agent.tool.AgentToolRegistry;
import com.company.employeemanagement.ai.agent.tool.AiAgentTool;
import com.company.employeemanagement.ai.agent.tool.impl.GetCurrentEmployeeTool;
import com.company.employeemanagement.ai.agent.tool.impl.GetEmployeeAttendanceTool;
import com.company.employeemanagement.ai.agent.tool.impl.GetEmployeeAvailabilityTool;
import com.company.employeemanagement.ai.agent.tool.impl.GetEmployeeWorkloadTool;
import com.company.employeemanagement.ai.agent.tool.impl.GetLeaveRequestsTool;
import com.company.employeemanagement.ai.agent.tool.impl.GetTaskTool;
import com.company.employeemanagement.ai.agent.tool.impl.SearchEmployeesTool;
import com.company.employeemanagement.ai.agent.tool.impl.SearchTasksTool;
import com.company.employeemanagement.ai.agent.repository.AiAgentAuditLogRepository;
import com.company.employeemanagement.ai.client.GroqClient;
import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.service.KnowledgeRetrievalService;
import com.company.employeemanagement.ai.rag.service.RagPromptContextBuilder;
import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.dto.response.EmployeeResponse;
import com.company.employeemanagement.dto.response.LeaveRequestResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.dto.response.WorkloadResponse;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.UserRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.AttendanceService;
import com.company.employeemanagement.service.EmployeeService;
import com.company.employeemanagement.service.LeaveRequestService;
import com.company.employeemanagement.service.TaskService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Phase 7E.1 — Grounding tests for the Agentic AI Copilot.
 *
 * <p>These tests verify that:
 * <ol>
 *   <li>Each tool executes correctly and returns live data.</li>
 *   <li>Tool results are injected back into the agent conversation.</li>
 *   <li>The final answer is grounded in the tool result, not model knowledge.</li>
 *   <li>Security: employees see only their own data; unauthorised tools are blocked.</li>
 *   <li>Edge cases: empty results, invalid IDs, tool execution failure.</li>
 * </ol>
 *
 * <h2>Test coverage</h2>
 * <ul>
 *   <li>get_current_employee — profile retrieval</li>
 *   <li>search_tasks — task list for employee and manager</li>
 *   <li>search_employees — employee lookup (manager/HR only)</li>
 *   <li>get_task — specific task by ID</li>
 *   <li>get_employee_workload — workload summary</li>
 *   <li>get_employee_attendance — attendance (employee + manager paths)</li>
 *   <li>get_leave_requests — leave requests (employee + manager paths)</li>
 *   <li>get_employee_availability — availability check</li>
 *   <li>Empty results, invalid IDs, unauthorized access, tool execution failure</li>
 *   <li>Full agent loop: tool call → result injection → grounded final answer</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Phase7E1GroundingTest {

    // ── Service mocks ───────────────────────────────────────────────────────

    @Mock private GroqClient groqClient;
    @Mock private AiAgentAuditLogRepository auditLogRepository;
    @Mock private SecurityUtils securityUtils;
    @Mock private KnowledgeRetrievalService retrievalService;
    @Mock private RagPromptContextBuilder contextBuilder;
    @Mock private UserRepository userRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private EmployeeService employeeService;
    @Mock private TaskService taskService;
    @Mock private AttendanceService attendanceService;
    @Mock private LeaveRequestService leaveRequestService;
    @Mock private SecurityContext securityContext;
    @Mock private Authentication authentication;

    // ── Fixed test data ─────────────────────────────────────────────────────

    private static final UUID EMPLOYEE_ID    = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID        = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID TASK_ID        = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID LEAVE_ID       = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID ATTENDANCE_ID  = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final String USERNAME     = "employee@test.com";
    private static final String MANAGER_USERNAME = "manager@test.com";

    private Employee currentEmployee;

    // ── Tools under test ────────────────────────────────────────────────────

    private GetCurrentEmployeeTool getCurrentEmployeeTool;
    private SearchTasksTool searchTasksTool;
    private SearchEmployeesTool searchEmployeesTool;
    private GetTaskTool getTaskTool;
    private GetEmployeeWorkloadTool getEmployeeWorkloadTool;
    private GetEmployeeAttendanceTool getEmployeeAttendanceTool;
    private GetLeaveRequestsTool getLeaveRequestsTool;
    private GetEmployeeAvailabilityTool getEmployeeAvailabilityTool;

    @BeforeEach
    void setUp() {
        currentEmployee = new Employee();
        currentEmployee.setId(EMPLOYEE_ID);

        // Initialise tools
        getCurrentEmployeeTool    = new GetCurrentEmployeeTool(securityUtils, employeeService);
        searchTasksTool           = new SearchTasksTool(taskService);
        searchEmployeesTool       = new SearchEmployeesTool(employeeService);
        getTaskTool               = new GetTaskTool(taskService);
        getEmployeeWorkloadTool   = new GetEmployeeWorkloadTool(taskService);
        getEmployeeAttendanceTool = new GetEmployeeAttendanceTool(attendanceService);
        getLeaveRequestsTool      = new GetLeaveRequestsTool(leaveRequestService);
        getEmployeeAvailabilityTool = new GetEmployeeAvailabilityTool(taskService);

        // RAG disabled by default for agent tests
        when(retrievalService.search(any())).thenReturn(List.of());
    }

    // ── Helper factories ────────────────────────────────────────────────────

    private AgentToolContext employeeContext() {
        return new AgentToolContext(USER_ID, USERNAME,
                Set.of("ROLE_EMPLOYEE"), currentEmployee);
    }

    private AgentToolContext managerContext() {
        return new AgentToolContext(USER_ID, MANAGER_USERNAME,
                Set.of("ROLE_MANAGER"), currentEmployee);
    }

    private AgentToolContext hrContext() {
        return new AgentToolContext(USER_ID, "hr@test.com",
                Set.of("ROLE_HR"), currentEmployee);
    }

    private PageResponse<TaskResponse> singleTaskPage(final TaskResponse task) {
        return PageResponse.from(new PageImpl<>(List.of(task)));
    }

    private PageResponse<EmployeeResponse> singleEmployeePage(final EmployeeResponse emp) {
        return PageResponse.from(new PageImpl<>(List.of(emp)));
    }

    private PageResponse<AttendanceResponse> singleAttendancePage(final AttendanceResponse a) {
        return PageResponse.from(new PageImpl<>(List.of(a)));
    }

    private PageResponse<LeaveRequestResponse> singleLeavePage(final LeaveRequestResponse l) {
        return PageResponse.from(new PageImpl<>(List.of(l)));
    }

    private EmployeeResponse sampleEmployeeResponse() {
        return new EmployeeResponse(
                EMPLOYEE_ID, "EMP-0001", UUID.randomUUID(), "Engineering",
                USER_ID, "Jane", "Doe", "jane.doe@test.com",
                "Software Engineer", null, null,
                LocalDate.of(2023, 1, 1), BigDecimal.valueOf(70000),
                EmployeeStatus.ACTIVE, LocalDateTime.now(), LocalDateTime.now(),
                "admin@test.com", "admin@test.com", null);
    }

    private TaskResponse sampleTaskResponse() {
        return new TaskResponse(
                TASK_ID, "Implement login page", "Login page description",
                null, null, EMPLOYEE_ID, "Jane Doe", "EMP-0001",
                UUID.randomUUID(), "Manager Bob",
                TaskPriority.MEDIUM, TaskStatus.ASSIGNED, false,
                LocalDate.now().plusDays(5), BigDecimal.valueOf(8),
                null, LocalDateTime.now(), LocalDateTime.now(),
                "manager@test.com", "manager@test.com");
    }

    private AttendanceResponse sampleAttendanceResponse() {
        return new AttendanceResponse(
                ATTENDANCE_ID, EMPLOYEE_ID, "EMP-0001", "Jane Doe",
                LocalDate.now(), LocalTime.of(9, 0), null,
                AttendanceStatus.PRESENT, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private LeaveRequestResponse sampleLeaveResponse() {
        return new LeaveRequestResponse(
                LEAVE_ID, EMPLOYEE_ID, "EMP-0001", "Jane Doe", "Engineering",
                LeaveType.ANNUAL, LocalDate.now(), LocalDate.now().plusDays(4),
                5, "Family vacation", LeaveStatus.APPROVED, null,
                null, null, LocalDateTime.now(), LocalDateTime.now(),
                "jane.doe@test.com", "hr@test.com");
    }

    private WorkloadResponse sampleWorkloadResponse() {
        return new WorkloadResponse(EMPLOYEE_ID, "Jane Doe", 3, 1, 0,
                WorkloadResponse.WorkloadLevel.MEDIUM);
    }

    private EmployeeAvailabilityResponse sampleAvailabilityResponse() {
        return new EmployeeAvailabilityResponse(
                EMPLOYEE_ID, "Jane Doe", "EMP-0001",
                true, 3, false, true, false,
                null, null);
    }

    // ══════════════════════════════════════════════════════════════════════
    // 1. Tool registration and role access
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tool Registration and Role Access")
    class ToolRegistrationTests {

        @Test
        @DisplayName("All 8 read tools are registered in the registry")
        void allReadToolsAreRegistered() {
            AgentToolRegistry registry = new AgentToolRegistry(List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool));

            assertThat(registry.findByName("get_current_employee")).isPresent();
            assertThat(registry.findByName("search_tasks")).isPresent();
            assertThat(registry.findByName("search_employees")).isPresent();
            assertThat(registry.findByName("get_task")).isPresent();
            assertThat(registry.findByName("get_employee_workload")).isPresent();
            assertThat(registry.findByName("get_employee_attendance")).isPresent();
            assertThat(registry.findByName("get_leave_requests")).isPresent();
            assertThat(registry.findByName("get_employee_availability")).isPresent();
        }

        @Test
        @DisplayName("ROLE_EMPLOYEE can access get_current_employee, search_tasks, get_task, get_leave_requests, get_employee_attendance")
        void employeeRoleHasCorrectToolAccess() {
            AgentToolRegistry registry = new AgentToolRegistry(List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool));

            List<AiAgentTool> employeeTools = registry.toolsForRoles(Set.of("ROLE_EMPLOYEE"));
            List<String> names = employeeTools.stream().map(AiAgentTool::getName).toList();

            assertThat(names).contains(
                    "get_current_employee",
                    "search_tasks",
                    "get_task",
                    "get_leave_requests",
                    "get_employee_attendance");
            // Employees must NOT access manager-only tools
            assertThat(names).doesNotContain(
                    "search_employees",
                    "get_employee_workload",
                    "get_employee_availability");
        }

        @Test
        @DisplayName("ROLE_MANAGER can access all tools including manager-only tools")
        void managerRoleHasFullToolAccess() {
            AgentToolRegistry registry = new AgentToolRegistry(List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool));

            List<AiAgentTool> managerTools = registry.toolsForRoles(Set.of("ROLE_MANAGER"));
            List<String> names = managerTools.stream().map(AiAgentTool::getName).toList();

            assertThat(names).containsExactlyInAnyOrder(
                    "get_current_employee",
                    "search_tasks",
                    "search_employees",
                    "get_task",
                    "get_employee_workload",
                    "get_employee_attendance",
                    "get_leave_requests",
                    "get_employee_availability");
        }

        @Test
        @DisplayName("get_employee_attendance is now accessible to ROLE_EMPLOYEE")
        void attendanceToolAllowsEmployeeRole() {
            assertThat(getEmployeeAttendanceTool.getAllowedRoles()).contains("ROLE_EMPLOYEE");
        }

        @Test
        @DisplayName("All read tools are marked isReadOnly=true and isRequiresConfirmation=false")
        void readToolsAreReadOnlyAndNoConfirmation() {
            List<AiAgentTool> readTools = List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool);

            for (AiAgentTool tool : readTools) {
                assertThat(tool.isReadOnly())
                        .as("Tool %s should be read-only", tool.getName()).isTrue();
                assertThat(tool.isRequiresConfirmation())
                        .as("Tool %s should not require confirmation", tool.getName()).isFalse();
            }
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 2. Tool execution — live data retrieval
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tool Execution — Live Data Retrieval")
    class ToolExecutionTests {

        @Test
        @DisplayName("get_current_employee returns actual employee profile from service")
        void getCurrentEmployeeReturnsActualProfile() {
            EmployeeResponse emp = sampleEmployeeResponse();
            when(employeeService.findById(EMPLOYEE_ID)).thenReturn(emp);

            String result = getCurrentEmployeeTool.execute("{}", employeeContext());

            assertThat(result).contains("Jane").contains("Doe").contains("EMP-0001")
                    .contains("Engineering").contains("Software Engineer");
            // Salary must NOT be present
            assertThat(result).doesNotContain("70000");
            verify(employeeService).findById(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("get_current_employee returns graceful message when no employee record linked")
        void getCurrentEmployeeNoRecord() {
            AgentToolContext noEmpContext = new AgentToolContext(USER_ID, USERNAME,
                    Set.of("ROLE_EMPLOYEE"), null);

            String result = getCurrentEmployeeTool.execute("{}", noEmpContext);

            assertThat(result).contains("No employee record");
            verifyNoInteractions(employeeService);
        }

        @Test
        @DisplayName("search_tasks returns real task list from TaskService")
        void searchTasksReturnsDatabaseTasks() {
            TaskResponse task = sampleTaskResponse();
            when(taskService.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(singleTaskPage(task));

            String result = searchTasksTool.execute("{}", managerContext());

            assertThat(result).contains("Implement login page")
                    .contains("ASSIGNED")
                    .contains("MEDIUM");
            verify(taskService).findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("search_tasks with status filter delegates status to TaskService")
        void searchTasksWithStatusFilter() {
            when(taskService.findAll(isNull(), isNull(), eq(TaskStatus.ASSIGNED), isNull(), isNull(), any()))
                    .thenReturn(singleTaskPage(sampleTaskResponse()));

            String result = searchTasksTool.execute("{\"status\":\"ASSIGNED\"}", managerContext());

            assertThat(result).contains("ASSIGNED");
            verify(taskService).findAll(isNull(), isNull(), eq(TaskStatus.ASSIGNED), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("search_tasks with invalid status returns error message, not exception")
        void searchTasksInvalidStatus() {
            String result = searchTasksTool.execute("{\"status\":\"NOT_A_STATUS\"}", managerContext());

            assertThat(result).containsIgnoringCase("Invalid status");
            verifyNoInteractions(taskService);
        }

        @Test
        @DisplayName("search_employees returns employee list from EmployeeService")
        void searchEmployeesReturnsActualEmployees() {
            EmployeeResponse emp = sampleEmployeeResponse();
            when(employeeService.findAll(eq("Engineering"), isNull(), isNull(), any()))
                    .thenReturn(singleEmployeePage(emp));

            String result = searchEmployeesTool.execute("{\"keyword\":\"Engineering\"}", managerContext());

            assertThat(result).contains("Jane").contains("Doe").contains("Engineering");
            verify(employeeService).findAll(eq("Engineering"), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("get_task returns actual task details from TaskService")
        void getTaskReturnsActualDetails() {
            TaskResponse task = sampleTaskResponse();
            when(taskService.findById(TASK_ID)).thenReturn(task);

            String result = getTaskTool.execute("{\"taskId\":\"" + TASK_ID + "\"}", managerContext());

            assertThat(result).contains("Implement login page")
                    .contains("ASSIGNED")
                    .contains("MEDIUM")
                    .contains(TASK_ID.toString());
            verify(taskService).findById(TASK_ID);
        }

        @Test
        @DisplayName("get_task with missing taskId parameter returns error, not exception")
        void getTaskMissingId() {
            String result = getTaskTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("required");
            verifyNoInteractions(taskService);
        }

        @Test
        @DisplayName("get_task with invalid UUID format returns error, not exception")
        void getTaskInvalidUuid() {
            String result = getTaskTool.execute("{\"taskId\":\"not-a-uuid\"}", managerContext());

            assertThat(result).containsIgnoringCase("Invalid");
            verifyNoInteractions(taskService);
        }

        @Test
        @DisplayName("get_employee_workload returns actual workload from TaskService")
        void getEmployeeWorkloadReturnsActualData() {
            WorkloadResponse workload = sampleWorkloadResponse();
            when(taskService.getWorkloadSummary()).thenReturn(List.of(workload));

            String result = getEmployeeWorkloadTool.execute("{}", managerContext());

            assertThat(result).contains("Jane Doe")
                    .contains("Active: 3")
                    .contains("MEDIUM");
            verify(taskService).getWorkloadSummary();
        }

        @Test
        @DisplayName("get_employee_workload with specific employeeId delegates to getWorkload")
        void getEmployeeWorkloadSingleEmployee() {
            WorkloadResponse workload = sampleWorkloadResponse();
            when(taskService.getWorkload(EMPLOYEE_ID)).thenReturn(workload);

            String result = getEmployeeWorkloadTool.execute(
                    "{\"employeeId\":\"" + EMPLOYEE_ID + "\"}", managerContext());

            assertThat(result).contains("Jane Doe");
            verify(taskService).getWorkload(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("get_employee_attendance — EMPLOYEE role uses findMyAttendance (scoped)")
        void attendanceToolEmployeeUsesScoped() {
            AttendanceResponse attendance = sampleAttendanceResponse();
            when(attendanceService.findMyAttendance(any(), isNull(), any()))
                    .thenReturn(singleAttendancePage(attendance));

            String result = getEmployeeAttendanceTool.execute("{}", employeeContext());

            assertThat(result).contains("Jane Doe").contains("PRESENT");
            // Must use scoped method — NOT findAll
            verify(attendanceService).findMyAttendance(any(), isNull(), any());
            verify(attendanceService, never()).findAll(any(), any(), any(), any());
        }

        @Test
        @DisplayName("get_employee_attendance — MANAGER role uses findAll (unrestricted)")
        void attendanceToolManagerUsesUnrestricted() {
            AttendanceResponse attendance = sampleAttendanceResponse();
            when(attendanceService.findAll(isNull(), any(), isNull(), any()))
                    .thenReturn(singleAttendancePage(attendance));

            String result = getEmployeeAttendanceTool.execute("{}", managerContext());

            assertThat(result).contains("Jane Doe").contains("PRESENT");
            verify(attendanceService).findAll(isNull(), any(), isNull(), any());
            verify(attendanceService, never()).findMyAttendance(any(), any(), any());
        }

        @Test
        @DisplayName("get_leave_requests — EMPLOYEE role uses findMyLeaves (scoped)")
        void leaveToolEmployeeUsesScoped() {
            LeaveRequestResponse leave = sampleLeaveResponse();
            when(leaveRequestService.findMyLeaves(isNull(), isNull(), any()))
                    .thenReturn(singleLeavePage(leave));

            String result = getLeaveRequestsTool.execute("{}", employeeContext());

            assertThat(result).contains("Jane Doe").contains("APPROVED").contains("ANNUAL");
            verify(leaveRequestService).findMyLeaves(isNull(), isNull(), any());
            verify(leaveRequestService, never()).findAll(any(), any(), any(), any());
        }

        @Test
        @DisplayName("get_leave_requests — MANAGER role uses findAll (unrestricted)")
        void leaveToolManagerUsesUnrestricted() {
            LeaveRequestResponse leave = sampleLeaveResponse();
            when(leaveRequestService.findAll(isNull(), isNull(), isNull(), any()))
                    .thenReturn(singleLeavePage(leave));

            String result = getLeaveRequestsTool.execute("{}", managerContext());

            assertThat(result).contains("Jane Doe").contains("APPROVED");
            verify(leaveRequestService).findAll(isNull(), isNull(), isNull(), any());
            verify(leaveRequestService, never()).findMyLeaves(any(), any(), any());
        }

        @Test
        @DisplayName("get_employee_availability returns actual availability from TaskService")
        void availabilityToolReturnsActualData() {
            EmployeeAvailabilityResponse avail = sampleAvailabilityResponse();
            when(taskService.getEmployeeAvailability()).thenReturn(List.of(avail));

            String result = getEmployeeAvailabilityTool.execute("{}", managerContext());

            assertThat(result).contains("Jane Doe")
                    .contains("Available: YES")
                    .contains("Checked in");
            verify(taskService).getEmployeeAvailability();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 3. Empty results
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Empty Results — No Hallucination")
    class EmptyResultTests {

        @Test
        @DisplayName("search_tasks returns 'no tasks found' message when service returns empty page")
        void searchTasksEmptyResult() {
            when(taskService.findAll(any(), any(), any(), any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = searchTasksTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No tasks found");
        }

        @Test
        @DisplayName("search_employees returns 'no employees found' message on empty result")
        void searchEmployeesEmptyResult() {
            when(employeeService.findAll(isNull(), isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = searchEmployeesTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No employees found");
        }

        @Test
        @DisplayName("get_employee_workload returns 'no workload data' message on empty list")
        void workloadEmptyResult() {
            when(taskService.getWorkloadSummary()).thenReturn(List.of());

            String result = getEmployeeWorkloadTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No workload data");
        }

        @Test
        @DisplayName("get_employee_availability returns 'no availability data' message on empty list")
        void availabilityEmptyResult() {
            when(taskService.getEmployeeAvailability()).thenReturn(List.of());

            String result = getEmployeeAvailabilityTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No employee availability data");
        }

        @Test
        @DisplayName("get_employee_attendance returns 'no records found' message on empty page")
        void attendanceEmptyResult() {
            when(attendanceService.findAll(any(), any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = getEmployeeAttendanceTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No attendance records");
        }

        @Test
        @DisplayName("get_leave_requests returns 'no leave requests found' message on empty page")
        void leaveEmptyResult() {
            when(leaveRequestService.findAll(any(), any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = getLeaveRequestsTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No leave requests found");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 4. Security — unauthorized access and data isolation
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Security — Unauthorized Access and Data Isolation")
    class SecurityTests {

        @Test
        @DisplayName("EMPLOYEE role cannot access search_employees via registry")
        void employeeCannotAccessSearchEmployees() {
            AgentToolRegistry registry = new AgentToolRegistry(List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool));

            List<AiAgentTool> allowed = registry.toolsForRoles(Set.of("ROLE_EMPLOYEE"));

            assertThat(allowed.stream().map(AiAgentTool::getName))
                    .doesNotContain("search_employees", "get_employee_workload", "get_employee_availability");
        }

        @Test
        @DisplayName("EMPLOYEE role cannot access get_employee_workload via registry")
        void employeeCannotAccessWorkloadTool() {
            assertThat(getEmployeeWorkloadTool.getAllowedRoles()).doesNotContain("ROLE_EMPLOYEE");
        }

        @Test
        @DisplayName("EMPLOYEE role cannot access get_employee_availability via registry")
        void employeeCannotAccessAvailabilityTool() {
            assertThat(getEmployeeAvailabilityTool.getAllowedRoles()).doesNotContain("ROLE_EMPLOYEE");
        }

        @Test
        @DisplayName("Tool execution failure is caught and returned as safe error string, not re-thrown")
        void toolExecutionFailureReturnsSafeError() {
            when(taskService.getWorkloadSummary())
                    .thenThrow(new RuntimeException("DB connection failed"));

            String result = getEmployeeWorkloadTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("Error");
            // No exception propagated to caller
        }

        @Test
        @DisplayName("ResourceNotFoundException in tool is caught and returned as 'not found' message")
        void resourceNotFoundReturnsSafeMessage() {
            when(taskService.findById(TASK_ID))
                    .thenThrow(new com.company.employeemanagement.exception.ResourceNotFoundException("Task", TASK_ID));

            String result = getTaskTool.execute("{\"taskId\":\"" + TASK_ID + "\"}", managerContext());

            assertThat(result).containsIgnoringCase("Not found");
        }

        @Test
        @DisplayName("AccessDeniedException in tool is caught and returned as 'access denied' message")
        void accessDeniedReturnsSafeMessage() {
            when(taskService.findById(TASK_ID))
                    .thenThrow(new com.company.employeemanagement.exception.AccessDeniedException("Not your task"));

            String result = getTaskTool.execute("{\"taskId\":\"" + TASK_ID + "\"}", employeeContext());

            assertThat(result).containsIgnoringCase("Access denied");
        }

        @Test
        @DisplayName("Employee leave tool: EMPLOYEE sees only own leaves, not all employees")
        void employeeLeaveScopedToOwnLeaves() {
            when(leaveRequestService.findMyLeaves(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            getLeaveRequestsTool.execute("{}", employeeContext());

            // Must NOT call the unrestricted findAll
            verify(leaveRequestService, never()).findAll(any(), any(), any(), any());
            verify(leaveRequestService).findMyLeaves(isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Employee attendance tool: EMPLOYEE sees only own attendance, not all employees")
        void employeeAttendanceScopedToOwnRecords() {
            when(attendanceService.findMyAttendance(any(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            getEmployeeAttendanceTool.execute("{}", employeeContext());

            // Must NOT call the unrestricted findAll
            verify(attendanceService, never()).findAll(any(), any(), any(), any());
            verify(attendanceService).findMyAttendance(any(), isNull(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 5. Agent loop — tool result injection
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Agent Loop — Tool Result Injection and Grounding")
    class AgentLoopTests {

        private AiAgentService buildAgentService(final AiAgentTool... tools) {
            AgentToolRegistry registry = new AgentToolRegistry(List.of(tools));
            AgentConfirmationStore confirmationStore = new AgentConfirmationStore();
            RagProperties ragProperties = new RagProperties();
            ragProperties.setEnabled(false);

            AiAgentService service = new AiAgentService(
                    groqClient, registry, confirmationStore,
                    mock(AgentActionExecutor.class),
                    auditLogRepository, securityUtils,
                    retrievalService, contextBuilder, ragProperties,
                    userRepository, employeeRepository);

            ReflectionTestUtils.setField(service, "maxToolCalls", 8);
            return service;
        }

        private void mockSecurityContext(final String username, final String role) {
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(username);
            when(authentication.getAuthorities()).thenAnswer(inv ->
                    List.of(new SimpleGrantedAuthority(role)));
            when(userRepository.findByEmail(username))
                    .thenReturn(Optional.empty()); // Simplified — no User entity needed
        }

        @Test
        @DisplayName("Agent loop: Groq selects get_current_employee, tool result is injected, Groq produces grounded answer")
        void agentLoopGetCurrentEmployeeGrounded() {
            // Set up security context so buildContext() finds the correct employee
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(USERNAME);
            when(authentication.getAuthorities()).thenAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));

            // Mock the user → employee resolution chain used by AiAgentService.buildContext()
            com.company.employeemanagement.entity.User mockUser =
                    com.company.employeemanagement.entity.User.builder().build();
            mockUser.setId(USER_ID);
            when(userRepository.findByEmail(USERNAME)).thenReturn(Optional.of(mockUser));
            when(employeeRepository.findByUserId(USER_ID)).thenReturn(Optional.of(currentEmployee));

            EmployeeResponse emp = sampleEmployeeResponse();
            when(employeeService.findById(EMPLOYEE_ID)).thenReturn(emp);
            when(auditLogRepository.save(any())).thenReturn(null);

            // First Groq call: requests the tool
            String toolCallJson = "{\"tool_call\":{\"name\":\"get_current_employee\",\"arguments\":{}}}";
            // Second Groq call: produces grounded answer after receiving tool result
            String groundedAnswer = "Your profile: Jane Doe (EMP-0001), Software Engineer in Engineering.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCallJson)
                    .thenReturn(groundedAnswer);

            AiAgentService agentService = buildAgentService(getCurrentEmployeeTool);

            AgentChatRequest request = new AgentChatRequest("What is my employee profile?");
            AgentChatResponse response = agentService.chat(request);

            // Tool was called and the employee service was queried for live data
            verify(employeeService).findById(EMPLOYEE_ID);
            // Final grounded answer is returned
            assertThat(response.answer()).isEqualTo(groundedAnswer);
            assertThat(response.toolsExecuted()).contains("get_current_employee");
        }

        @Test
        @DisplayName("Agent loop: tool call followed by final answer completes in two Groq calls")
        void agentLoopTwoCallsToolThenFinalAnswer() {
            mockSecurityContext(MANAGER_USERNAME, "ROLE_MANAGER");
            when(taskService.getWorkloadSummary()).thenReturn(List.of(sampleWorkloadResponse()));
            when(auditLogRepository.save(any())).thenReturn(null);

            String toolCallJson = "{\"tool_call\":{\"name\":\"get_employee_workload\",\"arguments\":{}}}";
            String groundedAnswer = "Jane Doe has the highest workload: 3 active tasks (MEDIUM level).";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCallJson)
                    .thenReturn(groundedAnswer);

            AiAgentService agentService = buildAgentService(getEmployeeWorkloadTool);

            AgentChatRequest request = new AgentChatRequest("Who has the highest workload?");
            AgentChatResponse response = agentService.chat(request);

            assertThat(response.toolsExecuted()).contains("get_employee_workload");
            assertThat(response.answer()).isEqualTo(groundedAnswer);
            // Groq was called twice: once for tool selection, once for grounded answer
            verify(groqClient, times(2)).chatWithToolSchema(any(), any(), any());
        }

        @Test
        @DisplayName("Agent loop: unknown tool name returns safe error message and loop continues")
        void agentLoopUnknownToolContinuesLoop() {
            mockSecurityContext(MANAGER_USERNAME, "ROLE_MANAGER");
            when(auditLogRepository.save(any())).thenReturn(null);

            String unknownToolCall = "{\"tool_call\":{\"name\":\"nonexistent_tool\",\"arguments\":{}}}";
            String finalAnswer = "I could not find that information.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(unknownToolCall)
                    .thenReturn(finalAnswer);

            AiAgentService agentService = buildAgentService(getEmployeeWorkloadTool);
            AgentChatRequest request = new AgentChatRequest("Tell me about something unknown.");
            AgentChatResponse response = agentService.chat(request);

            assertThat(response.answer()).isEqualTo(finalAnswer);
        }

        @Test
        @DisplayName("Agent loop: malformed JSON from Groq falls back to plain text answer")
        void agentLoopMalformedJsonTreatedAsAnswer() {
            mockSecurityContext(MANAGER_USERNAME, "ROLE_MANAGER");
            when(auditLogRepository.save(any())).thenReturn(null);

            // Groq returns plain text (no tool_call) — this is the final answer
            String plainAnswer = "I am an AI assistant. How can I help you?";
            when(groqClient.chatWithToolSchema(any(), any(), any())).thenReturn(plainAnswer);

            AiAgentService agentService = buildAgentService(getEmployeeWorkloadTool);
            AgentChatRequest request = new AgentChatRequest("Hello");
            AgentChatResponse response = agentService.chat(request);

            assertThat(response.answer()).isEqualTo(plainAnswer);
            assertThat(response.toolsExecuted()).isEmpty();
        }

        @Test
        @DisplayName("Agent loop: multiple tool calls in sequence accumulate results correctly")
        void agentLoopMultipleToolCalls() {
            mockSecurityContext(MANAGER_USERNAME, "ROLE_MANAGER");
            when(taskService.getWorkloadSummary()).thenReturn(List.of(sampleWorkloadResponse()));
            when(taskService.getEmployeeAvailability()).thenReturn(List.of(sampleAvailabilityResponse()));
            when(auditLogRepository.save(any())).thenReturn(null);

            String workloadCall = "{\"tool_call\":{\"name\":\"get_employee_workload\",\"arguments\":{}}}";
            String availCall = "{\"tool_call\":{\"name\":\"get_employee_availability\",\"arguments\":{}}}";
            String finalAnswer = "Jane Doe is available and has medium workload.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(workloadCall)
                    .thenReturn(availCall)
                    .thenReturn(finalAnswer);

            AiAgentService agentService = buildAgentService(
                    getEmployeeWorkloadTool, getEmployeeAvailabilityTool);

            AgentChatRequest request = new AgentChatRequest(
                    "Who is available and has capacity for a new task?");
            AgentChatResponse response = agentService.chat(request);

            assertThat(response.toolsExecuted())
                    .contains("get_employee_workload", "get_employee_availability");
            assertThat(response.answer()).isEqualTo(finalAnswer);
            verify(taskService).getWorkloadSummary();
            verify(taskService).getEmployeeAvailability();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 6. Tool descriptions — mandatory "Use this tool when" phrasing
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Tool Descriptions Quality")
    class ToolDescriptionTests {

        @Test
        @DisplayName("All tool descriptions contain 'Use this tool' phrasing")
        void allDescriptionsContainUseThisTool() {
            List<AiAgentTool> tools = List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool);

            for (AiAgentTool tool : tools) {
                assertThat(tool.getDescription())
                        .as("Tool '%s' description should contain 'Use this tool'", tool.getName())
                        .containsIgnoringCase("Use this tool");
            }
        }

        @Test
        @DisplayName("All tool descriptions contain 'Never' (anti-hallucination guidance)")
        void allDescriptionsContainNeverGuideance() {
            List<AiAgentTool> tools = List.of(
                    getCurrentEmployeeTool, searchTasksTool, searchEmployeesTool,
                    getTaskTool, getEmployeeWorkloadTool, getEmployeeAttendanceTool,
                    getLeaveRequestsTool, getEmployeeAvailabilityTool);

            for (AiAgentTool tool : tools) {
                assertThat(tool.getDescription())
                        .as("Tool '%s' description should contain 'Never' to prevent hallucination", tool.getName())
                        .containsIgnoringCase("Never");
            }
        }

        @Test
        @DisplayName("get_employee_availability description emphasizes it is the ONLY availability tool")
        void availabilityToolDescriptionIsExclusive() {
            assertThat(getEmployeeAvailabilityTool.getDescription())
                    .containsIgnoringCase("ONLY tool");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // 7. AgentToolContext correctness
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("AgentToolContext Freshness and Correctness")
    class ContextTests {

        @Test
        @DisplayName("Context currentEmployeeId() returns the linked employee's UUID")
        void contextReturnsCorrectEmployeeId() {
            AgentToolContext ctx = employeeContext();
            assertThat(ctx.currentEmployeeId()).isEqualTo(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("Context currentEmployeeId() returns null when no employee is linked")
        void contextReturnsNullWhenNoEmployee() {
            AgentToolContext ctx = new AgentToolContext(USER_ID, USERNAME, Set.of("ROLE_EMPLOYEE"), null);
            assertThat(ctx.currentEmployeeId()).isNull();
        }

        @Test
        @DisplayName("Context hasAnyRole() correctly checks role presence")
        void contextHasAnyRoleCheck() {
            AgentToolContext ctx = managerContext();
            assertThat(ctx.hasAnyRole(Set.of("ROLE_MANAGER"))).isTrue();
            assertThat(ctx.hasAnyRole(Set.of("ROLE_ADMIN"))).isFalse();
            assertThat(ctx.hasAnyRole(Set.of("ROLE_MANAGER", "ROLE_HR"))).isTrue();
        }

        @Test
        @DisplayName("Context username and userId are correctly set")
        void contextUsernameAndUserIdAreCorrect() {
            AgentToolContext ctx = employeeContext();
            assertThat(ctx.username()).isEqualTo(USERNAME);
            assertThat(ctx.userId()).isEqualTo(USER_ID);
        }

        @Test
        @DisplayName("Context primaryRole() returns a role string")
        void contextPrimaryRoleIsNotEmpty() {
            AgentToolContext ctx = employeeContext();
            assertThat(ctx.primaryRole()).isNotBlank();
        }
    }
}
