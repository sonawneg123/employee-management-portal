package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.LeaveStatus;
import com.company.employeemanagement.entity.enums.LeaveType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a leave request returned to API consumers.
 *
 * @param id              UUID primary key of the leave request
 * @param employeeId      UUID of the employee who submitted the request
 * @param employeeCode    unique HR-assigned code of the requesting employee
 * @param employeeName    full name of the requesting employee (may be null if no linked user)
 * @param departmentName  department of the requesting employee
 * @param leaveType       category of leave
 * @param startDate       inclusive first day of the leave period
 * @param endDate         inclusive last day of the leave period
 * @param totalDays       computed number of calendar days (endDate - startDate + 1)
 * @param reason          optional justification provided by the employee
 * @param status          current approval status
 * @param rejectionReason reason supplied by the reviewer on rejection, or {@code null}
 * @param reviewedBy      UUID of the reviewer, or {@code null} while pending
 * @param reviewedAt      timestamp of the review decision, or {@code null} while pending
 * @param createdAt       record creation timestamp
 * @param updatedAt       record last-modified timestamp
 * @param createdBy       email of the principal who submitted this request, or {@code "SYSTEM"}
 * @param updatedBy       email of the principal who last modified this request, or {@code "SYSTEM"}
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Leave request record as returned by the API")
public record LeaveRequestResponse(

        @Schema(description = "UUID of the leave request",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID id,

        @Schema(description = "UUID of the requesting employee",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        UUID employeeId,

        @Schema(description = "Employee code of the requester", example = "EMP-0001")
        String employeeCode,

        @Schema(description = "Full name of the requesting employee", example = "John Doe")
        String employeeName,

        @Schema(description = "Department of the requesting employee", example = "Engineering")
        String departmentName,

        @Schema(description = "Category of leave", example = "ANNUAL")
        LeaveType leaveType,

        @Schema(description = "Inclusive first day of the leave period", example = "2025-08-01")
        LocalDate startDate,

        @Schema(description = "Inclusive last day of the leave period", example = "2025-08-05")
        LocalDate endDate,

        @Schema(description = "Number of calendar days requested (inclusive)", example = "5")
        long totalDays,

        @Schema(description = "Optional reason or justification", example = "Family vacation")
        String reason,

        @Schema(description = "Current approval status", example = "PENDING")
        LeaveStatus status,

        @Schema(description = "Reason provided by the reviewer on rejection",
                example = "Insufficient leave balance")
        String rejectionReason,

        @Schema(description = "UUID of the reviewer who made the decision")
        UUID reviewedBy,

        @Schema(description = "Timestamp when the review decision was recorded")
        LocalDateTime reviewedAt,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Email of the principal who submitted this request, or SYSTEM",
                example = "employee@example.com")
        String createdBy,

        @Schema(description = "Email of the principal who last modified this request, or SYSTEM",
                example = "hr@example.com")
        String updatedBy
) {
}
