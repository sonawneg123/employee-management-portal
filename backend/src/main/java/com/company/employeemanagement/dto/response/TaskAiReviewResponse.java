package com.company.employeemanagement.dto.response;

import com.company.employeemanagement.entity.enums.AiRecommendedAction;
import com.company.employeemanagement.entity.enums.AiReviewStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for a {@link com.company.employeemanagement.entity.TaskAiReview}.
 *
 * <p>The {@code structuredAnalysisJson} is returned as a raw String so that
 * the frontend can parse it as needed. We do not map it to a nested object
 * here to avoid coupling the response shape to the internal analysis record.
 *
 * @author Employee Management Portal Team
 */
public record TaskAiReviewResponse(

        UUID id,
        UUID taskId,
        UUID submissionId,
        UUID requestedById,
        String requestedByName,

        AiReviewStatus status,
        String aiProvider,
        String aiModel,
        String promptVersion,

        Integer completionScore,
        Integer qualityScore,
        Integer confidence,
        AiRecommendedAction recommendedAction,

        /** Full structured JSON from the AI — parse client-side. */
        String structuredAnalysisJson,
        String managerSummary,
        String errorMessage,

        LocalDateTime createdAt,
        LocalDateTime completedAt

) {}
