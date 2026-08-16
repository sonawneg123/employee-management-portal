package com.company.employeemanagement.service;

import com.company.employeemanagement.config.FileStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link FileValidationService}.
 *
 * Covers:
 * - PDF accepted
 * - CSV accepted (text/csv and text/plain variants)
 * - DOCX accepted
 * - TXT accepted
 * - Unsupported extension rejected
 * - Unsupported MIME type rejected
 * - Oversized file rejected
 * - Empty file rejected
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FileValidationService")
class FileValidationServiceTest {

    private FileValidationService validationService;

    @BeforeEach
    void setUp() {
        FileStorageProperties props = new FileStorageProperties();
        props.setMaxFileSizeBytes(10 * 1024 * 1024); // 10 MB
        validationService = new FileValidationService(props);
    }

    // ── Accepted file types ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Accepted file types")
    class AcceptedTypes {

        @Test
        @DisplayName("PDF file is accepted")
        void pdfAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "report.pdf", "application/pdf",
                    new byte[1024]);
            assertThatCode(() -> validationService.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CSV file with text/csv MIME type is accepted")
        void csvTextCsvAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "data.csv", "text/csv",
                    new byte[512]);
            assertThatCode(() -> validationService.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("CSV file with text/plain MIME type is accepted")
        void csvTextPlainAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "data.csv", "text/plain",
                    new byte[512]);
            assertThatCode(() -> validationService.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("DOCX file is accepted")
        void docxAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.docx",
                    "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                    new byte[2048]);
            assertThatCode(() -> validationService.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("TXT file is accepted")
        void txtAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "notes.txt", "text/plain",
                    new byte[256]);
            assertThatCode(() -> validationService.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("DOCX file with octet-stream MIME type is accepted (some browsers send this)")
        void docxOctetStreamAccepted() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "document.docx", "application/octet-stream",
                    new byte[2048]);
            assertThatCode(() -> validationService.validate(file)).doesNotThrowAnyException();
        }
    }

    // ── Rejected file types ───────────────────────────────────────────────────

    @Nested
    @DisplayName("Rejected file types")
    class RejectedTypes {

        @Test
        @DisplayName("ZIP extension is rejected with clear error")
        void zipRejected() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "archive.zip", "application/zip",
                    new byte[1024]);
            assertThatThrownBy(() -> validationService.validate(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(".zip")
                    .hasMessageContaining("Allowed types");
        }

        @Test
        @DisplayName("PNG extension is rejected")
        void pngRejected() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "image.png", "image/png",
                    new byte[1024]);
            assertThatThrownBy(() -> validationService.validate(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(".png");
        }

        @Test
        @DisplayName("XLSX extension is rejected")
        void xlsxRejected() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "spreadsheet.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[1024]);
            assertThatThrownBy(() -> validationService.validate(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(".xlsx");
        }

        @Test
        @DisplayName("MIME type mismatch is rejected — PDF extension with HTML MIME type")
        void mimeTypeMismatchRejected() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "report.pdf", "text/html",
                    new byte[1024]);
            assertThatThrownBy(() -> validationService.validate(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("text/html");
        }

        @Test
        @DisplayName("empty file is rejected")
        void emptyFileRejected() {
            MockMultipartFile file = new MockMultipartFile(
                    "file", "empty.pdf", "application/pdf",
                    new byte[0]);
            assertThatThrownBy(() -> validationService.validate(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }

        @Test
        @DisplayName("null file is rejected")
        void nullFileRejected() {
            assertThatThrownBy(() -> validationService.validate(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    // ── Size limit ────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("File size")
    class FileSize {

        @Test
        @DisplayName("file exactly at size limit is accepted")
        void fileSizeLimitExact() {
            FileStorageProperties props = new FileStorageProperties();
            props.setMaxFileSizeBytes(1024);
            FileValidationService svc = new FileValidationService(props);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "data.csv", "text/csv",
                    new byte[1024]);
            assertThatCode(() -> svc.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("file exceeding size limit is rejected")
        void fileSizeLimitExceeded() {
            FileStorageProperties props = new FileStorageProperties();
            props.setMaxFileSizeBytes(1024);
            FileValidationService svc = new FileValidationService(props);

            MockMultipartFile file = new MockMultipartFile(
                    "file", "data.csv", "text/csv",
                    new byte[1025]);
            assertThatThrownBy(() -> svc.validate(file))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("1025 bytes")
                    .hasMessageContaining("maximum");
        }
    }
}
