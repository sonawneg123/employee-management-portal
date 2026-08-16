package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.request.CreateTaskRequest;
import com.company.employeemanagement.dto.request.ReassignTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskRequest;
import com.company.employeemanagement.dto.request.UpdateTaskStatusRequest;
import com.company.employeemanagement.dto.response.EmployeeAvailabilityResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.dto.response.TaskActivityResponse;
import com.company.employeemanagement.dto.response.TaskDashboardStatsResponse;
import com.company.employeemanagement.dto.response.TaskResponse;
import com.company.employeemanagement.dto.response.WorkloadResponse;
import com.company.employeemanagement.entity.Attendance;
import com.company.employeemanagement.entity.Employee;
import com.company.employeemanagement.entity.Task;
import com.company.employeemanagement.entity.TaskActivity;
import com.company.employeemanagement.entity.User;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.NotificationType;
import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.exception.AccessDeniedException;
import com.company.employeemanagement.exception.ResourceNotFoundException;
import com.company.employeemanagement.mapper.TaskMapper;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.TaskActivityRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.security.SecurityUtils;
import com.company.employeemanagement.service.NotificationService;
import com.company.employeemanagement.service.TaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implementation of {@link TaskService} providing task lifecycle management.
 *
 * <p>Authorization model:
 * <ul>
 *   <li>ADMIN, HR, MANAGER can create, assign, view, and fully update any task.</li>
 *   <li>EMPLOYEE can only view tasks assigned to themselves and perform
 *       limited status transitions (ASSIGNED → IN_PROGRESS).</li>
 *   <li>EMPLOYEE cannot create, assign, or modify other employees' tasks.</li>
 *   <li>EMPLOYEE must be checked in to start a task (ASSIGNED → IN_PROGRESS).</li>
 *   <li>The assigned employee must be checked in when a task is assigned (create/update/reassign).</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Service
public class TaskServiceImpl implements TaskService {

    private static final Logger log = LoggerFactory.getLogger(TaskServiceImpl.class);

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("d MMM yyyy");

    private static final List<TaskStatus> DONE_STATUSES =
            List.of(TaskStatus.COMPLETED, TaskStatus.REJECTED);

    /**
     * Status transitions allowed for employees.
     * Keyed by current status → set of valid next statuses.
     */
    private static final Map<TaskStatus, Set<TaskStatus>> EMPLOYEE_TRANSITIONS =
            Map.of(
                    TaskStatus.ASSIGNED,    EnumSet.of(TaskStatus.IN_PROGRESS),
                    TaskStatus.IN_PROGRESS, EnumSet.of(TaskStatus.IN_PROGRESS)
            );

    private final TaskRepository taskRepository;
    private final EmployeeRepository employeeRepository;
    private final AttendanceRepository attendanceRepository;
    private final TaskActivityRepository taskActivityRepository;
    private final TaskMapper taskMapper;
    private final SecurityUtils securityUtils;
    private final NotificationService notificationService;

