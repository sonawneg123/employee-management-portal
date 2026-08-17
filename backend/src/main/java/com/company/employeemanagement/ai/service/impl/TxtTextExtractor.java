package com.company.employeemanagement.ai.service.impl;

import com.company.employeemanagement.ai.service.SubmissionFileExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Extracts plain text from TXT files by reading them as UTF-8.
 *
 * <p>Security: the extracted text is treated as UNTRUSTED DATA.
 *
 * @author Employee Management Portal Team
 */
@Component
public class TxtTextExtractor implements SubmissionFileExtractor {

    private static final Logger log = LoggerFactory.getLogger(TxtTextExtractor.class);

    @Override
    public boolean supports(final String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.toLowerCase();
        return lower.startsWith("text/plain") || lower.equals("text/txt");
    }

    @Override
    public String extract(final InputStream inputStream, final String filename) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            String raw = new String(bytes, StandardCharsets.UTF_8);
            return PdfTextExtractor.normaliseAndTruncate(raw, filename);
        } catch (Exception e) {
            log.warn("TXT extraction failed for '{}': {}", filename, e.getMessage());
            return "[TXT extraction failed: " + PdfTextExtractor.sanitiseErrorMessage(e.getMessage()) + "]";
        }
    }
}
