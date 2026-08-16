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
 * A file attached to a {@link Task} by a manager, HR, or admin.
 *
 * <p>Files are stored via {@link com.company.employeemanagement.service.FileStorageService}
 * under the key format {@code tasks/{taskId}/{uuid}.{ext}}.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "task_attachments")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TaskAttachment extends BaseEntity {

    /**
     * The task this attachment belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "task_id", nullable = false)
    private Task task;

    /**
     * The employee who uploaded the file (may be null if the uploader was deleted).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploader_id")
    private Employee uploader;

    /**
     * Original filename as provided by the client browser.
     */
    @Column(name = "original_name", nullable = false, length = 255)
    private String originalName;

    /**
     * The filename stored on disk (UUID-based to avoid collisions).
     */
    @Column(name = "stored_name", nullable = false, length = 255)
    private String storedName;

    /**
     * MIME type of the file.
     */
    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    /**
     * File size in bytes.
     */
    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    /**
     * Storage key (path / S3 object key) returned by {@link com.company.employeemanagement.service.FileStorageService#store}.
     */
    @Column(name = "storage_key", nullable = false, length = 512)
    private String storageKey;
}
