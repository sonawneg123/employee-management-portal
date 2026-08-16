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

/**
 * Immutable audit entry recording a state-change event on a {@link Task}.
 *
 * <p>Activity records are created automatically by the system.
 * Users cannot create, update, or delete them via the API.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "task_activities")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskActivity extends BaseEntity {

    /**
     * The task to which this activity belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The employee who performed the action (may be null for system events).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_id")
    private Employee actor;

    /**
     * Short event type string, e.g. "TASK_ASSIGNED", "TASK_STARTED", "TASK_STATUS_CHANGED".
     */
    @Column(name = "event_type", nullable = false, length = 50)
    private String eventType;

    /**
     * Human-readable description of the event.
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Previous status value (if applicable), stored as a plain string.
     */
    @Column(name = "from_status", length = 30)
    private String fromStatus;

    /**
     * New status value (if applicable), stored as a plain string.
     */
    @Column(name = "to_status", length = 30)
    private String toStatus;
}
