package com.company.employeemanagement.ai.service.impl;

import com.company.employeemanagement.ai.service.SubmissionFileExtractor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Extracts content from CSV files as structured plain text.
 *
 * <p>The CSV is formatted as a simple table (header + rows) so the AI
 * can interpret it without parsing raw delimiters.
 * Up to {@value #MAX_CSV_ROWS} rows are read to prevent token exhaustion.
 *
 * <p>Security: the extracted text is treated as UNTRUSTED DATA.
 *
 * @author Employee Management Portal Team
 */
@Component
public class CsvTextExtractor implements SubmissionFileExtractor {

    private static final Logger log = LoggerFactory.getLogger(CsvTextExtractor.class);

    /** Maximum rows to read from a CSV file (excluding header). */
    public static final int MAX_CSV_ROWS = 200;

    @Override
    public boolean supports(final String mimeType) {
        if (mimeType == null) return false;
        String lower = mimeType.toLowerCase();
        return lower.startsWith("text/csv")
               || lower.startsWith("application/csv")
               || lower.startsWith("application/vnd.ms-excel");
    }

    @Override
    public String extract(final InputStream inputStream, final String filename) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<String> lines = new ArrayList<>();
            String line;
            int rowCount = 0;
            boolean truncated = false;

            while ((line = reader.readLine()) != null) {
                if (rowCount > MAX_CSV_ROWS) {
                    truncated = true;
                    break;
                }
                lines.add(line);
                rowCount++;
            }

            if (lines.isEmpty()) {
                return "[CSV file is empty: " + filename + "]";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("CSV data from file: ").append(filename).append("\n");
            sb.append("Columns: ").append(lines.get(0)).append("\n\n");

            if (lines.size() > 1) {
                sb.append("Data rows:\n");
                for (int i = 1; i < lines.size(); i++) {
                    sb.append(lines.get(i)).append("\n");
                }
            }

            if (truncated) {
                sb.append("\n[... CSV truncated after ").append(MAX_CSV_ROWS).append(" rows]");
            }

            return PdfTextExtractor.normaliseAndTruncate(sb.toString(), filename);

        } catch (Exception e) {
            log.warn("CSV extraction failed for '{}': {}", filename, e.getMessage());
            return "[CSV extraction failed: " + PdfTextExtractor.sanitiseErrorMessage(e.getMessage()) + "]";
        }
    }
}
