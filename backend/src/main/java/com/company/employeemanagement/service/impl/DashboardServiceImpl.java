package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.dto.response.ActivityItemResponse;
import com.company.employeemanagement.dto.response.DashboardChartsResponse;
import com.company.employeemanagement.dto.response.DashboardSummaryResponse;
import com.company.employeemanagement.entity.LeaveRequest;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.entity.enums.EmployeeStatus;
import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.repository.AttendanceRepository;
import com.company.employeemanagement.repository.DepartmentRepository;
import com.company.employeemanagement.repository.EmployeeRepository;
import com.company.employeemanagement.repository.LeaveRequestRepository;
import com.company.employeemanagement.service.DashboardService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of {@link DashboardService} that aggregates KPIs, chart data,
 * and recent activity from the existing repositories.
 *
 * <p>All methods are read-only ({@code @Transactional(readOnly = true)}).
 * No new state is created or modified. All counts are computed directly in the
 * database via JPQL aggregation queries added to the relevant repositories.
 *
 * @author Employee Management Portal Team
 */
@Service
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private final EmployeeRepository    employeeRepository;
    private final DepartmentRepository  departmentRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final AttendanceRepository  attendanceRepository;

    /**
     * Constructs the service with all required repository dependencies.
     *
     * @param employeeRepository     repository for employee counts
     * @param departmentRepository   repository for department distribution
     * @param leaveRequestRepository repository for leave request counts
     * @param attendanceRepository   repository for attendance counts
     */
    public DashboardServiceImpl(
            final EmployeeRepository    employeeRepository,
            final DepartmentRepository  departmentRepository,
            final LeaveRequestRepository leaveRequestRepository,
            final AttendanceRepository  attendanceRepository) {
        this.employeeRepository    = employeeRepository;
        this.departmentRepository  = departmentRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.attendanceRepository  = attendanceRepository;
    }

    // ── getSummary ─────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Executes the following aggregation queries:
     * <ul>
     *   <li>{@code COUNT(*) FROM employees} — total headcount</li>
     *   <li>{@code COUNT(*) FROM departments} — department count</li>
     *   <li>{@code COUNT(*) FROM leave_requests WHERE status='PENDING'} — pending leaves</li>
     *   <li>{@code COUNT(*) FROM employees WHERE status='ACTIVE'} — active employees</li>
     *   <li>Attendance count for today with status PRESENT</li>
     *   <li>Approved leave count spanning today</li>
     *   <li>Employees whose date_of_joining ≥ first day of current month</li>
     *   <li>Month-over-month trend by comparing current vs previous month joins</li>
     *   <li>Attendance rate: today vs yesterday present counts</li>
     * </ul>
     */
    @Override
    public DashboardSummaryResponse getSummary() {
        final LocalDate today     = LocalDate.now();
        final LocalDate yesterday = today.minusDays(1);

        // ── Head-count KPIs ───────────────────────────────────────────────────
        final long totalEmployees   = employeeRepository.count();
        final long totalDepartments = departmentRepository.count();
        final long pendingLeaves    = leaveRequestRepository.countByStatus(LeaveStatus.PENDING);
        final long activeEmployees  = employeeRepository.countByStatus(EmployeeStatus.ACTIVE);

        // ── Attendance KPIs ───────────────────────────────────────────────────
        final long presentToday     = attendanceRepository
                .countByAttendanceDateAndStatus(today, AttendanceStatus.PRESENT);
        final long presentYesterday = attendanceRepository
                .countByAttendanceDateAndStatus(yesterday, AttendanceStatus.PRESENT);

        // ── Leave KPIs ────────────────────────────────────────────────────────
        final long onLeaveToday = leaveRequestRepository
                .countByStatusAndDateRange(LeaveStatus.APPROVED, today);

        // ── New-this-month ────────────────────────────────────────────────────
        final LocalDate firstOfMonth = today.withDayOfMonth(1);
        final long newThisMonth = employeeRepository
                .countByDateOfJoiningOnOrAfter(firstOfMonth);

        // ── Month-over-month trend ────────────────────────────────────────────
        final LocalDate firstOfLastMonth = firstOfMonth.minusMonths(1);
        final LocalDate lastOfLastMonth  = firstOfMonth.minusDays(1);
        final long lastMonthJoins = employeeRepository
                .countByDateOfJoiningBetween(firstOfLastMonth, lastOfLastMonth);
        final long trendEmployees = newThisMonth - lastMonthJoins;

        // ── Pending-leaves trend (vs 7 days ago) ──────────────────────────────
        final LocalDate weekAgo = today.minusDays(7);
        final long pendingLastWeek = leaveRequestRepository
                .countByStatusCreatedOnOrBefore(LeaveStatus.PENDING, weekAgo);
        final long trendLeaves = pendingLeaves - pendingLastWeek;

        // ── Attendance rate and trend ─────────────────────────────────────────
        double attendanceRate  = totalEmployees > 0
                ? (double) presentToday / totalEmployees : 0.0;
        double yesterdayRate   = totalEmployees > 0
                ? (double) presentYesterday / totalEmployees : 0.0;
        double trendAttendance = attendanceRate - yesterdayRate;

        return new DashboardSummaryResponse(
                totalEmployees,
                totalDepartments,
                pendingLeaves,
                activeEmployees,
                presentToday,
                onLeaveToday,
                newThisMonth,
                trendEmployees,
                trendLeaves,
                trendAttendance,
                attendanceRate
        );
    }

    // ── getCharts ──────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Executes three aggregation queries:
     * <ul>
     *   <li>Department distribution — {@code GROUP BY department}</li>
     *   <li>Employee status breakdown — {@code GROUP BY status}</li>
     *   <li>Last-14-days attendance trend — daily present/absent counts</li>
     * </ul>
     */
    @Override
    public DashboardChartsResponse getCharts() {
        // ── Department distribution ───────────────────────────────────────────
        final List<Object[]> deptRows = departmentRepository.findDepartmentDistribution();
        final List<DashboardChartsResponse.DepartmentDistribution> departmentDistribution =
                new ArrayList<>(deptRows.size());
        for (final Object[] row : deptRows) {
            departmentDistribution.add(new DashboardChartsResponse.DepartmentDistribution(
                    (String) row[0],          // name
                    (String) row[1],          // code
                    ((Number) row[2]).longValue() // count
            ));
        }

        // ── Employee status breakdown ─────────────────────────────────────────
        final List<Object[]> statusRows = employeeRepository.countGroupByStatus();
        final List<DashboardChartsResponse.EmployeeStatusCount> employeeStatusBreakdown =
                new ArrayList<>(statusRows.size());
        for (final Object[] row : statusRows) {
            employeeStatusBreakdown.add(new DashboardChartsResponse.EmployeeStatusCount(
                    row[0].toString(),            // status enum name
                    ((Number) row[1]).longValue() // count
            ));
        }

        // ── Attendance trend (last 14 days) ───────────────────────────────────
        final LocalDate today  = LocalDate.now();
        final LocalDate from14 = today.minusDays(13);  // inclusive, gives 14 days
        final List<Object[]> trendRows = attendanceRepository
                .countPresentAndTotalByDateRange(from14, today, AttendanceStatus.PRESENT);

        final long totalEmployees = employeeRepository.count();
        final List<DashboardChartsResponse.AttendanceTrendPoint> attendanceTrend =
                new ArrayList<>(trendRows.size());
        for (final Object[] row : trendRows) {
            final LocalDate date    = (LocalDate) row[0];
            final long      present = ((Number) row[1]).longValue();
            final long      total   = ((Number) row[2]).longValue();
            final long      absent  = Math.max(0L, totalEmployees - present);
            attendanceTrend.add(new DashboardChartsResponse.AttendanceTrendPoint(
                    date.format(DATE_FORMATTER),
                    present,
                    absent
            ));
        }

        return new DashboardChartsResponse(
                departmentDistribution,
                employeeStatusBreakdown,
                attendanceTrend
        );
    }

    // ── getActivity ────────────────────────────────────────────────────────────

    /**
     * {@inheritDoc}
     *
     * <p>Derives the activity feed from the most recent leave request events,
     * since those represent real human-initiated actions with named actors.
     * Future phases may extend this with employee-creation and approval events.
     *
     * @param limit maximum number of events (clamped to 50)
     */
    @Override
    public List<ActivityItemResponse> getActivity(final int limit) {
        final int clampedLimit = Math.min(Math.max(limit, 1), 50);

        final List<LeaveRequest> recent = leaveRequestRepository
                .findRecentWithEmployee(PageRequest.of(0, clampedLimit))
                .getContent();

        final List<ActivityItemResponse> result = new ArrayList<>(recent.size());
        for (final LeaveRequest lr : recent) {
            final String actorName = resolveEmployeeName(lr);
            final String type      = resolveActivityType(lr);
            final String desc      = buildDescription(lr, actorName);
            final String timestamp = formatTimestamp(lr.getCreatedAt());

            result.add(new ActivityItemResponse(
                    lr.getId().toString(),
                    type,
                    desc,
                    timestamp,
                    actorName,
                    null
            ));
        }
        return result;
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    /**
     * Returns the employee's full name from the linked user account,
     * or falls back to the employee code when no user is linked.
     *
     * @param lr the leave request
     * @return display name string
     */
    private String resolveEmployeeName(final LeaveRequest lr) {
        if (lr.getEmployee() == null) {
            return "Unknown";
        }
        if (lr.getEmployee().getUser() != null) {
            final var user = lr.getEmployee().getUser();
            return user.getFirstName() + " " + user.getLastName();
        }
        return lr.getEmployee().getEmployeeCode();
    }

    /**
     * Maps the leave request status to an activity type key that matches the
     * frontend's {@code ACTIVITY_TYPE_META} map.
     *
     * @param lr the leave request
     * @return activity type key string
     */
    private String resolveActivityType(final LeaveRequest lr) {
        return switch (lr.getStatus()) {
            case APPROVED  -> "LEAVE_APPROVED";
            case REJECTED  -> "LEAVE_REJECTED";
            case CANCELLED -> "LEAVE_CANCELLED";
            default        -> "LEAVE_REQUESTED";
        };
    }

    /**
     * Builds a human-readable description for the activity item.
     *
     * @param lr       the leave request
     * @param actorName the employee's display name
     * @return description string
     */
    private String buildDescription(final LeaveRequest lr, final String actorName) {
        return switch (lr.getStatus()) {
            case APPROVED  -> actorName + "'s " + formatLeaveType(lr) + " leave was approved";
            case REJECTED  -> actorName + "'s " + formatLeaveType(lr) + " leave was rejected";
            case CANCELLED -> actorName + " cancelled their " + formatLeaveType(lr) + " leave";
            default        -> actorName + " requested " + formatLeaveType(lr) + " leave";
        };
    }

    /**
     * Formats the leave type into a lowercase human-readable word.
     *
     * @param lr the leave request
     * @return lowercase leave type label
     */
    private String formatLeaveType(final LeaveRequest lr) {
        if (lr.getLeaveType() == null) {
            return "";
        }
        return lr.getLeaveType().name().toLowerCase().replace('_', ' ');
    }

    /**
     * Formats a {@link LocalDateTime} as an ISO-8601 string, or returns an
     * empty string when the timestamp is null.
     *
     * @param dt the timestamp to format, may be null
     * @return ISO-8601 string or empty string
     */
    private String formatTimestamp(final LocalDateTime dt) {
        return dt != null ? dt.toString() : "";
    }
}
