package com.company.employeemanagement.dto.request;

import com.company.employeemanagement.entity.enums.AttendanceStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalTime;

/**
 * Request DTO for updating an existing attendance record.
 *
 * @param checkInTime  updated check-in time (nullable)
 * @param checkOutTime updated check-out time (nullable)
 * @param status       updated attendance status
 * @param notes        updated free-text notes (nullable)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for updating an attendance record")
public record UpdateAttendanceRequest(

        @Schema(description = "Updated check-in time", example = "08:45:00")
        LocalTime checkInTime,

        @Schema(description = "Updated check-out time", example = "17:30:00")
        LocalTime checkOutTime,

        @Schema(description = "Updated attendance status", example = "WORK_FROM_HOME")
        @NotNull(message = "Status is required")
        AttendanceStatus status,

        @Schema(description = "Updated notes", example = "Left early - medical appointment")
        String notes
) {
}
