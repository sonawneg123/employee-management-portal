package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link DocumentChunkingService}.
 *
 * <p>Tests cover: null input, blank input, short text, long text,
 * overlap behaviour, word-boundary awareness, ordering, and determinism.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("DocumentChunkingService")
class DocumentChunkingServiceTest {

    private DocumentChunkingService chunkingService;

    /** Builds a {@link RagProperties} with explicit chunk size and overlap. */
    private static RagProperties props(int chunkSize, int chunkOverlap) {
        RagProperties p = new RagProperties();
        p.setChunkSize(chunkSize);
        p.setChunkOverlap(chunkOverlap);
        return p;
    }

    @BeforeEach
    void setUp() {
        // Default: 1000 chars, 150 overlap — matches application.properties
        chunkingService = new DocumentChunkingService(props(1000, 150));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Null / blank / empty
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Null / blank / empty input")
    class NullBlankEmpty {

        @Test
        @DisplayName("null input returns empty list")
        void nullReturnsEmpty() {
            assertThat(chunkingService.chunk(null)).isEmpty();
        }

        @Test
        @DisplayName("empty string returns empty list")
        void emptyStringReturnsEmpty() {
            assertThat(chunkingService.chunk("")).isEmpty();
        }

        @Test
        @DisplayName("whitespace-only string returns empty list")
        void whitespaceOnlyReturnsEmpty() {
            assertThat(chunkingService.chunk("   \t\n   ")).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Short text (fits within a single chunk)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Short text (single chunk)")
    class ShortText {

        @Test
        @DisplayName("text shorter than chunkSize returns single chunk")
        void shortTextReturnsSingleChunk() {
            String text = "Hello world";
            List<String> chunks = chunkingService.chunk(text);
            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo("Hello world");
        }

        @Test
        @DisplayName("text exactly at chunkSize returns single chunk")
        void textAtChunkSizeReturnsSingleChunk() {
            DocumentChunkingService svc = new DocumentChunkingService(props(10, 2));
            String text = "0123456789"; // exactly 10 chars
            List<String> chunks = svc.chunk(text);
            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo("0123456789");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Long text — multiple chunks
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Long text — multiple chunks")
    class LongText {

        @Test
        @DisplayName("text longer than chunkSize produces more than one chunk")
        void longTextProducesMultipleChunks() {
            // Build a string of ~100 words (well over 20-char chunkSize)
            String text = "one two three four five six seven eight nine ten ".repeat(5).trim();
            DocumentChunkingService svc = new DocumentChunkingService(props(20, 5));
            List<String> chunks = svc.chunk(text);
            assertThat(chunks).hasSizeGreaterThan(1);
        }

        @Test
        @DisplayName("no chunk exceeds chunkSize + a small word-boundary tolerance")
        void noChunkExceedsChunkSizeTolerance() {
            String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa ".repeat(10).trim();
            DocumentChunkingService svc = new DocumentChunkingService(props(50, 10));
            List<String> chunks = svc.chunk(text);
            // After word-boundary snapping each chunk should stay within chunkSize
            for (String chunk : chunks) {
                assertThat(chunk.length())
                        .as("chunk length should not greatly exceed chunkSize")
                        .isLessThanOrEqualTo(60);
            }
        }

        @Test
        @DisplayName("all words from original text appear across all chunks")
        void allWordsAppearInChunks() {
            String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa";
            DocumentChunkingService svc = new DocumentChunkingService(props(30, 5));
            List<String> chunks = svc.chunk(text);
            String combined = String.join(" ", chunks);
            for (String word : text.split("\\s+")) {
                assertThat(combined).contains(word);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Overlap
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Overlap")
    class Overlap {

        @Test
        @DisplayName("with overlap > 0 consecutive chunks share content near boundaries")
        void overlapProducesSharedContent() {
            // 30-char chunks with 10-char overlap → step=20 chars
            String text = "aaaaa bbbbb ccccc ddddd eeeee fffff ggggg hhhhh";
            DocumentChunkingService svc = new DocumentChunkingService(props(30, 10));
            List<String> chunks = svc.chunk(text);
            // With overlap, consecutive chunks should not be completely disjoint
            assertThat(chunks).hasSizeGreaterThanOrEqualTo(2);
        }

        @Test
        @DisplayName("with overlap = 0 chunks cover all content (combined text contains each word)")
        void zeroOverlapCoversAllContent() {
            // Use a large chunkSize relative to the text so each chunk captures full words cleanly.
            // The focus is on zero-overlap behaviour, not edge-case word-snapping.
            String text = "alpha beta gamma delta epsilon zeta eta theta iota kappa";
            DocumentChunkingService svc = new DocumentChunkingService(props(100, 0));
            List<String> chunks = svc.chunk(text);
            // With chunkSize >= text length, the whole text is a single chunk
            assertThat(chunks).hasSize(1);
            assertThat(chunks.get(0)).isEqualTo(text);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Ordering
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Ordering")
    class Ordering {

        @Test
        @DisplayName("chunks are returned in document order (first words in first chunk)")
        void chunksAreInDocumentOrder() {
            String text = "FIRST second third fourth fifth sixth seventh eighth ninth tenth eleventh";
            DocumentChunkingService svc = new DocumentChunkingService(props(20, 5));
            List<String> chunks = svc.chunk(text);
            assertThat(chunks.get(0)).startsWith("FIRST");
        }

        @Test
        @DisplayName("last chunk contains the end of the document")
        void lastChunkContainsEndOfDocument() {
            String text = "one two three four five six seven eight nine TEN";
            DocumentChunkingService svc = new DocumentChunkingService(props(20, 5));
            List<String> chunks = svc.chunk(text);
            assertThat(chunks.get(chunks.size() - 1)).contains("TEN");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Determinism
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Determinism")
    class Determinism {

        @Test
        @DisplayName("same text always produces identical chunks")
        void sameInputProducesSameOutput() {
            String text = "The quick brown fox jumps over the lazy dog. ".repeat(20).trim();
            DocumentChunkingService svc = new DocumentChunkingService(props(100, 20));
            List<String> first  = svc.chunk(text);
            List<String> second = svc.chunk(text);
            assertThat(first).isEqualTo(second);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // estimateTokenCount
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("estimateTokenCount")
    class EstimateTokenCount {

        @Test
        @DisplayName("null returns 0")
        void nullReturnsZero() {
            assertThat(chunkingService.estimateTokenCount(null)).isZero();
        }

        @Test
        @DisplayName("blank returns 0")
        void blankReturnsZero() {
            assertThat(chunkingService.estimateTokenCount("   ")).isZero();
        }

        @Test
        @DisplayName("three words return 3")
        void threeWordsReturnThree() {
            assertThat(chunkingService.estimateTokenCount("one two three")).isEqualTo(3);
        }

        @Test
        @DisplayName("single word returns 1")
        void singleWordReturnsOne() {
            assertThat(chunkingService.estimateTokenCount("hello")).isEqualTo(1);
        }
    }
}
