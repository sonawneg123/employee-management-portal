package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.service.impl.CsvTextExtractor;
import com.company.employeemanagement.ai.service.impl.DocxTextExtractor;
import com.company.employeemanagement.ai.service.impl.TxtTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the file text extractor implementations.
 *
 * <p>PDF extraction tests require PDFBox (tested via integration); here we
 * focus on TXT, CSV, and DOCX support detection and CSV logic.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("File Text Extractors")
class FileTextExtractorTest {

    // ── TxtTextExtractor ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("TxtTextExtractor")
    class TxtExtractorTests {

        private final TxtTextExtractor extractor = new TxtTextExtractor();

        @Test
        @DisplayName("supports text/plain MIME type")
        void supportsTextPlain() {
            assertThat(extractor.supports("text/plain")).isTrue();
        }
        @Test
        @DisplayName("supports text/plain with charset parameter (startsWith match)")
        void supportsTextPlainWithCharset() {
            // The TxtTextExtractor uses startsWith("text/plain") so it matches with charset
            assertThat(extractor.supports("text/plain; charset=UTF-8")).isTrue();
        }


        @Test
        @DisplayName("does not support application/pdf")
        void rejectsPdf() {
            assertThat(extractor.supports("application/pdf")).isFalse();
        }

        @Test
        @DisplayName("extracts UTF-8 text correctly")
        void extractsUtf8Text() {
            String content = "Hello, World!\nThis is a test submission.";
            InputStream stream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));
            String result = extractor.extract(stream, "test.txt");
            assertThat(result).contains("Hello, World!");
            assertThat(result).contains("This is a test submission.");
        }

        @Test
        @DisplayName("returns extraction failure notice on corrupt stream")
        void handlesCorruptStream() {
            // A stream that throws on read
            InputStream badStream = new InputStream() {
                @Override public int read() throws java.io.IOException {
                    throw new java.io.IOException("Simulated read failure");
                }
            };
            String result = extractor.extract(badStream, "bad.txt");
            assertThat(result).contains("TXT extraction failed");
        }
    }

    // ── CsvTextExtractor ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("CsvTextExtractor")
    class CsvExtractorTests {

        private final CsvTextExtractor extractor = new CsvTextExtractor();

        @Test
        @DisplayName("supports text/csv MIME type")
        void supportsTextCsv() {
            assertThat(extractor.supports("text/csv")).isTrue();
        }

        @Test
        @DisplayName("supports application/csv MIME type")
        void supportsApplicationCsv() {
            assertThat(extractor.supports("application/csv")).isTrue();
        }

        @Test
        @DisplayName("does not support text/plain (to avoid confusion with TXT)")
        void rejectsTextPlain() {
            assertThat(extractor.supports("text/plain")).isFalse();
        }

        @Test
        @DisplayName("extracts header and data rows")
        void extractsHeaderAndRows() {
            String csv = "Name,Hours,Status\nAlice,8,DONE\nBob,4,PENDING\n";
            InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));
            String result = extractor.extract(stream, "report.csv");
            assertThat(result).contains("Columns: Name,Hours,Status");
            assertThat(result).contains("Alice,8,DONE");
            assertThat(result).contains("Bob,4,PENDING");
        }

        @Test
        @DisplayName("returns empty notice for blank CSV")
        void emptyFileNotice() {
            InputStream stream = new ByteArrayInputStream(new byte[0]);
            String result = extractor.extract(stream, "empty.csv");
            assertThat(result).contains("empty");
        }

        @Test
        @DisplayName("truncates CSV at MAX_CSV_ROWS + 1")
        void truncatesAtMaxRows() {
            StringBuilder sb = new StringBuilder("col1,col2\n");
            for (int i = 0; i <= CsvTextExtractor.MAX_CSV_ROWS + 10; i++) {
                sb.append("val").append(i).append(",").append(i).append("\n");
            }
            InputStream stream = new ByteArrayInputStream(sb.toString().getBytes(StandardCharsets.UTF_8));
            String result = extractor.extract(stream, "big.csv");
            assertThat(result).contains("truncated");
        }
    }

    // ── DocxTextExtractor ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("DocxTextExtractor")
    class DocxExtractorTests {

        private final DocxTextExtractor extractor = new DocxTextExtractor();

        @Test
        @DisplayName("supports DOCX MIME type")
        void supportsDocx() {
            assertThat(extractor.supports(
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
                    .isTrue();
        }

        @Test
        @DisplayName("does not support text/plain")
        void rejectsTextPlain() {
            assertThat(extractor.supports("text/plain")).isFalse();
        }

        @Test
        @DisplayName("returns extraction failure notice on non-DOCX bytes")
        void handlesInvalidDocx() {
            // Pass random bytes that are not a valid DOCX
            byte[] notDocx = "This is not a DOCX file".getBytes(StandardCharsets.UTF_8);
            InputStream stream = new ByteArrayInputStream(notDocx);
            String result = extractor.extract(stream, "fake.docx");
            // Should return an error notice, not throw
            assertThat(result).contains("DOCX extraction failed");
        }
    }
}
