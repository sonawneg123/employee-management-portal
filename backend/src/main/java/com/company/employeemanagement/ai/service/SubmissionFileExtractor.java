package com.company.employeemanagement.ai.service;

import java.io.InputStream;

/**
 * Extracts plain text from uploaded submission attachments.
 *
 * <p>Implementations must handle each supported file type safely,
 * enforcing size limits and truncation to prevent oversized prompts.
 *
 * <p>Phase 7A supported types: PDF, DOCX, TXT, CSV.
 * Image OCR is intentionally out of scope.
 *
 * <p>Security rules that all implementations must enforce:
 * <ul>
 *   <li>Extracted text is UNTRUSTED DATA. It will be clearly marked in the prompt.</li>
 *   <li>Text is truncated to {@link #MAX_EXTRACTED_CHARS} to prevent token exhaustion.</li>
 *   <li>Extraction failures (corrupt file, unsupported encoding, etc.) return a
 *       human-readable error string rather than throwing, so the AI analysis can
 *       proceed with a note that the file could not be read.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public interface SubmissionFileExtractor {

    /**
     * Maximum number of characters to extract from a single file.
     * Prevents oversized AI prompts and token-limit errors.
     */
    int MAX_EXTRACTED_CHARS = 6_000;

    /**
     * Returns whether this extractor supports the given MIME type.
     *
     * @param mimeType the MIME type (e.g., {@code "application/pdf"})
     * @return {@code true} if this extractor handles the given MIME type
     */
    boolean supports(String mimeType);

    /**
     * Extracts and normalises plain text from the given input stream.
     *
     * <p>The returned text is:
     * <ul>
     *   <li>Trimmed of leading/trailing whitespace.</li>
     *   <li>Normalised (multiple consecutive blank lines collapsed to one).</li>
     *   <li>Truncated to {@link #MAX_EXTRACTED_CHARS} characters with a truncation notice appended.</li>
     * </ul>
     *
     * <p>On extraction failure the method returns a message describing the failure
     * rather than throwing, allowing the AI to note the failure in its analysis.
     *
     * @param inputStream the file content stream; the caller retains ownership and closes it
     * @param filename    the original filename (used for logging and truncation notices)
     * @return extracted text, or an error description if extraction failed
     */
    String extract(InputStream inputStream, String filename);
}
