package com.company.employeemanagement.entity;

import com.company.employeemanagement.entity.enums.TaskCategory;
import com.company.employeemanagement.entity.enums.TaskPriority;
import com.company.employeemanagement.entity.enums.TaskStatus;
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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Represents a company work task that can be created by managers and
 * assigned to employees.
 *
 * <p>Relationships:
 * <ul>
 *   <li>{@link Employee} ({@code assignedEmployee}) — the employee responsible
 *       for completing the task; may be {@code null} for DRAFT tasks.</li>
 *   <li>{@link Employee} ({@code createdByEmployee}) — the manager or privileged
 *       user who created the task; resolved from the authenticated principal.</li>
 * </ul>
 *
 * <p>The {@code overdue} state is derived, not persisted: a task is overdue
 * when its status is not {@link TaskStatus#COMPLETED} and its {@code dueDate}
 * is before the current date.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "tasks")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task extends BaseEntity {

    /**
     * Short descriptive title for the task.
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Detailed description of the work to be performed.
     */
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /**
     * Step-by-step guidelines the assignee should follow.
     */
    @Column(name = "guidelines", columnDefinition = "TEXT")
    private String guidelines;

    /**
     * Criteria that must be met for the task to be considered complete.
     */
    @Column(name = "acceptance_criteria", columnDefinition = "TEXT")
    private String acceptanceCriteria;

    /**
     * The employee assigned to complete this task. May be {@code null}
     * while the task is in {@link TaskStatus#DRAFT} state.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_employee_id")
    private Employee assignedEmployee;

    /**
     * The employee who created this task (typically a manager).
     * Resolved at creation time from the authenticated principal's
     * linked employee record. May be {@code null} for system-created tasks.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_employee_id")
    private Employee createdByEmployee;

    /**
     * Importance / urgency level of the task.
     * Defaults to {@link TaskPriority#MEDIUM}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false, length = 20)
    private TaskPriority priority = TaskPriority.MEDIUM;

    /**
     * Current lifecycle state of the task.
     * Defaults to {@link TaskStatus#DRAFT}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TaskStatus status = TaskStatus.DRAFT;

    /**
     * The date by which the task should be completed.
     */
    @Column(name = "due_date")
    private LocalDate dueDate;

    /**
     * Estimated number of hours required to complete the task.
     */
    @Column(name = "estimated_hours", precision = 6, scale = 2)
    private BigDecimal estimatedHours;

    /**
     * Structured task category. Stored as a VARCHAR(30) using the enum name.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "category", length = 30)
    private TaskCategory category;

    // ── Reminder deduplication flags (set by TaskDeadlineReminderService) ─────

    /**
     * {@code true} once the 24-hour-before-due-date reminder notification has been sent.
     */
    @Builder.Default
    @Column(name = "reminder_24h_sent", nullable = false)
    private boolean reminder24hSent = false;

    /**
     * {@code true} once the 2-hour-before-due-date reminder notification has been sent.
     */
    @Builder.Default
    @Column(name = "reminder_2h_sent", nullable = false)
    private boolean reminder2hSent = false;

    /**
     * {@code true} once the overdue notification has been sent for this task.
     */
    @Builder.Default
    @Column(name = "overdue_notification_sent", nullable = false)
    private boolean overdueNotificationSent = false;
}