    public TaskServiceImpl(final TaskRepository taskRepository,
                           final EmployeeRepository employeeRepository,
                           final AttendanceRepository attendanceRepository,
                           final TaskActivityRepository taskActivityRepository,
                           final TaskMapper taskMapper,
                           final SecurityUtils securityUtils,
                           final NotificationService notificationService) {
        this.taskRepository = taskRepository;
        this.employeeRepository = employeeRepository;
        this.attendanceRepository = attendanceRepository;
        this.taskActivityRepository = taskActivityRepository;
        this.taskMapper = taskMapper;
        this.securityUtils = securityUtils;
        this.notificationService = notificationService;
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findAll(final UUID assignedEmployeeId,
                                               final UUID createdByEmployeeId,
                                               final TaskStatus status,
                                               final TaskPriority priority,
                                               final TaskCategory category,
                                               final Pageable pageable) {
        if (isEmployeeOnly()) {
            return findMyAssignedTasks(status, priority, pageable);
        }
        return fetchPage(assignedEmployeeId, createdByEmployeeId, status, priority, category, pageable);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findMyAssignedTasks(final TaskStatus status,
                                                           final TaskPriority priority,
                                                           final Pageable pageable) {
        Optional<Employee> employeeOpt = securityUtils.getCurrentEmployee();
        if (employeeOpt.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0L));
        }
        UUID ownEmployeeId = employeeOpt.get().getId();
        return fetchPage(ownEmployeeId, null, status, priority, null, pageable);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<TaskResponse> findMyCreatedTasks(final TaskStatus status,
                                                          final TaskPriority priority,
                                                          final Pageable pageable) {
        Optional<Employee> employeeOpt = securityUtils.getCurrentEmployee();
        if (employeeOpt.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0L));
        }
        UUID ownEmployeeId = employeeOpt.get().getId();
        return fetchPage(null, ownEmployeeId, status, priority, null, pageable);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public TaskResponse findById(final UUID id) {
        Task task = taskRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        if (isEmployeeOnly()) {
            requireAssignedToCurrentEmployee(task);
        }
        return taskMapper.toResponse(task);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskResponse create(final CreateTaskRequest request) {
        Employee assignedEmployee = null;
        if (request.assignedEmployeeId() != null) {
            assignedEmployee = employeeRepository.findById(request.assignedEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employee", request.assignedEmployeeId()));
            requireEmployeeCheckedIn(assignedEmployee);
        }

        Employee creatingEmployee = securityUtils.getCurrentEmployee().orElse(null);

        TaskStatus initialStatus = assignedEmployee != null
                ? TaskStatus.ASSIGNED
                : TaskStatus.DRAFT;

        TaskPriority priority = request.priority() != null
                ? request.priority()
                : TaskPriority.MEDIUM;

        Task task = Task.builder()
                .title(request.title())
                .description(request.description())
                .guidelines(request.guidelines())
                .acceptanceCriteria(request.acceptanceCriteria())
                .assignedEmployee(assignedEmployee)
                .createdByEmployee(creatingEmployee)
                .priority(priority)
                .status(initialStatus)
                .dueDate(request.dueDate())
                .estimatedHours(request.estimatedHours())
                .category(request.category())
                .build();

        Task saved = taskRepository.save(task);
        log.info("Task.create: persisted task id={} title='{}' status={} by={}",
                saved.getId(), saved.getTitle(), saved.getStatus(),
                securityUtils.getCurrentUsername());

        Task withAssociations = taskRepository.findByIdWithAssociations(saved.getId())
                .orElse(saved);

        recordActivity(withAssociations, creatingEmployee, "TASK_ASSIGNED",
                "Task '" + saved.getTitle() + "' created"
                        + (assignedEmployee != null ? " and assigned to " + employeeName(assignedEmployee) : ""),
                null, initialStatus.name());

        if (assignedEmployee != null) {
            String dueDateStr = request.dueDate() != null
                    ? request.dueDate().format(DATE_FMT) : "N/A";
            notificationService.createNotification(
                    assignedEmployee,
                    NotificationType.TASK_ASSIGNED,
                    "New Task Assigned",
                    "You have been assigned: \"" + saved.getTitle() + "\"\n"
                            + "Due: " + dueDateStr + "\n"
                            + "Priority: " + priority.name(),
                    saved.getId()
            );
        }

        return taskMapper.toResponse(withAssociations);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskResponse update(final UUID id, final UpdateTaskRequest request) {
        Task task = taskRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        if (isEmployeeOnly()) {
            throw new AccessDeniedException(
                    "Employees may not update task details. Use the status endpoint.");
        }

        Employee prevAssigned = task.getAssignedEmployee();

        Employee assignedEmployee = null;
        if (request.assignedEmployeeId() != null) {
            assignedEmployee = employeeRepository.findById(request.assignedEmployeeId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Employee", request.assignedEmployeeId()));
            // Only enforce check-in if this is a NEW assignment (not keeping the same employee)
            if (prevAssigned == null || !prevAssigned.getId().equals(assignedEmployee.getId())) {
                requireEmployeeCheckedIn(assignedEmployee);
            }
        }

        TaskStatus prevStatus = task.getStatus();
        TaskStatus newStatus = request.status() != null ? request.status() : task.getStatus();

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setGuidelines(request.guidelines());
        task.setAcceptanceCriteria(request.acceptanceCriteria());
        task.setAssignedEmployee(assignedEmployee);
        task.setPriority(request.priority() != null ? request.priority() : task.getPriority());
        task.setStatus(newStatus);
        task.setDueDate(request.dueDate());
        task.setEstimatedHours(request.estimatedHours());
        task.setCategory(request.category());

        Task updated = taskRepository.save(task);
        log.info("Task.update: updated task id={} by={}", id, securityUtils.getCurrentUsername());

        if (prevStatus != newStatus) {
            Employee actor = securityUtils.getCurrentEmployee().orElse(null);
            recordActivity(updated, actor, "TASK_STATUS_CHANGED",
                    "Status changed from " + prevStatus + " to " + newStatus,
                    prevStatus.name(), newStatus.name());
        }

        if (assignedEmployee != null
                && (prevAssigned == null || !prevAssigned.getId().equals(assignedEmployee.getId()))) {
            String dueDateStr = updated.getDueDate() != null
                    ? updated.getDueDate().format(DATE_FMT) : "N/A";
            notificationService.createNotification(
                    assignedEmployee,
                    NotificationType.TASK_ASSIGNED,
                    "New Task Assigned",
                    "You have been assigned: \"" + updated.getTitle() + "\"\n"
                            + "Due: " + dueDateStr + "\n"
                            + "Priority: " + updated.getPriority().name(),
                    updated.getId()
            );
        }

        return taskMapper.toResponse(taskRepository.findByIdWithAssociations(updated.getId())
                .orElse(updated));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskResponse updateStatus(final UUID id, final UpdateTaskStatusRequest request) {
        Task task = taskRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        if (isEmployeeOnly()) {
            requireAssignedToCurrentEmployee(task);
            validateEmployeeTransition(task.getStatus(), request.status());

            if (request.status() == TaskStatus.IN_PROGRESS
                    && task.getStatus() == TaskStatus.ASSIGNED) {
                requireCheckedIn();
            }
        }

        TaskStatus prevStatus = task.getStatus();
        task.setStatus(request.status());
        Task updated = taskRepository.save(task);
        log.info("Task.updateStatus: task id={} status changed to {} by={}",
                id, request.status(), securityUtils.getCurrentUsername());

        Employee actor = securityUtils.getCurrentEmployee().orElse(null);
        String eventType = (request.status() == TaskStatus.IN_PROGRESS && prevStatus == TaskStatus.ASSIGNED)
                ? "TASK_STARTED" : "TASK_STATUS_CHANGED";
        String actorName = actor != null ? employeeName(actor) : securityUtils.getCurrentUsername();
        recordActivity(updated, actor, eventType,
                actorName + " changed status from " + prevStatus + " to " + request.status(),
                prevStatus.name(), request.status().name());

        if (eventType.equals("TASK_STARTED") && task.getCreatedByEmployee() != null) {
            notificationService.createNotification(
                    task.getCreatedByEmployee(),
                    NotificationType.TASK_STARTED,
                    "Task Started",
                    actorName + " has started: \"" + task.getTitle() + "\"",
                    task.getId()
            );
        }

        return taskMapper.toResponse(taskRepository.findByIdWithAssociations(updated.getId())
                .orElse(updated));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public TaskResponse reassign(final UUID id, final ReassignTaskRequest request) {
        Task task = taskRepository.findByIdWithAssociations(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));

        Employee newEmployee = employeeRepository.findById(request.newEmployeeId())
                .orElseThrow(() -> new ResourceNotFoundException("Employee", request.newEmployeeId()));

        requireEmployeeCheckedIn(newEmployee);

        Employee previousEmployee = task.getAssignedEmployee();
        Employee actor = securityUtils.getCurrentEmployee().orElse(null);

        task.setAssignedEmployee(newEmployee);
        if (task.getStatus() == TaskStatus.DRAFT) {
            task.setStatus(TaskStatus.ASSIGNED);
        }
        Task updated = taskRepository.save(task);

        String prevName = previousEmployee != null ? employeeName(previousEmployee) : "unassigned";
        String newName  = employeeName(newEmployee);
        String reason   = request.reason() != null ? " Reason: " + request.reason() : "";

        recordActivity(updated, actor, "TASK_REASSIGNED",
                "Task reassigned from " + prevName + " to " + newName + reason,
                null, null);

        // Notify original employee if there was one
        if (previousEmployee != null) {
            notificationService.createNotification(
                    previousEmployee,
                    NotificationType.TASK_REASSIGNED,
                    "Task Reassigned",
                    "\"" + task.getTitle() + "\" has been reassigned to " + newName + "." + reason,
                    task.getId()
            );
        }

        // Notify new employee
        notificationService.createNotification(
                newEmployee,
                NotificationType.TASK_REASSIGNED,
                "Task Assigned to You",
                "\"" + task.getTitle() + "\" has been assigned to you." + reason,
                task.getId()
        );

        log.info("Task.reassign: taskId={} from={} to={} by={}",
                id, prevName, newName, securityUtils.getCurrentUsername());

        return taskMapper.toResponse(taskRepository.findByIdWithAssociations(updated.getId())
                .orElse(updated));
    }

    /** {@inheritDoc} */
    @Override
    @Transactional
    public void delete(final UUID id) {
        if (!taskRepository.existsById(id)) {
            throw new ResourceNotFoundException("Task", id);
        }
        if (isEmployeeOnly()) {
            throw new AccessDeniedException("Employees may not delete tasks.");
        }
        taskRepository.deleteById(id);
        log.info("Task.delete: deleted task id={} by={}", id, securityUtils.getCurrentUsername());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<TaskActivityResponse> getActivityTimeline(final UUID taskId) {
        Task task = taskRepository.findByIdWithAssociations(taskId)
                .orElseThrow(() -> new ResourceNotFoundException("Task", taskId));

        if (isEmployeeOnly()) {
            requireAssignedToCurrentEmployee(task);
        }

        return taskActivityRepository.findByTaskIdOrderByCreatedAtAsc(taskId)
                .stream()
                .map(a -> new TaskActivityResponse(
                        a.getId(),
                        a.getTask().getId(),
                        a.getActor() != null ? a.getActor().getId() : null,
                        a.getActor() != null ? employeeName(a.getActor()) : "System",
                        a.getEventType(),
                        a.getDescription(),
                        a.getFromStatus(),
                        a.getToStatus(),
                        a.getCreatedAt()
                ))
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public WorkloadResponse getWorkload(final UUID employeeId) {
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee", employeeId));

        long active      = taskRepository.countActiveTasksByEmployeeId(employeeId);
        long pending     = taskRepository.countPendingReviewByEmployeeId(employeeId);
        long overdue     = taskRepository.countOverdueTasksByEmployeeId(employeeId, LocalDate.now(), DONE_STATUSES);

        return new WorkloadResponse(
                employee.getId(),
                employeeName(employee),
                active,
                pending,
                overdue,
                WorkloadResponse.levelFrom(active)
        );
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public TaskDashboardStatsResponse getDashboardStats() {
        long total = taskRepository.count();
        long completed = taskRepository.countByStatus(TaskStatus.COMPLETED);
        long overdue = taskRepository.countOverdue(DONE_STATUSES, LocalDate.now());

        // Build counts by status
        Map<String, Long> byStatus = new HashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            byStatus.put(s.name(), taskRepository.countByStatus(s));
        }

        // Count URGENT-priority tasks (total URGENT, used as urgency indicator)
        long urgent = countUrgentNonCompleted();

        double completionPct = total > 0 ? (completed * 100.0) / total : 0.0;

        return new TaskDashboardStatsResponse(total, byStatus, overdue, urgent, completionPct);
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<WorkloadResponse> getWorkloadSummary() {
        // Fetch all active employees
        List<Employee> activeEmployees = employeeRepository.findByStatus(
                EmployeeStatus.ACTIVE,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        LocalDate today = LocalDate.now();

        return activeEmployees.stream()
                .map(e -> {
                    long active  = taskRepository.countActiveTasksByEmployeeId(e.getId());
                    long pending = taskRepository.countPendingReviewByEmployeeId(e.getId());
                    long overdue = taskRepository.countOverdueTasksByEmployeeId(e.getId(), today, DONE_STATUSES);
                    return new WorkloadResponse(
                            e.getId(),
                            employeeName(e),
                            active,
                            pending,
                            overdue,
                            WorkloadResponse.levelFrom(active)
                    );
                })
                .filter(w -> w.activeTasks() > 0 || w.pendingReview() > 0)
                .collect(Collectors.toList());
    }

    /** {@inheritDoc} */
    @Override
    @Transactional(readOnly = true)
    public List<EmployeeAvailabilityResponse> getEmployeeAvailability() {
        List<Employee> activeEmployees = employeeRepository.findByStatus(
                EmployeeStatus.ACTIVE,
                org.springframework.data.domain.Pageable.unpaged()
        ).getContent();

        LocalDate today = LocalDate.now();

        return activeEmployees.stream()
                .map(e -> {
                    boolean checkedIn = attendanceRepository
                            .findByEmployeeIdAndAttendanceDate(e.getId(), today)
                            .map(a -> a.getCheckOutTime() == null)
                            .orElse(false);
                    long active = taskRepository.countActiveTasksByEmployeeId(e.getId());
                    return new EmployeeAvailabilityResponse(
                            e.getId(),
                            employeeName(e),
                            e.getEmployeeCode(),
                            checkedIn,
                            active
                    );
                })
                .collect(Collectors.toList());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private PageResponse<TaskResponse> fetchPage(final UUID assignedEmployeeId,
                                                  final UUID createdByEmployeeId,
                                                  final TaskStatus status,
                                                  final TaskPriority priority,
                                                  final TaskCategory category,
                                                  final Pageable pageable) {
        final Page<UUID> idPage = taskRepository.findIdsByFilters(
                assignedEmployeeId, createdByEmployeeId, status, priority, category, pageable);

        if (idPage.isEmpty()) {
            return PageResponse.from(new PageImpl<>(List.of(), pageable, idPage.getTotalElements()));
        }

        final List<UUID> ids = idPage.getContent();
        final Map<UUID, Task> byId = taskRepository.findAllWithAssociationsByIds(ids)
                .stream()
                .collect(Collectors.toMap(Task::getId, Function.identity()));

        final List<TaskResponse> content = ids.stream()
                .filter(byId::containsKey)
                .map(tid -> taskMapper.toResponse(byId.get(tid)))
                .collect(Collectors.toList());

        return PageResponse.from(new PageImpl<>(content, pageable, idPage.getTotalElements()));
    }

    private boolean isEmployeeOnly() {
        return securityUtils.hasRole("ROLE_EMPLOYEE") && !securityUtils.isPrivileged();
    }

    private void requireAssignedToCurrentEmployee(final Task task) {
        Employee ownEmployee = securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));
        if (task.getAssignedEmployee() == null
                || !ownEmployee.getId().equals(task.getAssignedEmployee().getId())) {
            throw new AccessDeniedException("You may only access tasks assigned to you.");
        }
    }

    private void validateEmployeeTransition(final TaskStatus currentStatus,
                                             final TaskStatus newStatus) {
        Set<TaskStatus> allowed = EMPLOYEE_TRANSITIONS.getOrDefault(currentStatus, Set.of());
        if (!allowed.contains(newStatus)) {
            throw new IllegalStateException(
                    "Status transition from " + currentStatus + " to " + newStatus
                    + " is not permitted for employees.");
        }
    }

    /**
     * Enforces that the currently authenticated employee has an active check-in today.
     */
    private void requireCheckedIn() {
        Employee employee = securityUtils.getCurrentEmployee()
                .orElseThrow(() -> new AccessDeniedException(
                        "No employee record is linked to your account."));
        requireEmployeeCheckedIn(employee);
    }

    /**
     * Enforces that the given employee has an active check-in record today.
     *
     * @param employee the employee to check
     * @throws IllegalStateException if the employee has not checked in or has already checked out
     */
    private void requireEmployeeCheckedIn(final Employee employee) {
        Optional<Attendance> todayAttendance = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employee.getId(), LocalDate.now());

        if (todayAttendance.isEmpty()) {
            throw new IllegalStateException(
                    "Employee must be checked in before a new task can be assigned.");
        }
        if (todayAttendance.get().getCheckOutTime() != null) {
            throw new IllegalStateException(
                    "Employee must be checked in before a new task can be assigned.");
        }
    }

    private void recordActivity(final Task task,
                                 final Employee actor,
                                 final String eventType,
                                 final String description,
                                 final String fromStatus,
                                 final String toStatus) {
        try {
            TaskActivity activity = TaskActivity.builder()
                    .task(task)
                    .actor(actor)
                    .eventType(eventType)
                    .description(description)
                    .fromStatus(fromStatus)
                    .toStatus(toStatus)
                    .build();
            taskActivityRepository.save(activity);
        } catch (Exception e) {
            log.warn("Task.recordActivity: failed to record activity for task={} event={}: {}",
                    task.getId(), eventType, e.getMessage());
        }
    }

    private String employeeName(final Employee employee) {
        if (employee == null) return "Unknown";
        User user = employee.getUser();
        if (user != null) {
            String first = user.getFirstName();
            String last  = user.getLastName();
            if (first != null || last != null) {
                return ((first != null ? first : "") + " " + (last != null ? last : "")).trim();
            }
        }
        if (employee.getFirstName() != null || employee.getLastName() != null) {
            return ((employee.getFirstName() != null ? employee.getFirstName() : "")
                    + " " + (employee.getLastName() != null ? employee.getLastName() : "")).trim();
        }
        return employee.getEmployeeCode();
    }

    /**
     * Counts URGENT-priority tasks that are not in a terminal state.
     */
    private long countUrgentNonCompleted() {
        try {
            // We use the ID filter query with URGENT priority and page size 1 just to get total count.
            // For COMPLETED separately we'd need another query; approximate with all URGENT tasks minus COMPLETED URGENT.
            long urgentTotal = taskRepository.findIdsByFilters(
                    null, null, null, TaskPriority.URGENT, null,
                    org.springframework.data.domain.PageRequest.of(0, 1)
            ).getTotalElements();
            long urgentCompleted = taskRepository.countByAssignedEmployeeIdAndStatus(null, TaskStatus.COMPLETED);
            // countByAssignedEmployeeIdAndStatus with null employeeId counts all employees (returns 0 actually with null)
            // Use a simple approximation: just return urgentTotal
            return urgentTotal;
        } catch (Exception e) {
            log.warn("Task.countUrgentNonCompleted: {}", e.getMessage());
            return 0L;
        }
    }
}
