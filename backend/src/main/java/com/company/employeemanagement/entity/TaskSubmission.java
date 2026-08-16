package com.company.employeemanagement.entity;

import com.company.employeemanagement.entity.enums.SubmissionStatus;
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
 * Represents an employee's submission of completed work for manager review.
 *
 * <p>A submission is created when an employee submits an {@code IN_PROGRESS} task
 * for review. The manager then either approves it (transitioning the task to
 * {@link com.company.employeemanagement.entity.enums.TaskStatus#COMPLETED}) or
 * requests changes (reverting the task to {@code IN_PROGRESS} so the employee
 * can revise and resubmit).
 *
 * <p>A task may have multiple submissions over its lifecycle (one per round of
 * review). The latest submission is the one that is currently active.
 *
 * <p>Phase 6B.1: optional file attachment support added.
 * Attachment metadata (original name, stored name, MIME type, size, etc.) is
 * stored in this row. The binary file itself lives in the file storage back-end
 * (local filesystem or S3) and is referenced by {@code attachmentStorageKey}.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "task_submissions")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskSubmission extends BaseEntity {

    /**
     * The task for which work has been submitted.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The employee who submitted the work.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submitted_by_id", nullable = false)
    private Employee submittedBy;

    /**
     * Summary notes from the employee describing what was done.
     */
    @Column(name = "submission_notes", columnDefinition = "TEXT")
    private String submissionNotes;

    /**
     * Detailed description of the work that was completed.
     */
    @Column(name = "work_completed", columnDefinition = "TEXT")
    private String workCompleted;

    /**
     * Optional additional comments from the employee (e.g. known issues, edge cases).
     */
    @Column(name = "additional_comments", columnDefinition = "TEXT")
    private String additionalComments;

    /**
     * Timestamp when the submission was created (initial submit or resubmit).
     * Explicitly tracked because {@code BaseEntity.createdAt} is set once
     * and never changes, while this field is updated on resubmit.
     */
    @Builder.Default
    @Column(name = "submitted_at", nullable = false)
    private LocalDateTime submittedAt = LocalDateTime.now();

    /**
     * Current review/approval status of this submission.
     * Defaults to {@link SubmissionStatus#PENDING_REVIEW}.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "review_status", nullable = false, length = 30)
    private SubmissionStatus reviewStatus = SubmissionStatus.PENDING_REVIEW;

    /**
     * Manager's review comment, explaining what needs to change or confirming approval.
     * Required when review status is {@link SubmissionStatus#CHANGES_REQUESTED}.
     */
    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    /**
     * The manager (employee) who reviewed this submission.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_id")
    private Employee reviewedBy;

    /**
     * Timestamp when the manager reviewed this submission.
     */
    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    // ── Attachment metadata (Phase 6B.1) ──────────────────────────────────────

    /**
     * Original filename as supplied by the browser (sanitised to basename only).
     * Null when no attachment was uploaded.
     */
    @Column(name = "attachment_original_name", length = 255)
    private String attachmentOriginalName;

    /**
     * Server-generated UUID-based filename used for storage.
     * Null when no attachment was uploaded.
     */
    @Column(name = "attachment_stored_name", length = 255)
    private String attachmentStoredName;

    /**
     * Validated MIME type of the attachment.
     * Null when no attachment was uploaded.
     */
    @Column(name = "attachment_mime_type", length = 100)
    private String attachmentMimeType;

    /**
     * File size in bytes.
     * Null when no attachment was uploaded.
     */
    @Column(name = "attachment_size_bytes")
    private Long attachmentSizeBytes;

    /**
     * Timestamp when the attachment was stored.
     * Null when no attachment was uploaded.
     */
    @Column(name = "attachment_uploaded_at")
    private LocalDateTime attachmentUploadedAt;

    /**
     * Logical storage path/key (filesystem path relative to base-dir, or S3 object key).
     * Used by {@link com.company.employeemanagement.service.FileStorageService} to retrieve or
     * delete the file. Null when no attachment was uploaded.
     */
    @Column(name = "attachment_storage_key", length = 512)
    private String attachmentStorageKey;

    /**
     * Returns {@code true} if this submission has a file attachment.
     */
    public boolean hasAttachment() {
        return attachmentStorageKey != null && !attachmentStorageKey.isBlank();
    }
}
