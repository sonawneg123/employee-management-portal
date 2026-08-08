package com.company.employeemanagement.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents a formal performance review conducted for an {@link Employee}.
 *
 * <p>Reviews are typically conducted quarterly or annually. Each review
 * captures a numeric rating (1–5), written comments, and goals for the
 * next period.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "performance_reviews")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceReview extends BaseEntity {

    /**
     * The employee being reviewed.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * UUID of the manager or HR user conducting the review.
     * Stored as a bare UUID reference to avoid a hard FK dependency
     * on the users table from this table.
     */
    @Column(name = "reviewer_id")
    private UUID reviewerId;

    /**
     * Human-readable review period label (e.g., "Q1 2025", "Annual 2024").
     */
    @Column(name = "review_period", nullable = false, length = 50)
    private String reviewPeriod;

    /**
     * Numeric performance rating on a 1–5 scale, where
     * 1 = Unsatisfactory and 5 = Outstanding.
     * Enforced by a database CHECK constraint.
     */
    @Column(name = "rating", nullable = false)
    private int rating;

    /**
     * Qualitative narrative comments on the employee's performance.
     */
    @Column(name = "comments", columnDefinition = "TEXT")
    private String comments;

    /**
     * Goals and objectives set for the next review period.
     */
    @Column(name = "goals", columnDefinition = "TEXT")
    private String goals;

    /**
     * The calendar date on which the review was conducted.
     */
    @Column(name = "review_date", nullable = false)
    private LocalDate reviewDate;
}
