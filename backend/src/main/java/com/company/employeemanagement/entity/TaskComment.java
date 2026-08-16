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
 * A comment posted by an employee or manager on a {@link Task}.
 *
 * <p>Authorization rules:
 * <ul>
 *   <li>Employees may only comment on tasks assigned to them.</li>
 *   <li>Managers / HR / Admin may comment on any task they can access.</li>
 *   <li>When a comment is added, the other party on the task is notified
 *       (i.e., if the author is the assignee, the creator is notified, and vice versa).</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "task_comments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskComment extends BaseEntity {

    /**
     * The task this comment belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The employee who authored this comment.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private Employee author;

    /**
     * The text body of the comment.
     */
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    /**
     * Whether this comment has been edited after original posting.
     */
    @Builder.Default
    @Column(name = "edited", nullable = false)
    private boolean edited = false;
}
