package com.company.employeemanagement.service.impl;

import com.company.employeemanagement.config.FileStorageProperties;
import com.company.employeemanagement.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

/**
 * Local-filesystem implementation of {@link FileStorageService}.
 *
 * <p>Files are stored under:
 * <pre>
 *   {baseDir}/submissions/{submissionId}/{uuid}.{ext}
 * </pre>
 *
 * <p>To switch to S3 later:
 * <ol>
 *   <li>Create an {@code S3FileStorageService} that implements {@link FileStorageService}.</li>
 *   <li>Annotate this class with {@code @ConditionalOnProperty(name="app.storage.provider", havingValue="local")}.</li>
 *   <li>Annotate the S3 class with {@code @ConditionalOnProperty(name="app.storage.provider", havingValue="s3")}.</li>
 *   <li>The storage key format is already designed to double as an S3 object key.</li>
 * </ol>
 *
 * @author Employee Management Portal Team
 */
@Service
public class LocalFileStorageService implements FileStorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageService.class);

    private final FileStorageProperties properties;
    private Path baseDir;

    public LocalFileStorageService(final FileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Creates the base upload directory on startup if it does not already exist.
     */
    @PostConstruct
    public void init() {
        baseDir = Paths.get(properties.getLocal().getBaseDir()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(baseDir);
            log.info("FileStorage.init: base directory ready at {}", baseDir);
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Could not create file storage directory: " + baseDir, e);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a storage key in the format {@code submissions/{submissionId}/{uuid}.{ext}}
     * so that the key can later be used verbatim as an S3 object key.
     */
    @Override
    public String store(final MultipartFile file, final UUID submissionId) throws IOException {
        String storedName = UUID.randomUUID().toString() + extractExtension(file.getOriginalFilename());
        String storageKey = "submissions/" + submissionId + "/" + storedName;

        Path target = resolveAndValidate(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("FileStorage.store: key={} size={}", storageKey, file.getSize());
        return storageKey;
    }

    /** {@inheritDoc} */
    @Override
    public InputStream openForRead(final String storageKey) throws IOException {
        Path target = resolveAndValidate(storageKey);
        if (!Files.exists(target)) {
            throw new IOException("Attachment not found: " + storageKey);
        }
        return Files.newInputStream(target);
    }

    /** {@inheritDoc} */
    @Override
    public void delete(final String storageKey) {
        try {
            Path target = resolveAndValidate(storageKey);
            Files.deleteIfExists(target);
            log.debug("FileStorage.delete: key={}", storageKey);
        } catch (IOException e) {
            log.warn("FileStorage.delete: failed for key={}: {}", storageKey, e.getMessage());
        }
    }

    /** {@inheritDoc} */
    @Override
    public boolean exists(final String storageKey) {
        try {
            return Files.exists(resolveAndValidate(storageKey));
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p>Generates a storage key in the format {@code profiles/{employeeId}/{uuid}.{ext}}.
     */
    @Override
    public String storeProfilePhoto(final MultipartFile file, final UUID employeeId) throws IOException {
        String storedName = UUID.randomUUID().toString() + extractExtension(file.getOriginalFilename());
        String storageKey = "profiles/" + employeeId + "/" + storedName;

        Path target = resolveAndValidate(storageKey);
        Files.createDirectories(target.getParent());
        Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

        log.info("FileStorage.storeProfilePhoto: key={} size={}", storageKey, file.getSize());
        return storageKey;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Resolves the storage key to an absolute path under the base directory,
     * preventing path traversal by ensuring the result stays within baseDir.
     */
    private Path resolveAndValidate(final String storageKey) throws IOException {
        Path resolved = baseDir.resolve(storageKey).normalize();
        if (!resolved.startsWith(baseDir)) {
            throw new IOException("Path traversal detected for key: " + storageKey);
        }
        return resolved;
    }

    /**
     * Extracts the lowercase extension (including the leading dot) from a filename,
     * returning an empty string if no extension is present.
     */
    private static String extractExtension(final String filename) {
        if (filename == null || filename.isBlank()) return "";
        int dot = filename.lastIndexOf('.');
        return dot >= 0 ? filename.substring(dot).toLowerCase() : "";
    }
}
