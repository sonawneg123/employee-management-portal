package com.company.employeemanagement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO representing a performance review as returned by the API.
 *
 * @param id             UUID primary key
 * @param employeeId     UUID of the reviewed employee
 * @param employeeCode   HR-assigned employee code
 * @param employeeName   full name of the reviewed employee (may be null if no linked user)
 * @param departmentName department of the reviewed employee
 * @param reviewerId     UUID of the reviewer (manager/HR), or {@code null}
 * @param reviewerName   display name of the reviewer, or {@code null}
 * @param reviewPeriod   human-readable label, e.g. "Q1 2025"
 * @param rating         numeric 1–5 rating
 * @param ratingLabel    text label for the rating (Unsatisfactory … Outstanding)
 * @param reviewDate     calendar date the review was conducted
 * @param comments       qualitative feedback narrative, or {@code null}
 * @param goals          goals set for the next period, or {@code null}
 * @param createdAt      record creation timestamp
 * @param updatedAt      record last-modified timestamp
 * @param createdBy      email of the principal who created the review
 * @param updatedBy      email of the principal who last modified the review
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Performance review as returned by the API")
public record ReviewResponse(

        @Schema(description = "UUID of the review")
        UUID id,

        @Schema(description = "UUID of the reviewed employee")
        UUID employeeId,

        @Schema(description = "Employee code", example = "EMP-0001")
        String employeeCode,

        @Schema(description = "Full name of the reviewed employee", example = "Jane Smith")
        String employeeName,

        @Schema(description = "Department of the reviewed employee", example = "Engineering")
        String departmentName,

        @Schema(description = "UUID of the reviewer")
        UUID reviewerId,

        @Schema(description = "Display name of the reviewer", example = "John Manager")
        String reviewerName,

        @Schema(description = "Review period label", example = "Q1 2025")
        String reviewPeriod,

        @Schema(description = "Numeric rating 1–5", example = "4")
        int rating,

        @Schema(description = "Text label for the rating", example = "Good")
        String ratingLabel,

        @Schema(description = "Date the review was conducted", example = "2025-03-31")
        LocalDate reviewDate,

        @Schema(description = "Qualitative performance feedback")
        String comments,

        @Schema(description = "Goals for the next review period")
        String goals,

        @Schema(description = "Record creation timestamp")
        LocalDateTime createdAt,

        @Schema(description = "Record last-modified timestamp")
        LocalDateTime updatedAt,

        @Schema(description = "Email of the principal who created the review")
        String createdBy,

        @Schema(description = "Email of the principal who last modified the review")
        String updatedBy
) {
}
