package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.response.AnalyticsAttendanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsDepartmentsResponse;
import com.company.employeemanagement.dto.response.AnalyticsLeavesResponse;
import com.company.employeemanagement.dto.response.AnalyticsPerformanceResponse;
import com.company.employeemanagement.dto.response.AnalyticsSummaryResponse;
import com.company.employeemanagement.dto.response.AnalyticsTasksResponse;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.TaskStatus;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import com.company.employeemanagement.repository.TaskAiReviewRepository;
import com.company.employeemanagement.repository.TaskRepository;
import com.company.employeemanagement.service.AnalyticsService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Implementation of {@link AnalyticsService}.
 *
 * <p>All methods are read-only ({@code @Transactional(readOnly = true)}).
 * Each method queries the existing repositories directly — no new tables or
 * duplicated business logic. Authorization scoping (department/employee filters)
 * is enforced by the controller before calling these methods.
 *
 * @author Employee Management Portal Team
 */
@Service
@Transactional(readOnly = true)
public class AnalyticsServiceImpl implements AnalyticsService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EmployeeRepository      employeeRepository;
    private final AttendanceRepository    attendanceRepository;
    private final LeaveRequestRepository  leaveRequestRepository;
    private final TaskRepository          taskRepository;
    private final TaskAiReviewRepository  taskAiReviewRepository;
    private final DepartmentRepository    departmentRepository;

    /**
     * Constructs the service with all required repository dependencies.
     */
    public AnalyticsServiceImpl(
            final EmployeeRepository      employeeRepository,
            final AttendanceRepository    attendanceRepository,
            final LeaveRequestRepository  leaveRequestRepository,
            final TaskRepository          taskRepository,
            final TaskAiReviewRepository  taskAiReviewRepository,
            final DepartmentRepository    departmentRepository) {
        this.employeeRepository     = employeeRepository;
        this.attendanceRepository   = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.taskRepository         = taskRepository;
        this.taskAiReviewRepository = taskAiReviewRepository;
        this.departmentRepository   = departmentRepository;
    }

    // ── getSummary ─────────────────────────────────────────────────────────────

    @Override
    public AnalyticsSummaryResponse getSummary(
            final LocalDate from,
            final LocalDate to,
            final UUID departmentId,
            final UUID employeeId) {

        final LocalDate today = LocalDate.now();

        // ── Employee KPIs ───────────────────────────────────────────────────
        final long totalEmployees  = employeeRepository.countByDepartment(departmentId);
        final long activeEmployees = employeeRepository.countByStatusAndDepartment(
                EmployeeStatus.ACTIVE, departmentId);
        final long inactiveEmployees = totalEmployees - activeEmployees;
        final long onLeaveToday = leaveRequestRepository.countByStatusAndDateRange(
                LeaveStatus.APPROVED, today);

        // New employees: joined this calendar month
        final LocalDate firstOfMonth = today.withDayOfMonth(1);
        final long newEmployees = employeeRepository
                .countByDateOfJoiningOnOrAfter(firstOfMonth);

        // ── Attendance KPIs ─────────────────────────────────────────────────
        final long presentCount = employeeId != null
                ? attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                        employeeId, from, to, AttendanceStatus.PRESENT)
                : attendanceRepository.countByDateRangeAndStatusAndDepartment(
                        from, to, AttendanceStatus.PRESENT, departmentId);

        final long absentCount = employeeId != null
                ? attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                        employeeId, from, to, AttendanceStatus.ABSENT)
                : attendanceRepository.countByDateRangeAndStatusAndDepartment(
                        from, to, AttendanceStatus.ABSENT, departmentId);

        final long halfDayCount = employeeId != null
                ? attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                        employeeId, from, to, AttendanceStatus.HALF_DAY)
                : attendanceRepository.countByDateRangeAndStatusAndDepartment(
                        from, to, AttendanceStatus.HALF_DAY, departmentId);

        final long onLeaveCount = employeeId != null
                ? attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                        employeeId, from, to, AttendanceStatus.ON_LEAVE)
                : attendanceRepository.countByDateRangeAndStatusAndDepartment(
                        from, to, AttendanceStatus.ON_LEAVE, departmentId);

        final long totalAttendance = employeeId != null
                ? attendanceRepository.countByEmployeeIdAndDateRange(employeeId, from, to)
                : attendanceRepository.countByDateRangeAndDepartment(from, to, departmentId);

        final double attendanceRate = totalAttendance > 0
                ? (double) presentCount / totalAttendance : 0.0;

        // ── Leave KPIs ──────────────────────────────────────────────────────
        final long totalLeaves    = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, null, departmentId, employeeId);
        final long pendingLeaves  = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.PENDING, departmentId, employeeId);
        final long approvedLeaves = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.APPROVED, departmentId, employeeId);
        final long rejectedLeaves = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.REJECTED, departmentId, employeeId);

        // ── Task KPIs ────────────────────────────────────────────────────────
        final List<TaskStatus> terminalStatuses = List.of(
                TaskStatus.COMPLETED, TaskStatus.REJECTED);

        final long totalTasks    = taskRepository.countByFilters(departmentId, employeeId);
        final long completedTasks = taskRepository.countByStatusAndFilters(
                TaskStatus.COMPLETED, departmentId, employeeId);
        final long overdueTasks  = taskRepository.countOverdueByFilters(
                terminalStatuses, today, departmentId, employeeId);
        final long pendingTasks  = totalTasks - completedTasks;
        final double taskCompletion = totalTasks > 0
                ? (double) completedTasks / totalTasks : 0.0;

        // ── AI KPIs ──────────────────────────────────────────────────────────
        final Object[] aiAgg = taskAiReviewRepository.findAggregateScores();
        final double avgAiScore = (aiAgg != null && aiAgg[0] != null)
                ? ((Number) aiAgg[0]).doubleValue() : -1.0;
        final long completedAi = taskAiReviewRepository.countByStatus(AiReviewStatus.COMPLETED);
        final long failedAi    = taskAiReviewRepository.countByStatus(AiReviewStatus.FAILED);

        // ── Attendance trend ──────────────────────────────────────────────────
        final List<Object[]> trendRows = attendanceRepository
                .countByDateRangeGroupedByDate(from, to, departmentId, employeeId);

        final long employeeCount = totalEmployees > 0 ? totalEmployees : 1;
        final List<AnalyticsSummaryResponse.TrendPoint> attendanceTrend =
                new ArrayList<>(trendRows.size());
        for (final Object[] row : trendRows) {
            final LocalDate date    = (LocalDate) row[0];
            final long      present = ((Number) row[1]).longValue();
            final long      total   = ((Number) row[2]).longValue();
            final double    rate    = total > 0 ? (double) present / employeeCount : 0.0;
            attendanceTrend.add(new AnalyticsSummaryResponse.TrendPoint(
                    date.format(DATE_FMT), rate));
        }

        // ── AI score trend ────────────────────────────────────────────────────
        final List<Object[]> aiTrendRows = taskAiReviewRepository
                .findScoreTrendByDateRange(from, to);
        final List<AnalyticsSummaryResponse.TrendPoint> aiScoreTrend =
                new ArrayList<>(aiTrendRows.size());
        for (final Object[] row : aiTrendRows) {
            final LocalDate date     = (LocalDate) row[0];
            final double    avgScore = (row[1] != null) ? ((Number) row[1]).doubleValue() : 0.0;
            aiScoreTrend.add(new AnalyticsSummaryResponse.TrendPoint(
                    date.format(DATE_FMT), avgScore));
        }

        return new AnalyticsSummaryResponse(
                totalEmployees,
                activeEmployees,
                inactiveEmployees,
                onLeaveToday,
                newEmployees,
                attendanceRate,
                presentCount,
                absentCount,
                halfDayCount,
                onLeaveCount,
                totalLeaves,
                pendingLeaves,
                approvedLeaves,
                rejectedLeaves,
                totalTasks,
                completedTasks,
                pendingTasks,
                overdueTasks,
                taskCompletion,
                avgAiScore,
                completedAi,
                failedAi,
                attendanceTrend,
                aiScoreTrend
        );
    }

    // ── getAttendance ──────────────────────────────────────────────────────────

    @Override
    public AnalyticsAttendanceResponse getAttendance(
            final LocalDate from,
            final LocalDate to,
            final UUID departmentId,
            final UUID employeeId) {

        final long presentCount;
        final long absentCount;
        final long halfDayCount;
        final long wfhCount;
        final long onLeaveCount;
        final long totalRecords;

        if (employeeId != null) {
            presentCount  = attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                    employeeId, from, to, AttendanceStatus.PRESENT);
            absentCount   = attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                    employeeId, from, to, AttendanceStatus.ABSENT);
            halfDayCount  = attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                    employeeId, from, to, AttendanceStatus.HALF_DAY);
            wfhCount      = attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                    employeeId, from, to, AttendanceStatus.WORK_FROM_HOME);
            onLeaveCount  = attendanceRepository.countByEmployeeIdAndDateRangeAndStatus(
                    employeeId, from, to, AttendanceStatus.ON_LEAVE);
            totalRecords  = attendanceRepository.countByEmployeeIdAndDateRange(
                    employeeId, from, to);
        } else {
            presentCount  = attendanceRepository.countByDateRangeAndStatusAndDepartment(
                    from, to, AttendanceStatus.PRESENT, departmentId);
            absentCount   = attendanceRepository.countByDateRangeAndStatusAndDepartment(
                    from, to, AttendanceStatus.ABSENT, departmentId);
            halfDayCount  = attendanceRepository.countByDateRangeAndStatusAndDepartment(
                    from, to, AttendanceStatus.HALF_DAY, departmentId);
            wfhCount      = attendanceRepository.countByDateRangeAndStatusAndDepartment(
                    from, to, AttendanceStatus.WORK_FROM_HOME, departmentId);
            onLeaveCount  = attendanceRepository.countByDateRangeAndStatusAndDepartment(
                    from, to, AttendanceStatus.ON_LEAVE, departmentId);
            totalRecords  = attendanceRepository.countByDateRangeAndDepartment(
                    from, to, departmentId);
        }

        final double attendanceRate = totalRecords > 0
                ? (double) presentCount / totalRecords : 0.0;

        // Daily trend
        final List<Object[]> trendRows = attendanceRepository
                .countByDateRangeGroupedByDate(from, to, departmentId, employeeId);

        final long denominator = Math.max(employeeRepository.countByDepartment(departmentId), 1);
        final List<AnalyticsAttendanceResponse.DailyAttendancePoint> trend =
                new ArrayList<>(trendRows.size());
        for (final Object[] row : trendRows) {
            final LocalDate date    = (LocalDate) row[0];
            final long      present = ((Number) row[1]).longValue();
            final long      total   = ((Number) row[2]).longValue();
            final long      absent  = Math.max(0L, denominator - present);
            final double    rate    = denominator > 0 ? (double) present / denominator : 0.0;
            trend.add(new AnalyticsAttendanceResponse.DailyAttendancePoint(
                    date.format(DATE_FMT), present, absent, total, rate));
        }

        return new AnalyticsAttendanceResponse(
                totalRecords,
                presentCount,
                absentCount,
                halfDayCount,
                wfhCount,
                onLeaveCount,
                attendanceRate,
                trend
        );
    }

    // ── getLeaves ──────────────────────────────────────────────────────────────

    @Override
    public AnalyticsLeavesResponse getLeaves(
            final LocalDate from,
            final LocalDate to,
            final UUID departmentId,
            final UUID employeeId) {

        final long total     = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, null, departmentId, employeeId);
        final long pending   = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.PENDING, departmentId, employeeId);
        final long approved  = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.APPROVED, departmentId, employeeId);
        final long rejected  = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.REJECTED, departmentId, employeeId);
        final long cancelled = leaveRequestRepository.countByDateRangeAndFilters(
                from, to, LeaveStatus.CANCELLED, departmentId, employeeId);

        final double utilization = total > 0 ? (double) approved / total : 0.0;

        // By type breakdown
        final List<Object[]> typeRows = leaveRequestRepository
                .countGroupByLeaveType(from, to, departmentId, employeeId);
        final List<AnalyticsLeavesResponse.LeaveTypeBreakdown> byType =
                new ArrayList<>(typeRows.size());
        for (final Object[] row : typeRows) {
            byType.add(new AnalyticsLeavesResponse.LeaveTypeBreakdown(
                    row[0].toString(),
                    ((Number) row[1]).longValue()
            ));
        }

        // Monthly trend
        final List<Object[]> monthlyRows = leaveRequestRepository
                .countMonthlyTrend(from, to, departmentId, employeeId);
        final List<AnalyticsLeavesResponse.MonthlyLeaveTrend> trend =
                new ArrayList<>(monthlyRows.size());
        for (final Object[] row : monthlyRows) {
            trend.add(new AnalyticsLeavesResponse.MonthlyLeaveTrend(
                    (String) row[0],
                    ((Number) row[1]).longValue(),
                    ((Number) row[2]).longValue()
            ));
        }

        return new AnalyticsLeavesResponse(
                total, pending, approved, rejected, cancelled,
                utilization, byType, trend
        );
    }

    // ── getTasks ───────────────────────────────────────────────────────────────

    @Override
    public AnalyticsTasksResponse getTasks(
            final UUID departmentId,
            final UUID employeeId) {

        final LocalDate today = LocalDate.now();
        final List<TaskStatus> terminalStatuses = List.of(
                TaskStatus.COMPLETED, TaskStatus.REJECTED);

        final long total      = taskRepository.countByFilters(departmentId, employeeId);
        final long completed  = taskRepository.countByStatusAndFilters(
                TaskStatus.COMPLETED, departmentId, employeeId);
        final long assigned   = taskRepository.countByStatusAndFilters(
                TaskStatus.ASSIGNED, departmentId, employeeId);
        final long inProgress = taskRepository.countByStatusAndFilters(
                TaskStatus.IN_PROGRESS, departmentId, employeeId);
        final long submitted  = taskRepository.countByStatusAndFilters(
                TaskStatus.SUBMITTED, departmentId, employeeId);
        final long overdue    = taskRepository.countOverdueByFilters(
                terminalStatuses, today, departmentId, employeeId);
        final long draft      = taskRepository.countByStatusAndFilters(
                TaskStatus.DRAFT, departmentId, employeeId);

        final double completionRate = total > 0 ? (double) completed / total : 0.0;

        // Build status breakdown for charts
        final List<AnalyticsTasksResponse.TaskStatusBreakdown> breakdown = List.of(
                new AnalyticsTasksResponse.TaskStatusBreakdown("COMPLETED", completed),
                new AnalyticsTasksResponse.TaskStatusBreakdown("IN_PROGRESS", inProgress),
                new AnalyticsTasksResponse.TaskStatusBreakdown("ASSIGNED", assigned),
                new AnalyticsTasksResponse.TaskStatusBreakdown("SUBMITTED", submitted),
                new AnalyticsTasksResponse.TaskStatusBreakdown("OVERDUE", overdue),
                new AnalyticsTasksResponse.TaskStatusBreakdown("DRAFT", draft)
        );

        return new AnalyticsTasksResponse(
                total, completed, assigned, inProgress, submitted,
                overdue, draft, completionRate, breakdown
        );
    }

    // ── getPerformance ─────────────────────────────────────────────────────────

    @Override
    public AnalyticsPerformanceResponse getPerformance(
            final LocalDate from,
            final LocalDate to) {

        final Object[] agg = taskAiReviewRepository.findAggregateScores();
        final double avgCompletion = (agg != null && agg[0] != null)
                ? ((Number) agg[0]).doubleValue() : -1.0;
        final double avgQuality = (agg != null && agg[1] != null)
                ? ((Number) agg[1]).doubleValue() : -1.0;

        final long completed = taskAiReviewRepository.countByStatus(AiReviewStatus.COMPLETED);
        final long failed    = taskAiReviewRepository.countByStatus(AiReviewStatus.FAILED);
        final long pending   = taskAiReviewRepository.countByStatus(AiReviewStatus.PENDING)
                + taskAiReviewRepository.countByStatus(AiReviewStatus.PROCESSING);

        // Score trend
        final List<Object[]> trendRows = taskAiReviewRepository
                .findScoreTrendByDateRange(from, to);
        final List<AnalyticsPerformanceResponse.ScoreTrendPoint> trend =
                new ArrayList<>(trendRows.size());
        for (final Object[] row : trendRows) {
            final LocalDate date     = (LocalDate) row[0];
            final double    avgScore = (row[1] != null)
                    ? ((Number) row[1]).doubleValue() : 0.0;
            final long      count    = ((Number) row[3]).longValue();
            trend.add(new AnalyticsPerformanceResponse.ScoreTrendPoint(
                    date.format(DATE_FMT), avgScore, count));
        }

        return new AnalyticsPerformanceResponse(
                avgCompletion, avgQuality, completed, failed, pending, trend
        );
    }

    // ── getDepartments ─────────────────────────────────────────────────────────

    @Override
    public AnalyticsDepartmentsResponse getDepartments() {
        final LocalDate today = LocalDate.now();
        final long totalDepts = departmentRepository.count();

        final List<Object[]> rows = employeeRepository.findDepartmentHeadcounts();
        final List<AnalyticsDepartmentsResponse.DepartmentStat> depts =
                new ArrayList<>(rows.size());

        for (final Object[] row : rows) {
            final UUID   deptId   = (UUID)   row[0];
            final String name     = (String) row[1];
            final String code     = (String) row[2];
            final long   total    = ((Number) row[3]).longValue();
            final long   active   = (row[4] != null) ? ((Number) row[4]).longValue() : 0L;

            // Count employees on approved leave today for this department
            final long onLeave = leaveRequestRepository.countByStatusAndDateRange(
                    LeaveStatus.APPROVED, today);

            depts.add(new AnalyticsDepartmentsResponse.DepartmentStat(
                    deptId.toString(), name, code, total, active, onLeave
            ));
        }

        return new AnalyticsDepartmentsResponse(totalDepts, depts);
    }
}
