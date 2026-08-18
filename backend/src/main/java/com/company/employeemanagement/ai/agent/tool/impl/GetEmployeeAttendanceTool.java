package com.company.employeemanagement.ai.agent.tool.impl;

import com.company.employeemanagement.ai.agent.tool.AgentToolContext;
import com.company.employeemanagement.dto.response.AttendanceResponse;
import com.company.employeemanagement.dto.response.PageResponse;
import com.company.employeemanagement.entity.enums.AttendanceStatus;
import com.company.employeemanagement.service.AttendanceService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tool: get_employee_attendance
 * Returns attendance records for an employee or today's attendance summary.
 * EMPLOYEE role sees only their own attendance (via findMyAttendance).
 * ADMIN/HR/MANAGER can query any employee's attendance.
 */
@Component
public class GetEmployeeAttendanceTool extends AbstractAgentTool {

    private final AttendanceService attendanceService;

    public GetEmployeeAttendanceTool(final AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    @Override public String getName() { return "get_employee_attendance"; }

    @Override public String getDescription() {
        return "Use this tool whenever the user asks about attendance — whether they are present today, "
               + "their check-in/check-out times, their attendance history, "
               + "or (for managers/HR) who is present or absent on a given date. "
               + "Trigger phrases: 'what is my attendance today', 'am I checked in', "
               + "'my attendance status', 'who is present today', 'who checked in today', "
               + "'attendance for [date]', 'show my attendance history'. "
               + "Parameters: employeeId (optional UUID — managers/HR only), "
               + "date (optional YYYY-MM-DD, defaults to today), "
               + "status (optional: PRESENT/ABSENT/LATE/HALF_DAY/ON_LEAVE/REMOTE). "
               + "Employees automatically see only their own records. "
               + "Never answer attendance questions from general knowledge — always call this tool.";
    }

    @Override public String getParameterSchema() {
        return """
               {"type":"object","properties":{
               "employeeId":{"type":"string","description":"Employee UUID (managers/HR only — employees always see their own)"},
               "date":{"type":"string","description":"Date YYYY-MM-DD, defaults to today"},
               "status":{"type":"string","description":"Attendance status filter"}},
               "required":[]}""";
    }

    @Override public Set<String> getAllowedRoles() {
        return Set.of("ROLE_ADMIN", "ROLE_HR", "ROLE_MANAGER", "ROLE_EMPLOYEE");
    }

    @Override public boolean isRequiresConfirmation() { return false; }
    @Override public boolean isReadOnly() { return true; }

    @Override
    protected String doExecute(final Map<String, String> args, final AgentToolContext context) {
        String employeeIdStr = arg(args, "employeeId");
        String dateStr = arg(args, "date");
        String statusStr = arg(args, "status");

        LocalDate date = null;
        if (dateStr != null) {
            try { date = LocalDate.parse(dateStr); }
            catch (Exception e) { return "Invalid date format, use YYYY-MM-DD"; }
        }

        AttendanceStatus status = null;
        if (statusStr != null) {
            try { status = AttendanceStatus.valueOf(statusStr.toUpperCase()); }
            catch (IllegalArgumentException e) { return "Invalid status: " + statusStr; }
        }

        boolean isEmployeeOnly = context.roles().contains("ROLE_EMPLOYEE")
                && !context.roles().contains("ROLE_ADMIN")
                && !context.roles().contains("ROLE_HR")
                && !context.roles().contains("ROLE_MANAGER");

        PageResponse<AttendanceResponse> page;

        if (isEmployeeOnly) {
            // Employees always see only their own attendance via the scoped service method
            if (date == null) {
                date = LocalDate.now();
            }
            page = attendanceService.findMyAttendance(
                    date, status,
                    PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "attendanceDate")));
        } else {
            // Managers/HR/Admin can query any employee
            UUID employeeId = null;
            if (employeeIdStr != null) {
                try { employeeId = UUID.fromString(employeeIdStr); }
                catch (IllegalArgumentException e) { return "Invalid employeeId: " + employeeIdStr; }
            }
            if (date == null && employeeId == null) {
                date = LocalDate.now();
            }
            page = attendanceService.findAll(
                    employeeId, date, status,
                    PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, "attendanceDate")));
        }

        if (page.content().isEmpty()) {
            return "No attendance records found for the given criteria.";
        }

        return "Attendance records (" + page.totalElements() + " total):\n" +
               page.content().stream().map(this::formatAttendance).collect(Collectors.joining("\n"));
    }

    private String formatAttendance(final AttendanceResponse a) {
        return String.format("%s — %s | Status: %s | Check-in: %s | Check-out: %s",
                a.employeeName(), a.attendanceDate(), a.status(),
                a.checkInTime() != null ? a.checkInTime() : "N/A",
                a.checkOutTime() != null ? a.checkOutTime() : "N/A");
    }
}
