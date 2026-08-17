package com.company.employeemanagement.ai.service;

import com.company.employeemanagement.ai.service.impl.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the shared {@link PdfTextExtractor} utility methods.
 *
 * <p>Also validates the normalisation and truncation logic shared
 * across all extractor implementations.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("PdfTextExtractor (shared utilities)")
class PdfTextExtractorTest {

    @Nested
    @DisplayName("normaliseAndTruncate")
    class NormaliseAndTruncate {

        @Test
        @DisplayName("returns empty notice for null input")
        void nullInput() {
            String result = PdfTextExtractor.normaliseAndTruncate(null, "file.pdf");
            assertThat(result).contains("No readable text");
        }

        @Test
        @DisplayName("returns empty notice for blank input")
        void blankInput() {
            String result = PdfTextExtractor.normaliseAndTruncate("   ", "file.pdf");
            assertThat(result).contains("No readable text");
        }

        @Test
        @DisplayName("returns text unchanged when under the limit")
        void shortTextUnchanged() {
            String input = "Hello world\nThis is a test.";
            String result = PdfTextExtractor.normaliseAndTruncate(input, "file.pdf");
            assertThat(result).isEqualTo(input.trim());
        }

        @Test
        @DisplayName("collapses multiple blank lines into one")
        void collapsesBlanks() {
            String input = "Line 1\n\n\n\nLine 2";
            String result = PdfTextExtractor.normaliseAndTruncate(input, "file.pdf");
            assertThat(result).isEqualTo("Line 1\n\nLine 2");
        }

        @Test
        @DisplayName("truncates text exceeding MAX_EXTRACTED_CHARS")
        void truncatesLongText() {
            String longText = "A".repeat(SubmissionFileExtractor.MAX_EXTRACTED_CHARS + 1000);
            String result = PdfTextExtractor.normaliseAndTruncate(longText, "big.pdf");
            assertThat(result.length()).isLessThanOrEqualTo(
                    SubmissionFileExtractor.MAX_EXTRACTED_CHARS + 200); // allow for truncation notice
            assertThat(result).contains("truncated");
        }

        @Test
        @DisplayName("truncation notice includes character count")
        void truncationNoticeContainsCount() {
            String longText = "B".repeat(SubmissionFileExtractor.MAX_EXTRACTED_CHARS + 500);
            String result = PdfTextExtractor.normaliseAndTruncate(longText, "big.pdf");
            assertThat(result).contains(String.valueOf(SubmissionFileExtractor.MAX_EXTRACTED_CHARS));
        }
    }

    @Nested
    @DisplayName("sanitiseErrorMessage")
    class SanitiseErrorMessage {

        @Test
        @DisplayName("returns first line of multi-line message")
        void firstLineOnly() {
            String message = "First line\nSecond line\nThird line";
            assertThat(PdfTextExtractor.sanitiseErrorMessage(message)).isEqualTo("First line");
        }

        @Test
        @DisplayName("handles null message")
        void nullMessage() {
            assertThat(PdfTextExtractor.sanitiseErrorMessage(null)).isEqualTo("unknown error");
        }
    }
}
