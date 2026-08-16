package com.company.employeemanagement.entity;

import com.company.employeemanagement.entity.enums.NotificationType;
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
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

/**
 * In-app notification for an {@link Employee}.
 *
 * <p>Notifications are created by the system (never directly by end users).
 * A recipient can mark notifications as read; they can never be modified otherwise.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification extends BaseEntity {

    /**
     * The employee who should receive this notification.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "recipient_id", nullable = false)
    private Employee recipient;

    /**
     * Notification type — determines the icon and behaviour in the UI.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 40)
    private NotificationType type;

    /**
     * Short one-line title, e.g. "New Task Assigned".
     */
    @Column(name = "title", nullable = false, length = 255)
    private String title;

    /**
     * Full human-readable description of the notification event.
     */
    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * UUID of the related task, if any.  Allows the frontend to build a
     * navigation link to the task detail page.
     *
     * <p>{@code @JdbcTypeCode(SqlTypes.CHAR)} forces Hibernate to map this
     * UUID field as {@code CHAR(36)} — matching the project-wide convention
     * established in {@link BaseEntity} and every Flyway migration.
     */
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "related_task_id", length = 36)
    private UUID relatedTaskId;

    /**
     * Whether the recipient has marked this notification as read.
     * Defaults to {@code false}.
     */
    @Builder.Default
    @Column(name = "is_read", nullable = false)
    private boolean read = false;
}
