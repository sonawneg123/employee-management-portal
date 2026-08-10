package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for updating an existing performance review.
 *
 * @param reviewPeriod updated review period label
 * @param rating       updated numeric rating 1–5
 * @param reviewDate   updated review date
 * @param comments     updated qualitative feedback (optional)
 * @param goals        updated goals for next period (optional)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for updating a performance review")
public record UpdateReviewRequest(

        @Schema(description = "Review period label", example = "Q2 2025")
        @NotBlank(message = "Review period is required")
        @Size(max = 50, message = "Review period must not exceed 50 characters")
        String reviewPeriod,

        @Schema(description = "Rating from 1 (Unsatisfactory) to 5 (Outstanding)", example = "5")
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @Schema(description = "Date the review was conducted", example = "2025-06-30")
        @NotNull(message = "Review date is required")
        LocalDate reviewDate,

        @Schema(description = "Qualitative performance feedback")
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        String comments,

        @Schema(description = "Goals for the next review period")
        @Size(max = 5000, message = "Goals must not exceed 5000 characters")
        String goals
) {
}
