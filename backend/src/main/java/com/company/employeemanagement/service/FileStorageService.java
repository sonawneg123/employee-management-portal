package com.company.employeemanagement.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * Storage abstraction for task submission file attachments.
 *
 * <p>The current implementation stores files on the local filesystem.
 * A future S3 implementation can be provided by creating a new bean that
 * implements this interface and activating it via a Spring {@code @Profile}
 * or a conditional property — the submission business logic does not need to change.
 *
 * <p>Storage key format (implementation detail, not a public contract):
 * <pre>
 *   submissions/{submissionId}/{uuid}.{ext}
 * </pre>
 *
 * <p>Phase 6C note: the {@link #openForRead(String)} method intentionally returns
 * a raw {@link InputStream} so that text-extraction pipelines can consume the file
 * without loading it entirely into memory.
 *
 * @author Employee Management Portal Team
 */
public interface FileStorageService {

    /**
     * Stores the given multipart file and returns the storage key assigned to it.
     *
     * @param file         the uploaded file
     * @param submissionId the UUID of the submission this file belongs to
     * @return the storage key (path/object key) that uniquely identifies the file
     * @throws java.io.IOException if the file cannot be written
     */
    String store(MultipartFile file, UUID submissionId) throws java.io.IOException;

    /**
     * Opens an {@link InputStream} for the file identified by the given storage key.
     *
     * <p>The caller is responsible for closing the stream.
     *
     * @param storageKey the storage key previously returned by {@link #store}
     * @return an {@link InputStream} over the file's bytes
     * @throws java.io.IOException if the file cannot be read
     */
    InputStream openForRead(String storageKey) throws java.io.IOException;

    /**
     * Deletes the file identified by the given storage key.
     *
     * <p>Implementations must not throw if the file does not exist (idempotent delete).
     *
     * @param storageKey the storage key of the file to delete
     */
    void delete(String storageKey);

    /**
     * Returns whether a file with the given storage key exists in the store.
     *
     * @param storageKey the storage key to check
     * @return {@code true} if the file exists
     */
    boolean exists(String storageKey);
}
