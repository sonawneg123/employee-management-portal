package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Request DTO for manually creating an attendance record (HR / Admin action).
 *
 * @param employeeId     UUID of the employee
 * @param attendanceDate date of the attendance record
 * @param checkInTime    optional check-in time
 * @param checkOutTime   optional check-out time
 * @param status         attendance status
 * @param notes          optional free-text notes
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for creating a new attendance record")
public record CreateAttendanceRequest(

        @Schema(description = "UUID of the employee", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @Schema(description = "Date of the attendance record", example = "2025-07-01")
        @NotNull(message = "Attendance date is required")
        LocalDate attendanceDate,

        @Schema(description = "Check-in time", example = "09:00:00")
        LocalTime checkInTime,

        @Schema(description = "Check-out time", example = "17:00:00")
        LocalTime checkOutTime,

        @Schema(description = "Attendance status", example = "PRESENT")
        @NotNull(message = "Status is required")
        AttendanceStatus status,

        @Schema(description = "Optional notes", example = "Worked from client site")
        String notes
) {
}
