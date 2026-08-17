package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.service.impl.PdfTextExtractor;
import com.company.employeemanagement.service.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

/**
 * Coordinates file text extraction for submission attachments.
 *
 * <p>Selects the appropriate {@link SubmissionFileExtractor} based on the
 * attachment's MIME type and delegates extraction to it. Falls back to a
 * descriptive message when no extractor supports the type.
 *
 * <p>Uses {@link FileStorageService} to open the stored file.
 *
 * <p>Security: all extracted text is UNTRUSTED DATA from the employee.
 * The AI prompt builder is responsible for correctly labelling it.
 *
 * @author Employee Management Portal Team
 */
@Service
public class SubmissionAttachmentExtractionService {

    private static final Logger log = LoggerFactory.getLogger(SubmissionAttachmentExtractionService.class);

    private final FileStorageService fileStorageService;
    private final List<SubmissionFileExtractor> extractors;

    /**
     * Constructs the service.
     *
     * @param fileStorageService the file storage abstraction
     * @param extractors         all registered {@link SubmissionFileExtractor} beans
     */
    public SubmissionAttachmentExtractionService(
            final FileStorageService fileStorageService,
            final List<SubmissionFileExtractor> extractors) {
        this.fileStorageService = fileStorageService;
        this.extractors = extractors;
        log.info("SubmissionAttachmentExtractionService initialised with {} extractor(s): {}",
                extractors.size(),
                extractors.stream()
                        .map(e -> e.getClass().getSimpleName())
                        .toList());
    }

    /**
     * Extracts text from the file identified by {@code storageKey}.
     *
     * @param storageKey   the file storage key
     * @param mimeType     the MIME type of the attachment
     * @param originalName the original filename (for logging and notices)
     * @return extracted plain text, truncated to the extractor's limit
     */
    public String extractText(final String storageKey,
                              final String mimeType,
                              final String originalName) {
        if (storageKey == null || storageKey.isBlank()) {
            return "[No attachment]";
        }

        // Find extractor — normalize MIME (strip parameters like charset)
        String normalisedMime = mimeType == null ? "" : mimeType.split(";")[0].trim().toLowerCase();
        SubmissionFileExtractor extractor = extractors.stream()
                .filter(e -> e.supports(normalisedMime))
                .findFirst()
                .orElse(null);

        if (extractor == null) {
            log.warn("No extractor available for MIME type '{}' (file: '{}')", normalisedMime, originalName);
            return "[Attachment type '" + normalisedMime + "' is not supported for text extraction]";
        }

        log.debug("Extracting text from '{}' using {} (MIME: {})",
                originalName, extractor.getClass().getSimpleName(), normalisedMime);

        try (InputStream stream = fileStorageService.openForRead(storageKey)) {
            String extracted = extractor.extract(stream, originalName);
            log.debug("Extracted {} chars from '{}'", extracted.length(), originalName);
            return extracted;
        } catch (Exception e) {
            log.warn("Failed to open file '{}' (key: {}): {}", originalName, storageKey, e.getMessage());
            return "[Could not read attachment '" + originalName + "': "
                   + PdfTextExtractor.sanitiseErrorMessage(e.getMessage()) + "]";
        }
    }
}
