package com.company.employeemanagement.ai.service.impl;

import com.company.employeemanagement.ai.service.SubmissionFileExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * Extracts plain text from DOCX files using Apache POI.
 *
 * <p>Security: the extracted text is treated as UNTRUSTED DATA.
 *
 * @author Employee Management Portal Team
 */
@Component
public class DocxTextExtractor implements SubmissionFileExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocxTextExtractor.class);

    @Override
    public boolean supports(final String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.toLowerCase();
        return lower.contains("wordprocessingml")
               || lower.contains("docx")
               || lower.equals("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
    }

    @Override
    public String extract(final InputStream inputStream, final String filename) {
        try (XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            String raw = extractor.getText();
            return PdfTextExtractor.normaliseAndTruncate(raw, filename);
        } catch (Exception e) {
            log.warn("DOCX extraction failed for '{}': {}", filename, e.getMessage());
            return "[DOCX extraction failed: " + PdfTextExtractor.sanitiseErrorMessage(e.getMessage()) + "]";
        }
    }
}
