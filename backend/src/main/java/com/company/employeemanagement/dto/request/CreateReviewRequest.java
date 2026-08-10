package com.company.employeemanagement.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for creating a new performance review.
 *
 * @param employeeId   UUID of the employee being reviewed
 * @param reviewPeriod human-readable label, e.g. "Q1 2025" or "Annual 2024"
 * @param rating       numeric rating 1–5
 * @param reviewDate   calendar date the review was conducted
 * @param comments     qualitative narrative feedback (optional)
 * @param goals        goals for next period (optional)
 *
 * @author Employee Management Portal Team
 */
@Schema(description = "Payload for creating a performance review")
public record CreateReviewRequest(

        @Schema(description = "UUID of the employee being reviewed",
                example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "Employee ID is required")
        UUID employeeId,

        @Schema(description = "Review period label", example = "Q1 2025")
        @NotBlank(message = "Review period is required")
        @Size(max = 50, message = "Review period must not exceed 50 characters")
        String reviewPeriod,

        @Schema(description = "Rating from 1 (Unsatisfactory) to 5 (Outstanding)", example = "4")
        @NotNull(message = "Rating is required")
        @Min(value = 1, message = "Rating must be at least 1")
        @Max(value = 5, message = "Rating must be at most 5")
        Integer rating,

        @Schema(description = "Date the review was conducted", example = "2025-03-31")
        @NotNull(message = "Review date is required")
        LocalDate reviewDate,

        @Schema(description = "Qualitative performance feedback", example = "Excellent work on the Q1 sprint.")
        @Size(max = 5000, message = "Comments must not exceed 5000 characters")
        String comments,

        @Schema(description = "Goals for the next review period",
                example = "Complete AWS certification by Q2.")
        @Size(max = 5000, message = "Goals must not exceed 5000 characters")
        String goals
) {
}
