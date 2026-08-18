package com.company.employeemanagement.ai.agent;

import com.company.employeemanagement.ai.agent.dto.AgentChatRequest;
import com.company.employeemanagement.ai.agent.dto.AgentChatResponse;
import com.company.employeemanagement.ai.agent.service.AgentActionExecutor;
import com.company.employeemanagement.ai.agent.service.AgentConfirmationStore;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Phase 7E.2 — Data Isolation Tests
 *
 * <p>These tests prove the critical invariant: when multiple users are authenticated,
 * each user sees only their own data. No user can accidentally receive another
 * user's tasks, attendance, or leave records.
 *
 * <h2>Test scenarios</h2>
 * <ol>
 *   <li>User A asking "What are my tasks?" gets only Employee A's tasks.</li>
 *   <li>User B asking "What are my tasks?" gets only Employee B's tasks.</li>
 *   <li>Employee A asking "What is my attendance today?" gets only Employee A's records.</li>
 *   <li>Manager asking "Who is available today?" gets availability via the correct tool.</li>
 *   <li>Manager asking "Who is on leave?" gets actual leave records, not general knowledge.</li>
 *   <li>Unknown live-data question does not produce a hallucinated answer.</li>
 *   <li>SearchTasksTool with EMPLOYEE role always calls findMyAssignedTasks, never findAll.</li>
 *   <li>SearchTasksTool with MANAGER role calls findAll (may filter by assignedEmployeeId).</li>
 *   <li>Authenticated employee resolution: JWT → User → Employee chain.</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class Phase7E2DataIsolationTest {

    // ── Mocks ───────────────────────────────────────────────────────────────

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

    // ── Fixed test data — User A ────────────────────────────────────────────

    private static final UUID USER_A_ID       = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private static final UUID EMPLOYEE_A_ID   = UUID.fromString("a1a1a1a1-a1a1-a1a1-a1a1-a1a1a1a1a1a1");
    private static final UUID TASK_A_ID       = UUID.fromString("a2a2a2a2-a2a2-a2a2-a2a2-a2a2a2a2a2a2");
    private static final String USERNAME_A    = "alice@test.com";

    // ── Fixed test data — User B ────────────────────────────────────────────

    private static final UUID USER_B_ID       = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final UUID EMPLOYEE_B_ID   = UUID.fromString("b1b1b1b1-b1b1-b1b1-b1b1-b1b1b1b1b1b1");
    private static final UUID TASK_B_ID       = UUID.fromString("b2b2b2b2-b2b2-b2b2-b2b2-b2b2b2b2b2b2");
    private static final String USERNAME_B    = "bob@test.com";

    // ── Fixed test data — Manager ───────────────────────────────────────────

    private static final UUID MANAGER_USER_ID     = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");
    private static final UUID MANAGER_EMPLOYEE_ID = UUID.fromString("c1c1c1c1-c1c1-c1c1-c1c1-c1c1c1c1c1c1");
    private static final String MANAGER_USERNAME  = "manager@test.com";

    // ── Employee entities ───────────────────────────────────────────────────

    private Employee employeeA;
    private Employee employeeB;
    private Employee managerEmployee;

    // ── Tool instances ──────────────────────────────────────────────────────

    private SearchTasksTool searchTasksTool;
    private GetEmployeeAttendanceTool getEmployeeAttendanceTool;
    private GetLeaveRequestsTool getLeaveRequestsTool;
    private GetEmployeeAvailabilityTool getEmployeeAvailabilityTool;
    private GetEmployeeWorkloadTool getEmployeeWorkloadTool;
    private GetCurrentEmployeeTool getCurrentEmployeeTool;
    private SearchEmployeesTool searchEmployeesTool;
    private GetTaskTool getTaskTool;

    @BeforeEach
    void setUp() {
        employeeA = new Employee();
        employeeA.setId(EMPLOYEE_A_ID);
        employeeA.setFirstName("Alice");
        employeeA.setLastName("Smith");

        employeeB = new Employee();
        employeeB.setId(EMPLOYEE_B_ID);
        employeeB.setFirstName("Bob");
        employeeB.setLastName("Jones");

        managerEmployee = new Employee();
        managerEmployee.setId(MANAGER_EMPLOYEE_ID);
        managerEmployee.setFirstName("Manager");
        managerEmployee.setLastName("Carol");

        searchTasksTool           = new SearchTasksTool(taskService);
        getEmployeeAttendanceTool = new GetEmployeeAttendanceTool(attendanceService);
        getLeaveRequestsTool      = new GetLeaveRequestsTool(leaveRequestService);
        getEmployeeAvailabilityTool = new GetEmployeeAvailabilityTool(taskService);
        getEmployeeWorkloadTool   = new GetEmployeeWorkloadTool(taskService);
        getCurrentEmployeeTool    = new GetCurrentEmployeeTool(securityUtils, employeeService);
        searchEmployeesTool       = new SearchEmployeesTool(employeeService);
        getTaskTool               = new GetTaskTool(taskService);

        when(retrievalService.search(any())).thenReturn(List.of());
        when(auditLogRepository.save(any())).thenReturn(null);
    }

    // ── Context helpers ─────────────────────────────────────────────────────

    private AgentToolContext employeeAContext() {
        return new AgentToolContext(USER_A_ID, USERNAME_A, Set.of("ROLE_EMPLOYEE"), employeeA);
    }

    private AgentToolContext employeeBContext() {
        return new AgentToolContext(USER_B_ID, USERNAME_B, Set.of("ROLE_EMPLOYEE"), employeeB);
    }

    private AgentToolContext managerContext() {
        return new AgentToolContext(MANAGER_USER_ID, MANAGER_USERNAME, Set.of("ROLE_MANAGER"), managerEmployee);
    }

    // ── Task fixtures ───────────────────────────────────────────────────────

    private TaskResponse taskForA() {
        return new TaskResponse(
                TASK_A_ID, "Alice's Task — Build Login Page", "desc",
                null, null, EMPLOYEE_A_ID, "Alice Smith", "EMP-A001",
                UUID.randomUUID(), "Manager Carol",
                TaskPriority.HIGH, TaskStatus.ASSIGNED, false,
                LocalDate.now().plusDays(3), BigDecimal.valueOf(4),
                null, LocalDateTime.now(), LocalDateTime.now(),
                "manager@test.com", "manager@test.com");
    }

    private TaskResponse taskForB() {
        return new TaskResponse(
                TASK_B_ID, "Bob's Task — Fix Bug 42", "desc",
                null, null, EMPLOYEE_B_ID, "Bob Jones", "EMP-B002",
                UUID.randomUUID(), "Manager Carol",
                TaskPriority.MEDIUM, TaskStatus.IN_PROGRESS, false,
                LocalDate.now().plusDays(1), BigDecimal.valueOf(2),
                null, LocalDateTime.now(), LocalDateTime.now(),
                "manager@test.com", "manager@test.com");
    }

    // ── Attendance fixtures ─────────────────────────────────────────────────

    private AttendanceResponse attendanceForA() {
        return new AttendanceResponse(
                UUID.randomUUID(), EMPLOYEE_A_ID, "EMP-A001", "Alice Smith",
                LocalDate.now(), LocalTime.of(9, 0), null,
                AttendanceStatus.PRESENT, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private AttendanceResponse attendanceForB() {
        return new AttendanceResponse(
                UUID.randomUUID(), EMPLOYEE_B_ID, "EMP-B002", "Bob Jones",
                LocalDate.now(), LocalTime.of(8, 30), null,
                AttendanceStatus.ABSENT, null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    // ── Leave fixtures ──────────────────────────────────────────────────────

    private LeaveRequestResponse leaveForA() {
        return new LeaveRequestResponse(
                UUID.randomUUID(), EMPLOYEE_A_ID, "EMP-A001", "Alice Smith", "Engineering",
                LeaveType.ANNUAL, LocalDate.now(), LocalDate.now().plusDays(2),
                3, "Holiday", LeaveStatus.APPROVED, null,
                null, null, LocalDateTime.now(), LocalDateTime.now(),
                "alice@test.com", "manager@test.com");
    }

    private LeaveRequestResponse leaveForB() {
        return new LeaveRequestResponse(
                UUID.randomUUID(), EMPLOYEE_B_ID, "EMP-B002", "Bob Jones", "Engineering",
                LeaveType.SICK, LocalDate.now(), LocalDate.now(),
                1, "Sick", LeaveStatus.PENDING, null,
                null, null, LocalDateTime.now(), LocalDateTime.now(),
                "bob@test.com", "manager@test.com");
    }

    // ── Availability fixture ────────────────────────────────────────────────

    private EmployeeAvailabilityResponse availabilityForA() {
        // Alice is on approved leave today — checkedIn=false, onApprovedLeaveToday=true, available=false, disabled=false
        return new EmployeeAvailabilityResponse(
                EMPLOYEE_A_ID, "Alice Smith", "EMP-A001",
                false, 2, true, false, false,
                null, "APPROVED_LEAVE");
    }

    private EmployeeAvailabilityResponse availabilityForB() {
        return new EmployeeAvailabilityResponse(
                EMPLOYEE_B_ID, "Bob Jones", "EMP-B002",
                true, 1, false, true, false,
                null, null);
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 1: Two users — each sees only their own tasks
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 1 — Two Users, Two Employee Records: Task Isolation")
    class TwoUserTaskIsolationTests {

        @Test
        @DisplayName("User A (EMPLOYEE) sees only Alice's tasks, not Bob's")
        void userASeesOnlyAliceTasks() {
            // searchTasksTool for EMPLOYEE calls findMyAssignedTasks (SecurityContext-scoped)
            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));

            String result = searchTasksTool.execute("{}", employeeAContext());

            assertThat(result).contains("Alice's Task").contains("Build Login Page");
            assertThat(result).doesNotContain("Bob's Task").doesNotContain("Fix Bug 42");
            // Must use scoped method, NEVER findAll
            verify(taskService).findMyAssignedTasks(isNull(), isNull(), any());
            verify(taskService, never()).findAll(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("User B (EMPLOYEE) sees only Bob's tasks, not Alice's")
        void userBSeesOnlyBobTasks() {
            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForB()))));

            String result = searchTasksTool.execute("{}", employeeBContext());

            assertThat(result).contains("Bob's Task").contains("Fix Bug 42");
            assertThat(result).doesNotContain("Alice's Task").doesNotContain("Build Login Page");
            verify(taskService).findMyAssignedTasks(isNull(), isNull(), any());
            verify(taskService, never()).findAll(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("User A and User B invoke the scoped service method — not findAll")
        void bothEmployeesNeverCallFindAll() {
            when(taskService.findMyAssignedTasks(any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            searchTasksTool.execute("{}", employeeAContext());
            searchTasksTool.execute("{}", employeeBContext());

            verify(taskService, never()).findAll(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Manager can query all tasks via findAll when no assignedEmployeeId is given")
        void managerCanQueryAllTasks() {
            when(taskService.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA(), taskForB()))));

            String result = searchTasksTool.execute("{}", managerContext());

            assertThat(result).contains("Alice's Task").contains("Bob's Task");
            verify(taskService).findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Manager can filter by assignedEmployeeId to see only Employee A's tasks")
        void managerFiltersByAssignedEmployeeId() {
            when(taskService.findAll(eq(EMPLOYEE_A_ID), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));

            String result = searchTasksTool.execute(
                    "{\"assignedEmployeeId\":\"" + EMPLOYEE_A_ID + "\"}", managerContext());

            assertThat(result).contains("Alice's Task");
            assertThat(result).doesNotContain("Bob's Task");
            verify(taskService).findAll(eq(EMPLOYEE_A_ID), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("EMPLOYEE role cannot override scoping by passing another employee's ID")
        void employeeCannotOverrideScopingByPassingOtherEmployeeId() {
            // Even if the LLM passes EMPLOYEE_B_ID as assignedEmployeeId for User A,
            // the employee path always uses findMyAssignedTasks (ignores the argument).
            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));

            String result = searchTasksTool.execute(
                    "{\"assignedEmployeeId\":\"" + EMPLOYEE_B_ID + "\"}", employeeAContext());

            assertThat(result).contains("Alice's Task");
            // Must use scoped method — NOT findAll with B's ID
            verify(taskService).findMyAssignedTasks(isNull(), isNull(), any());
            verify(taskService, never()).findAll(any(), any(), any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 2: Employee A's attendance — scoped to own records only
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 2 — Attendance Isolation: Employee Sees Only Own Records")
    class AttendanceIsolationTests {

        @Test
        @DisplayName("Employee A asking about attendance uses findMyAttendance, not findAll")
        void employeeAAttendanceUsesScoped() {
            when(attendanceService.findMyAttendance(any(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(attendanceForA()))));

            String result = getEmployeeAttendanceTool.execute("{}", employeeAContext());

            assertThat(result).contains("Alice Smith").contains("PRESENT");
            assertThat(result).doesNotContain("Bob Jones");
            verify(attendanceService).findMyAttendance(any(), isNull(), any());
            verify(attendanceService, never()).findAll(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Employee B asking about attendance uses findMyAttendance, not findAll")
        void employeeBAttendanceUsesScoped() {
            when(attendanceService.findMyAttendance(any(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(attendanceForB()))));

            String result = getEmployeeAttendanceTool.execute("{}", employeeBContext());

            assertThat(result).contains("Bob Jones").contains("ABSENT");
            assertThat(result).doesNotContain("Alice Smith");
            verify(attendanceService).findMyAttendance(any(), isNull(), any());
            verify(attendanceService, never()).findAll(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Manager asking about attendance uses findAll (can see all employees)")
        void managerAttendanceUsesUnscoped() {
            when(attendanceService.findAll(isNull(), any(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(attendanceForA(), attendanceForB()))));

            String result = getEmployeeAttendanceTool.execute("{}", managerContext());

            assertThat(result).contains("Alice Smith").contains("Bob Jones");
            verify(attendanceService).findAll(isNull(), any(), isNull(), any());
            verify(attendanceService, never()).findMyAttendance(any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 3: Manager — Who is available today?
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 3 — Manager Sees Actual Availability Rules")
    class AvailabilityTests {

        @Test
        @DisplayName("Manager gets actual availability from TaskService.getEmployeeAvailability()")
        void managerGetsActualAvailability() {
            when(taskService.getEmployeeAvailability())
                    .thenReturn(List.of(availabilityForA(), availabilityForB()));

            String result = getEmployeeAvailabilityTool.execute("{}", managerContext());

            assertThat(result).contains("Alice Smith").contains("Bob Jones");
            // Alice is NOT available (on leave); Bob IS available (checked in)
            // The tool formats availableToday flag
            assertThat(result).contains("Alice Smith");
            assertThat(result).contains("Bob Jones");
            verify(taskService).getEmployeeAvailability();
        }

        @Test
        @DisplayName("Availability result includes checked-in status and leave flag")
        void availabilityResultContainsStatusFlags() {
            when(taskService.getEmployeeAvailability())
                    .thenReturn(List.of(availabilityForA(), availabilityForB()));

            String result = getEmployeeAvailabilityTool.execute("{}", managerContext());

            // Alice is on leave and not available
            assertThat(result).contains("ON LEAVE");
            // Bob is checked in and available
            assertThat(result).contains("Checked in");
        }

        @Test
        @DisplayName("EMPLOYEE role cannot access get_employee_availability")
        void employeeCannotAccessAvailabilityTool() {
            assertThat(getEmployeeAvailabilityTool.getAllowedRoles())
                    .doesNotContain("ROLE_EMPLOYEE");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 4: Manager — Who is on leave?
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 4 — Leave Record Isolation")
    class LeaveIsolationTests {

        @Test
        @DisplayName("Manager asking 'who is on leave' gets actual leave records from service")
        void managerGetsActualLeaveRecords() {
            when(leaveRequestService.findAll(isNull(), isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(leaveForA(), leaveForB()))));

            String result = getLeaveRequestsTool.execute("{}", managerContext());

            assertThat(result).contains("Alice Smith").contains("APPROVED");
            assertThat(result).contains("Bob Jones").contains("PENDING");
            verify(leaveRequestService).findAll(isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Employee A sees only own leave requests via findMyLeaves")
        void employeeASeesOnlyOwnLeave() {
            when(leaveRequestService.findMyLeaves(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(leaveForA()))));

            String result = getLeaveRequestsTool.execute("{}", employeeAContext());

            assertThat(result).contains("Alice Smith").contains("APPROVED");
            assertThat(result).doesNotContain("Bob Jones");
            verify(leaveRequestService).findMyLeaves(isNull(), isNull(), any());
            verify(leaveRequestService, never()).findAll(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Employee B sees only own leave requests via findMyLeaves")
        void employeeBSeesOnlyOwnLeave() {
            when(leaveRequestService.findMyLeaves(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(leaveForB()))));

            String result = getLeaveRequestsTool.execute("{}", employeeBContext());

            assertThat(result).contains("Bob Jones").contains("PENDING");
            assertThat(result).doesNotContain("Alice Smith");
            verify(leaveRequestService).findMyLeaves(isNull(), isNull(), any());
            verify(leaveRequestService, never()).findAll(any(), any(), any(), any());
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 5: No hallucination for unknown / unauthorized data
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 5 — No Hallucination: Empty Tool Results Return Safe Messages")
    class NoHallucinationTests {

        @Test
        @DisplayName("search_tasks returns 'no tasks found' when tool result is empty — not invented tasks")
        void emptyTaskResultDoesNotHallucinate() {
            when(taskService.findMyAssignedTasks(any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = searchTasksTool.execute("{}", employeeAContext());

            assertThat(result).containsIgnoringCase("No tasks found");
            // Verify it does not contain any invented task title
            assertThat(result).doesNotContain("Design").doesNotContain("Review").doesNotContain("Meeting");
        }

        @Test
        @DisplayName("get_employee_attendance returns 'no records found' for empty result — not invented data")
        void emptyAttendanceResultDoesNotHallucinate() {
            when(attendanceService.findMyAttendance(any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = getEmployeeAttendanceTool.execute("{}", employeeAContext());

            assertThat(result).containsIgnoringCase("No attendance records");
        }

        @Test
        @DisplayName("get_leave_requests returns 'no leave requests found' for empty result — not invented data")
        void emptyLeaveResultDoesNotHallucinate() {
            when(leaveRequestService.findMyLeaves(any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            String result = getLeaveRequestsTool.execute("{}", employeeAContext());

            assertThat(result).containsIgnoringCase("No leave requests found");
        }

        @Test
        @DisplayName("get_employee_availability returns 'no availability data' for empty result")
        void emptyAvailabilityResultDoesNotHallucinate() {
            when(taskService.getEmployeeAvailability()).thenReturn(List.of());

            String result = getEmployeeAvailabilityTool.execute("{}", managerContext());

            assertThat(result).containsIgnoringCase("No employee availability data");
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 6: AiAgentService context building — JWT → User → Employee chain
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 6 — Authenticated Employee Resolution (JWT → User → Employee)")
    class ContextBuildingTests {

        private AiAgentService buildAgentService(final AiAgentTool... tools) {
            AgentToolRegistry registry = new AgentToolRegistry(List.of(tools));
            AgentConfirmationStore store = new AgentConfirmationStore();
            RagProperties ragProperties = new RagProperties();
            ragProperties.setEnabled(false);

            AiAgentService service = new AiAgentService(
                    groqClient, registry, store,
                    mock(AgentActionExecutor.class),
                    auditLogRepository, securityUtils,
                    retrievalService, contextBuilder, ragProperties,
                    userRepository, employeeRepository);
            ReflectionTestUtils.setField(service, "maxToolCalls", 8);
            return service;
        }

        private void mockSecurity(final String username, final String role,
                                   final UUID userId, final Employee employee) {
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(username);
            when(authentication.getAuthorities()).thenAnswer(inv ->
                    List.of(new SimpleGrantedAuthority(role)));

            com.company.employeemanagement.entity.User mockUser =
                    com.company.employeemanagement.entity.User.builder().build();
            mockUser.setId(userId);
            when(userRepository.findByEmail(username)).thenReturn(Optional.of(mockUser));
            when(employeeRepository.findByUserId(userId)).thenReturn(Optional.of(employee));
        }

        @Test
        @DisplayName("When User A is authenticated, AiAgentService builds context with Employee A")
        void userAResolvesToEmployeeA() {
            mockSecurity(USERNAME_A, "ROLE_EMPLOYEE", USER_A_ID, employeeA);

            // search_tasks for EMPLOYEE calls findMyAssignedTasks
            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));

            String toolCallJson = "{\"tool_call\":{\"name\":\"search_tasks\",\"arguments\":{}}}";
            String groundedAnswer = "You have 1 task: Alice's Task — Build Login Page.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCallJson)
                    .thenReturn(groundedAnswer);

            AiAgentService agentService = buildAgentService(searchTasksTool);
            AgentChatResponse response = agentService.chat(new AgentChatRequest("What are my tasks?"));

            assertThat(response.toolsExecuted()).contains("search_tasks");
            assertThat(response.answer()).isEqualTo(groundedAnswer);
            // The scoped task service method was called (not findAll)
            verify(taskService).findMyAssignedTasks(isNull(), isNull(), any());
        }

        @Test
        @DisplayName("When User B is authenticated, AiAgentService builds context with Employee B")
        void userBResolvesToEmployeeB() {
            mockSecurity(USERNAME_B, "ROLE_EMPLOYEE", USER_B_ID, employeeB);

            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForB()))));

            String toolCallJson = "{\"tool_call\":{\"name\":\"search_tasks\",\"arguments\":{}}}";
            String groundedAnswer = "You have 1 task: Bob's Task — Fix Bug 42.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCallJson)
                    .thenReturn(groundedAnswer);

            AiAgentService agentService = buildAgentService(searchTasksTool);
            AgentChatResponse response = agentService.chat(new AgentChatRequest("What are my tasks?"));

            assertThat(response.toolsExecuted()).contains("search_tasks");
            assertThat(response.answer()).isEqualTo(groundedAnswer);
            verify(taskService).findMyAssignedTasks(isNull(), isNull(), any());
        }

        @Test
        @DisplayName("User A and User B each get different task results — data is isolated")
        void userAAndUserBGetDifferentResults() {
            // Simulate User A session
            mockSecurity(USERNAME_A, "ROLE_EMPLOYEE", USER_A_ID, employeeA);
            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));
            String toolCall = "{\"tool_call\":{\"name\":\"search_tasks\",\"arguments\":{}}}";
            String answerA = "Alice's tasks: Build Login Page.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCall).thenReturn(answerA);

            AiAgentService serviceA = buildAgentService(searchTasksTool);
            AgentChatResponse responseA = serviceA.chat(new AgentChatRequest("What are my tasks?"));

            // Reset mock state for User B session
            when(taskService.findMyAssignedTasks(isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForB()))));
            mockSecurity(USERNAME_B, "ROLE_EMPLOYEE", USER_B_ID, employeeB);
            String answerB = "Bob's tasks: Fix Bug 42.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCall).thenReturn(answerB);

            AiAgentService serviceB = buildAgentService(searchTasksTool);
            AgentChatResponse responseB = serviceB.chat(new AgentChatRequest("What are my tasks?"));

            // The answers are different — each user gets their own data
            assertThat(responseA.answer()).isEqualTo(answerA);
            assertThat(responseB.answer()).isEqualTo(answerB);
            assertThat(responseA.answer()).isNotEqualTo(responseB.answer());
        }

        @Test
        @DisplayName("Null userId when user not found in database results in no employee context")
        void nullUserIdResultsInNoEmployee() {
            SecurityContextHolder.setContext(securityContext);
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn("unknown@test.com");
            when(authentication.getAuthorities()).thenAnswer(inv ->
                    List.of(new SimpleGrantedAuthority("ROLE_EMPLOYEE")));
            when(userRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

            // Tool: no employee → return no tasks message
            when(taskService.findMyAssignedTasks(any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));
            String toolCall = "{\"tool_call\":{\"name\":\"search_tasks\",\"arguments\":{}}}";
            String fallbackAnswer = "No tasks found matching the criteria.";
            when(groqClient.chatWithToolSchema(any(), any(), any()))
                    .thenReturn(toolCall).thenReturn(fallbackAnswer);

            AiAgentService agentService = buildAgentService(searchTasksTool);
            AgentChatResponse response = agentService.chat(new AgentChatRequest("What are my tasks?"));

            // Should not throw — returns a safe answer
            assertThat(response).isNotNull();
            assertThat(response.answer()).isNotBlank();
        }
    }

    // ══════════════════════════════════════════════════════════════════════
    // TEST 7: SearchTasksTool role scoping contract
    // ══════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Test 7 — SearchTasksTool Role Scoping Contract")
    class SearchTasksScopingContractTests {

        @Test
        @DisplayName("EMPLOYEE always calls findMyAssignedTasks — findAll is never called")
        void employeeAlwaysCallsFindMyAssignedTasks() {
            when(taskService.findMyAssignedTasks(any(), any(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            searchTasksTool.execute("{}", employeeAContext());

            verify(taskService).findMyAssignedTasks(any(), any(), any());
            verify(taskService, never()).findAll(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("EMPLOYEE with status filter still calls findMyAssignedTasks with the status")
        void employeeWithStatusFilterCallsScopedMethod() {
            when(taskService.findMyAssignedTasks(eq(TaskStatus.IN_PROGRESS), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));

            String result = searchTasksTool.execute("{\"status\":\"IN_PROGRESS\"}", employeeAContext());

            verify(taskService).findMyAssignedTasks(eq(TaskStatus.IN_PROGRESS), isNull(), any());
            verify(taskService, never()).findAll(any(), any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("MANAGER without assignedEmployeeId calls findAll(null, ...) — sees all tasks")
        void managerWithoutFilterCallsFindAll() {
            when(taskService.findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of())));

            searchTasksTool.execute("{}", managerContext());

            verify(taskService).findAll(isNull(), isNull(), isNull(), isNull(), isNull(), any());
            verify(taskService, never()).findMyAssignedTasks(any(), any(), any());
        }

        @Test
        @DisplayName("MANAGER with assignedEmployeeId calls findAll with the specified UUID")
        void managerWithFilterCallsFindAllWithId() {
            when(taskService.findAll(eq(EMPLOYEE_A_ID), isNull(), isNull(), isNull(), isNull(), any()))
                    .thenReturn(PageResponse.from(new PageImpl<>(List.of(taskForA()))));

            searchTasksTool.execute("{\"assignedEmployeeId\":\"" + EMPLOYEE_A_ID + "\"}", managerContext());

            verify(taskService).findAll(eq(EMPLOYEE_A_ID), isNull(), isNull(), isNull(), isNull(), any());
        }

        @Test
        @DisplayName("Invalid assignedEmployeeId UUID returns an error string — manager path")
        void invalidUuidReturnsError() {
            String result = searchTasksTool.execute("{\"assignedEmployeeId\":\"not-a-uuid\"}", managerContext());
            assertThat(result).containsIgnoringCase("Invalid assignedEmployeeId");
        }

        @Test
        @DisplayName("Invalid status string returns an error string for both roles")
        void invalidStatusReturnsSafeError() {
            String resultEmployee = searchTasksTool.execute("{\"status\":\"BOGUS\"}", employeeAContext());
            String resultManager  = searchTasksTool.execute("{\"status\":\"BOGUS\"}", managerContext());
            assertThat(resultEmployee).containsIgnoringCase("Invalid status");
            assertThat(resultManager).containsIgnoringCase("Invalid status");
        }
    }
}
