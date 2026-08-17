package com.company.employeemanagement.entity;

import com.company.employeemanagement.entity.enums.AiRecommendedAction;
import com.company.employeemanagement.entity.enums.AiReviewStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an AI-powered analysis of a task submission.
 *
 * <p>A manager may request an AI review of any submission in PENDING_REVIEW state.
 * The result is stored here; the original employee submission is never modified.
 *
 * <p>Multiple AI reviews may exist for the same submission (e.g., after resubmissions
 * or if the manager requests a fresh analysis). At most one review should be in
 * PENDING or PROCESSING state per submission at any given time — this is enforced
 * at the service layer, not by a database constraint (MySQL does not support filtered
 * unique indexes).
 *
 * <p>The {@code structuredAnalysisJson} column stores the full structured JSON produced
 * by the AI, matching the Phase 7 analysis schema. It is not mapped to a Java object
 * on the entity itself; the service layer parses it as needed using Jackson.
 *
 * <p>Phase 7A: advisory only — AI recommendations must never automatically
 * approve or reject a submission.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "task_ai_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAiReview extends BaseEntity {

    /**
     * The task that owns the submission under review.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The submission being analysed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    private TaskSubmission submission;

    /**
     * The employee (manager/HR/admin) who requested the AI review.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private Employee requestedBy;

    /**
     * Lifecycle status of this AI review.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private AiReviewStatus status = AiReviewStatus.PENDING;

    /**
     * Name of the AI provider used (e.g., {@code "groq"}).
     */
    @Builder.Default
    @Column(name = "ai_provider", nullable = false, length = 50)
    private String aiProvider = "groq";

    /**
     * Model identifier used for this analysis (e.g., {@code "llama-3.1-8b-instant"}).
     */
    @Column(name = "ai_model", length = 100)
    private String aiModel;

    /**
     * Version of the prompt template used. Allows tracking prompt changes over time.
     */
    @Builder.Default
    @Column(name = "prompt_version", nullable = false, length = 50)
    private String promptVersion = "v1";

    /**
     * AI-assessed completion score (0–100). {@code null} while PENDING/PROCESSING.
     */
    @Column(name = "completion_score")
    private Integer completionScore;

    /**
     * AI-assessed quality score (0–100). {@code null} while PENDING/PROCESSING.
     */
    @Column(name = "quality_score")
    private Integer qualityScore;

    /**
     * AI's confidence in its analysis (0–100). {@code null} while PENDING/PROCESSING.
     */
    @Column(name = "confidence")
    private Integer confidence;

    /**
     * AI's advisory recommended action. Advisory only — manager decides.
     * {@code null} while PENDING/PROCESSING.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "recommended_action", length = 30)
    private AiRecommendedAction recommendedAction;

    /**
     * Full structured analysis JSON as returned by the AI.
     * Stored as raw JSON text; parsed by the service layer when needed.
     * {@code null} while PENDING/PROCESSING or on FAILED.
     */
    @Column(name = "structured_analysis_json", columnDefinition = "LONGTEXT")
    private String structuredAnalysisJson;

    /**
     * Short manager-oriented summary from the AI analysis.
     */
    @Column(name = "manager_summary", columnDefinition = "TEXT")
    private String managerSummary;

    /**
     * Error detail when {@link #status} is {@link AiReviewStatus#FAILED}.
     * {@code null} for non-failed reviews.
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * Timestamp when the AI analysis completed (COMPLETED or FAILED).
     * {@code null} while PENDING/PROCESSING.
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;
}
