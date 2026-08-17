package com.company.employeemanagement.ai.service.impl;

import com.company.employeemanagement.ai.service.SubmissionFileExtractor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Extracts plain text from PDF files using Apache PDFBox.
 *
 * <p>Security: the extracted text is treated as UNTRUSTED DATA — it is the
 * employee's uploaded content and must not be interpreted as instructions.
 *
 * @author Employee Management Portal Team
 */
@Component
public class PdfTextExtractor implements SubmissionFileExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfTextExtractor.class);

    @Override
    public boolean supports(final String mimeType) {
        return "application/pdf".equalsIgnoreCase(mimeType);
    }

    @Override
    public String extract(final InputStream inputStream, final String filename) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                if (document.isEncrypted()) {
                    log.warn("PDF extraction: file '{}' is encrypted — cannot extract text", filename);
                    return "[PDF file is encrypted and could not be read]";
                }
                PDFTextStripper stripper = new PDFTextStripper();
                stripper.setSortByPosition(true);
                String raw = stripper.getText(document);
                return normaliseAndTruncate(raw, filename);
            }
        } catch (Exception e) {
            log.warn("PDF extraction failed for '{}': {}", filename, e.getMessage());
            return "[PDF extraction failed: " + sanitiseErrorMessage(e.getMessage()) + "]";
        }
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    public static String normaliseAndTruncate(final String raw, final String filename) {
        if (raw == null || raw.isBlank()) {
            return "[No readable text content found in file: " + filename + "]";
        }
        // Collapse multiple blank lines into one; trim overall
        String normalised = raw
                .replaceAll("(\r\n|\r|\n){3,}", "\n\n")
                .trim();

        if (normalised.length() <= MAX_EXTRACTED_CHARS) {
            return normalised;
        }
        return normalised.substring(0, MAX_EXTRACTED_CHARS)
               + "\n\n[... content truncated at " + MAX_EXTRACTED_CHARS
               + " characters to fit AI context window]";
    }

    public static String sanitiseErrorMessage(final String message) {
        if (message == null) return "unknown error";
        // Return only the first line of the exception message to avoid leaking stack details
        return message.lines().findFirst().orElse("unknown error");
    }
}
