package com.company.employeemanagement.service;

import com.company.employeemanagement.config.FileStorageProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Validates uploaded files against the allowed types and size limits.
 *
 * <p>Allowed types:
 * <ul>
 *   <li>PDF — {@code .pdf} / {@code application/pdf}</li>
 *   <li>CSV — {@code .csv} / {@code text/csv} or {@code text/plain}</li>
 *   <li>DOCX — {@code .docx} / {@code application/vnd.openxmlformats-officedocument.wordprocessingml.document}</li>
 *   <li>TXT — {@code .txt} / {@code text/plain}</li>
 * </ul>
 *
 * <p>Validation never trusts the client-supplied {@code Content-Type}; the file
 * extension is checked independently against the declared MIME type.
 *
 * @author Employee Management Portal Team
 */
@Component
public class FileValidationService {

    /** Allowed file extensions (lower-case, without dot). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("pdf", "csv", "docx", "txt");

    /**
     * Maps each allowed extension to the MIME types that are acceptable for it.
     * CSV may arrive as either {@code text/csv} or {@code text/plain}.
     * TXT arrives as {@code text/plain}.
     */
    private static final java.util.Map<String, List<String>> EXTENSION_TO_MIME = java.util.Map.of(
            "pdf",  Arrays.asList("application/pdf"),
            "csv",  Arrays.asList("text/csv", "text/plain", "application/csv",
                                  "application/vnd.ms-excel"),
            "docx", Arrays.asList(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    "application/octet-stream"),          // some browsers send this for docx
            "txt",  Arrays.asList("text/plain", "application/octet-stream")
    );

    private final FileStorageProperties properties;

    public FileValidationService(final FileStorageProperties properties) {
        this.properties = properties;
    }

    /**
     * Validates the given file.
     *
     * @param file the uploaded file
     * @throws IllegalArgumentException if the file fails any validation check
     */
    public void validate(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file is empty.");
        }

        // ── Extension check ───────────────────────────────────────────────────
        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Unsupported file type: '." + ext + "'. Allowed types: PDF, CSV, DOCX, TXT.");
        }

        // ── MIME type check ───────────────────────────────────────────────────
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Missing file content type.");
        }
        String normalisedMime = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);

        List<String> allowedMimes = EXTENSION_TO_MIME.get(ext);
        if (!allowedMimes.contains(normalisedMime)) {
            throw new IllegalArgumentException(
                    "MIME type '" + normalisedMime + "' is not permitted for ." + ext + " files.");
        }

        // ── Size check ────────────────────────────────────────────────────────
        long maxBytes = properties.getMaxFileSizeBytes();
        if (file.getSize() > maxBytes) {
            throw new IllegalArgumentException(
                    "File size " + file.getSize() + " bytes exceeds the maximum allowed size of "
                    + maxBytes + " bytes (" + (maxBytes / 1024 / 1024) + " MB).");
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Extracts the lower-case extension from a filename, without the leading dot.
     * Returns an empty string if the filename has no extension.
     */
    static String extractExtension(final String filename) {
        if (filename == null || filename.isBlank()) return "";
        String name = Paths.get(filename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }

    /** @return the set of allowed file extensions (without dot). */
    public static Set<String> getAllowedExtensions() {
        return ALLOWED_EXTENSIONS;
    }
}
