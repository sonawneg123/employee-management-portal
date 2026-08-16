package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link RagPromptContextBuilder}.
 *
 * @author Employee Management Portal Team
 */
@DisplayName("RagPromptContextBuilder")
class RagPromptContextBuilderTest {

    private RagPromptContextBuilder builder;

    @BeforeEach
    void setUp() {
        builder = new RagPromptContextBuilder();
    }

    // ── No-context notice ─────────────────────────────────────────────────────

    @Nested
    @DisplayName("No-context (empty or null chunks)")
    class NoContext {

        @Test
        @DisplayName("empty list returns no-context notice")
        void emptyListReturnsNotice() {
            String result = builder.buildContextSection(Collections.emptyList());

            assertThat(result).contains("No relevant company knowledge was found");
            assertThat(result).contains("Do not claim that general knowledge is an official company policy");
            assertThat(result).doesNotContain("KNOWLEDGE BASE CONTEXT");
        }

        @Test
        @DisplayName("null list returns no-context notice")
        void nullListReturnsNotice() {
            String result = builder.buildContextSection(null);

            assertThat(result).contains("No relevant company knowledge was found");
        }
    }

    // ── Single chunk ──────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Single chunk")
    class SingleChunk {

        @Test
        @DisplayName("contains header, document title, chunk content, and footer")
        void singleChunkHasAllParts() {
            KnowledgeSearchResult chunk = buildChunk("Employee Leave Policy", 0,
                    "Employees must submit leave requests at least three working days in advance.");

            String result = builder.buildContextSection(List.of(chunk));

            assertThat(result).startsWith("KNOWLEDGE BASE CONTEXT");
            assertThat(result).contains("[Document: Employee Leave Policy]");
            assertThat(result).contains("three working days in advance");
            assertThat(result).endsWith("END KNOWLEDGE BASE CONTEXT");
        }

        @Test
        @DisplayName("document ID is not exposed in the context block")
        void documentIdNotExposed() {
            KnowledgeSearchResult chunk = buildChunk("Leave Policy", 0, "Some policy text.");
            UUID docId = chunk.documentId();

            String result = builder.buildContextSection(List.of(chunk));

            assertThat(result).doesNotContain(docId.toString());
        }

        @Test
        @DisplayName("chunk ID is not exposed in the context block")
        void chunkIdNotExposed() {
            KnowledgeSearchResult chunk = buildChunk("Leave Policy", 0, "Some policy text.");
            UUID chunkId = chunk.chunkId();

            String result = builder.buildContextSection(List.of(chunk));

            assertThat(result).doesNotContain(chunkId.toString());
        }
    }

    // ── Multiple chunks, same document ───────────────────────────────────────

    @Nested
    @DisplayName("Multiple chunks from the same document")
    class MultipleChunksSameDoc {

        @Test
        @DisplayName("document heading appears only once for consecutive same-doc chunks")
        void documentHeadingAppearsOnce() {
            KnowledgeSearchResult c1 = buildChunk("Employee Leave Policy", 0, "Content one.");
            KnowledgeSearchResult c2 = buildChunk("Employee Leave Policy", 1, "Content two.");

            String result = builder.buildContextSection(List.of(c1, c2));

            long headingCount = result.lines()
                    .filter(l -> l.contains("[Document: Employee Leave Policy]"))
                    .count();
            assertThat(headingCount).isEqualTo(1);
            assertThat(result).contains("Content one.");
            assertThat(result).contains("Content two.");
        }
    }

    // ── Multiple chunks, different documents ─────────────────────────────────

    @Nested
    @DisplayName("Multiple chunks from different documents")
    class MultipleChunksDifferentDocs {

        @Test
        @DisplayName("each document gets its own heading")
        void eachDocumentGetsHeading() {
            KnowledgeSearchResult c1 = buildChunk("Leave Policy", 0, "Leave content.");
            KnowledgeSearchResult c2 = buildChunk("Remote Work Policy", 0, "Remote content.");

            String result = builder.buildContextSection(List.of(c1, c2));

            assertThat(result).contains("[Document: Leave Policy]");
            assertThat(result).contains("[Document: Remote Work Policy]");
            assertThat(result).contains("Leave content.");
            assertThat(result).contains("Remote content.");
        }

        @Test
        @DisplayName("ordering is preserved — first chunk's document appears first")
        void orderingPreserved() {
            KnowledgeSearchResult c1 = buildChunk("Doc A", 0, "A content.");
            KnowledgeSearchResult c2 = buildChunk("Doc B", 0, "B content.");

            String result = builder.buildContextSection(List.of(c1, c2));

            assertThat(result.indexOf("[Document: Doc A]"))
                    .isLessThan(result.indexOf("[Document: Doc B]"));
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private KnowledgeSearchResult buildChunk(String docTitle, int chunkIndex, String content) {
        return new KnowledgeSearchResult(
                UUID.randomUUID(),
                docTitle,
                UUID.randomUUID(),
                chunkIndex,
                content,
                "CONTENT"
        );
    }
}
