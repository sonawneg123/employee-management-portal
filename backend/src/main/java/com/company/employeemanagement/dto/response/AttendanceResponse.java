package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

/**
 * Response DTO representing an attendance record returned to API consumers.
 *
 * @param id             UUID primary key of the attendance record
 * @param employeeId     UUID of the employee
 * @param employeeCode   unique HR-assigned employee code
 * @param employeeName   full name of the employee (or employee code if no linked user)
 * @param attendanceDate date of the attendance record
 * @param checkInTime    time of check-in, or {@code null}
 * @param checkOutTime   time of check-out, or {@code null}
 * @param status         attendance status for the day
 * @param notes          optional free-text notes
 * @param createdAt      record creation timestamp
 * @param updatedAt      record last-modified timestamp
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Attendance record as returned by the API")
public record AttendanceResponse(

        @Schema(description = "UUID of the attendance record",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "UUID of the employee",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Employee code", example = "EMP-0001")
        String employeeCode,

        @Schema(description = "Full name of the employee", example = "Jane Smith")
        String employeeName,

        @Schema(description = "Date of the attendance record", example = "2025-07-01")
        LocalDate attendanceDate,

        @Schema(description = "Check-in time", example = "09:00:00")
        LocalTime checkInTime,

        @Schema(description = "Check-out time", example = "17:00:00")
        LocalTime checkOutTime,

        @Schema(description = "Attendance status", example = "PRESENT")
        AttendanceStatus status,

        @Schema(description = "Optional notes", example = "Worked from client site")
        String notes,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt
) {
}
