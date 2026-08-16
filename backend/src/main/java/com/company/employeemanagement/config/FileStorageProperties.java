package com.company.employeemanagement.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for the task-submission file storage back-end.
 *
 * <p>Bound from the {@code app.storage} prefix in {@code application.properties}.
 * The application currently uses a local filesystem provider.
 * Switching to S3 only requires adding a new {@link com.company.employeemanagement.service.FileStorageService}
 * implementation and changing {@code app.storage.provider=s3} — no submission
 * business logic needs to change.
 *
 * <p>Example configuration:
 * <pre>
 *   app.storage.provider=local
 *   app.storage.local.base-dir=/var/emp-portal/uploads/submissions
 *   app.storage.max-file-size-bytes=10485760
 * </pre>
 *
 * @author Employee Management Portal Team
 */
@ConfigurationProperties(prefix = "app.storage")
public class FileStorageProperties {

    /**
     * Storage provider: {@code local} (default) or {@code s3}.
     */
    private String provider = "local";

    /**
     * Maximum allowed upload size in bytes. Default: 10 MB.
     */
    private long maxFileSizeBytes = 10 * 1024 * 1024; // 10 MB

    /**
     * Local filesystem storage settings.
     */
    private Local local = new Local();

    // ── Accessors ─────────────────────────────────────────────────────────────

    public String getProvider() {
        return provider;
    }

    public void setProvider(final String provider) {
        this.provider = provider;
    }

    public long getMaxFileSizeBytes() {
        return maxFileSizeBytes;
    }

    public void setMaxFileSizeBytes(final long maxFileSizeBytes) {
        this.maxFileSizeBytes = maxFileSizeBytes;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(final Local local) {
        this.local = local;
    }

    // ── Nested types ──────────────────────────────────────────────────────────

    /**
     * Local filesystem storage configuration.
     */
    public static class Local {

        /**
         * Absolute path to the directory where uploaded files are stored.
         * The directory is created automatically if it does not exist.
         */
        private String baseDir = System.getProperty("user.home") + "/emp-portal/uploads/submissions";

        public String getBaseDir() {
            return baseDir;
        }

        public void setBaseDir(final String baseDir) {
            this.baseDir = baseDir;
        }
    }
}
