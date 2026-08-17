package com.company.employeemanagement.service;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Validates profile photo uploads against allowed image types and size limits.
 *
 * <p>Allowed extensions: {@code jpg}, {@code jpeg}, {@code png}, {@code webp}.
 * Max size: 5 MB.
 *
 * <p>Validation never trusts the client-supplied {@code Content-Type} alone; the file
 * extension is cross-checked against the declared MIME type to prevent spoofing.
 *
 * @author Employee Management Portal Team
 */
@Component
public class ProfilePhotoValidationService {

    /** Allowed image file extensions (lower-case, without dot). */
    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp");

    /**
     * Maps each allowed extension to the MIME types that are acceptable for it.
     */
    private static final Map<String, List<String>> EXTENSION_TO_MIME = Map.of(
            "jpg",  List.of("image/jpeg"),
            "jpeg", List.of("image/jpeg"),
            "png",  List.of("image/png"),
            "webp", List.of("image/webp")
    );

    /** Maximum allowed upload size: 5 MB. */
    private static final long MAX_SIZE_BYTES = 5L * 1024 * 1024;

    /**
     * Validates the given profile photo file.
     *
     * @param file the uploaded file
     * @throws IllegalArgumentException if the file fails any validation check
     */
    public void validate(final MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Profile photo is empty.");
        }

        // ── Extension check ───────────────────────────────────────────────────
        String originalName = file.getOriginalFilename();
        String ext = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new IllegalArgumentException(
                    "Unsupported image type: '." + ext + "'. Allowed: JPG, JPEG, PNG, WEBP.");
        }

        // ── MIME type check ───────────────────────────────────────────────────
        String contentType = file.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new IllegalArgumentException("Missing content type.");
        }
        String normMime = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
        List<String> allowedMimes = EXTENSION_TO_MIME.get(ext);
        if (!allowedMimes.contains(normMime)) {
            throw new IllegalArgumentException(
                    "MIME type '" + normMime + "' is not permitted for ." + ext + " files.");
        }

        // ── Size check ────────────────────────────────────────────────────────
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("Image size exceeds 5 MB limit.");
        }
    }

    // ── Package-private helper (accessible from tests) ────────────────────────

    /**
     * Extracts the lower-case extension (without dot) from a filename.
     * Returns an empty string if no extension is present.
     */
    static String extractExtension(final String filename) {
        if (filename == null || filename.isBlank()) return "";
        String name = java.nio.file.Paths.get(filename).getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";
    }
}
