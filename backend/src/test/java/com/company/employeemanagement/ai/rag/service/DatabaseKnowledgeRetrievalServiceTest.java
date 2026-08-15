package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
import com.company.employeemanagement.ai.rag.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DatabaseKnowledgeRetrievalService}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DatabaseKnowledgeRetrievalService")
class DatabaseKnowledgeRetrievalServiceTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    private DatabaseKnowledgeRetrievalService retrievalService;

    @BeforeEach
    void setUp() {
        retrievalService = new DatabaseKnowledgeRetrievalService(documentRepository, chunkRepository);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // No results
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("No results")
    class NoResults {

        @Test
        @DisplayName("returns empty list when no docs or chunks match")
        void returnsEmptyWhenNoMatch() {
            when(documentRepository.searchByKeyword(anyString(), any(KnowledgeDocumentStatus.class)))
                    .thenReturn(Collections.emptyList());
            when(chunkRepository.searchByContentKeyword(anyString(), any(KnowledgeDocumentStatus.class)))
                    .thenReturn(Collections.emptyList());

            KnowledgeSearchRequest req = new KnowledgeSearchRequest("nonexistent topic", 5);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).isEmpty();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Title match
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Title match")
    class TitleMatch {

        @Test
        @DisplayName("title match returns first chunk with matchType TITLE")
        void titleMatchReturnsTitleResult() {
            KnowledgeDocument doc = buildDoc("Remote Work Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, "Employees may work remotely up to 3 days per week.");

            when(documentRepository.searchByKeyword(eq("remote"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(doc));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId()))
                    .thenReturn(List.of(chunk));
            when(chunkRepository.searchByContentKeyword(eq("remote"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());

            KnowledgeSearchRequest req = new KnowledgeSearchRequest("remote", 10);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            KnowledgeSearchResult result = results.get(0);
            assertThat(result.matchType()).isEqualTo("TITLE");
            assertThat(result.documentTitle()).isEqualTo("Remote Work Policy");
            assertThat(result.chunkIndex()).isZero();
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Content match
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Content match")
    class ContentMatch {

        @Test
        @DisplayName("content match returns chunk with matchType CONTENT")
        void contentMatchReturnsContentResult() {
            KnowledgeDocument doc = buildDoc("HR Handbook");
            KnowledgeChunk chunk = buildChunk(doc, 2, "The vacation policy allows 15 days per year.");

            when(documentRepository.searchByKeyword(eq("vacation"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            when(chunkRepository.searchByContentKeyword(eq("vacation"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest("vacation", 10);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo("CONTENT");
            assertThat(results.get(0).chunkIndex()).isEqualTo(2);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Multiple chunks / deduplication
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Multiple chunks and deduplication")
    class MultipleChunks {

        @Test
        @DisplayName("same chunk from title and content is deduplicated")
        void sameChunkDeduplicatedAcrossTitleAndContent() {
            KnowledgeDocument doc = buildDoc("Leave Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, "Annual leave is 20 days.");

            when(documentRepository.searchByKeyword(eq("leave"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(doc));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId()))
                    .thenReturn(List.of(chunk));
            // same chunk also returned from content search
            when(chunkRepository.searchByContentKeyword(eq("leave"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest("leave", 10);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            // Even though the chunk appeared in both searches, it should appear only once
            assertThat(results).hasSize(1);
        }

        @Test
        @DisplayName("maxResults limits the number of returned results")
        void maxResultsLimitsResults() {
            KnowledgeDocument doc1 = buildDoc("Doc One");
            KnowledgeDocument doc2 = buildDoc("Doc Two");
            KnowledgeChunk chunk1 = buildChunk(doc1, 0, "content alpha");

            // Both docs match by title; maxResults=1 means the loop stops after doc1.
            // The content-search branch is never entered (already at maxResults),
            // so searchByContentKeyword must NOT be stubbed.
            when(documentRepository.searchByKeyword(eq("content"), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(doc1, doc2));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(doc1.getId()))
                    .thenReturn(List.of(chunk1));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest("content", 1);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).documentTitle()).isEqualTo("Doc One");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2A regression: topK alias + ACTIVE-status enum fix
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 2A regression — Employee Leave Policy scenario")
    class LeavePolicyRegression {

        private static final String LEAVE_CONTENT =
                "Employees are entitled to annual leave according to company policy. " +
                "Employees must submit leave requests through the employee management portal. " +
                "Leave requests should normally be submitted at least three working days before " +
                "the requested leave date. Managers are responsible for reviewing and approving " +
                "leave requests. Emergency leave may be requested with appropriate justification.";

        /**
         * Exact-phrase search — matches content containing the full query string.
         * Verifies that the repository is called with ACTIVE status enum (the bug fix).
         */
        @Test
        @DisplayName("exact phrase search returns matching chunk with matchType CONTENT")
        void exactPhraseSearchReturnsResult() {
            KnowledgeDocument doc = buildDoc("Employee Leave Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, LEAVE_CONTENT);

            String query = "Employees are entitled to annual leave";
            when(documentRepository.searchByKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            when(chunkRepository.searchByContentKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest(query, 5);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).documentTitle()).isEqualTo("Employee Leave Policy");
            assertThat(results.get(0).chunkContent()).contains("Employees are entitled to annual leave");
            assertThat(results.get(0).matchType()).isEqualTo("CONTENT");
            assertThat(results.get(0).chunkIndex()).isZero();
        }

        /**
         * Partial phrase search — a sub-phrase that appears within the chunk content.
         */
        @Test
        @DisplayName("partial phrase search returns matching chunk")
        void partialPhraseSearchReturnsResult() {
            KnowledgeDocument doc = buildDoc("Employee Leave Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, LEAVE_CONTENT);

            String query = "annual leave";
            when(documentRepository.searchByKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            when(chunkRepository.searchByContentKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest(query, 5);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).chunkContent()).containsIgnoringCase("annual leave");
        }

        /**
         * Case-insensitive content search — query in upper case should still match via LOWER() in JPQL.
         * The service layer passes the query straight through; the LOWER() is applied in the DB query.
         * The mock simulates the database having performed a case-insensitive match.
         */
        @Test
        @DisplayName("case-insensitive search (simulated via mock) returns result")
        void caseInsensitiveSearchReturnsResult() {
            KnowledgeDocument doc = buildDoc("Employee Leave Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, LEAVE_CONTENT);

            String query = "ANNUAL LEAVE";
            when(documentRepository.searchByKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            // DB would return the chunk because it does LOWER(content) LIKE LOWER('%ANNUAL LEAVE%')
            when(chunkRepository.searchByContentKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest(query, 5);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).chunkContent()).containsIgnoringCase("annual leave");
        }

        /**
         * topK is respected — if {@code topK} (sent as the JSON field name used by callers)
         * maps to maxResults=2 and there are 3 matching chunks, only 2 are returned.
         */
        @Test
        @DisplayName("topK limits the number of returned results")
        void topKLimitsResults() {
            KnowledgeDocument doc = buildDoc("Employee Leave Policy");
            KnowledgeChunk chunk0 = buildChunk(doc, 0, "Employees are entitled to annual leave.");
            KnowledgeChunk chunk1 = buildChunk(doc, 1, "Leave requests require manager approval.");
            KnowledgeChunk chunk2 = buildChunk(doc, 2, "Emergency leave needs justification.");

            String query = "leave";
            when(documentRepository.searchByKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            when(chunkRepository.searchByContentKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk0, chunk1, chunk2));

            // topK=2 serialises as maxResults=2 in the record (the @JsonAlias handles "topK" in JSON)
            KnowledgeSearchRequest req = new KnowledgeSearchRequest(query, 2);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(2);
        }

        /**
         * Verifies that the result maps actual DB values (documentTitle, chunkContent, chunkIndex)
         * rather than placeholder values.
         */
        @Test
        @DisplayName("result fields map actual database values, not placeholders")
        void resultFieldsMappedFromDatabase() {
            KnowledgeDocument doc = buildDoc("Employee Leave Policy");
            KnowledgeChunk chunk = buildChunk(doc, 3, "Emergency leave may be requested.");

            String query = "Emergency leave";
            when(documentRepository.searchByKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());
            when(chunkRepository.searchByContentKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest(query, 5);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            KnowledgeSearchResult r = results.get(0);
            assertThat(r.documentId()).isEqualTo(doc.getId());
            assertThat(r.documentTitle()).isEqualTo("Employee Leave Policy");
            assertThat(r.chunkId()).isEqualTo(chunk.getId());
            assertThat(r.chunkIndex()).isEqualTo(3);
            assertThat(r.chunkContent()).isEqualTo("Emergency leave may be requested.");
            assertThat(r.matchType()).isEqualTo("CONTENT");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 2A regression: title match for Leave Policy (title-search path)
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 2A regression — title match path with enum fix")
    class TitleMatchRegression {

        @Test
        @DisplayName("title search for 'Employee Leave Policy' returns TITLE match")
        void leavePolicyTitleMatchReturnsResult() {
            KnowledgeDocument doc = buildDoc("Employee Leave Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0,
                    "Employees are entitled to annual leave according to company policy.");

            String query = "Employee Leave Policy";
            when(documentRepository.searchByKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(List.of(doc));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId()))
                    .thenReturn(List.of(chunk));
            when(chunkRepository.searchByContentKeyword(eq(query), eq(KnowledgeDocumentStatus.ACTIVE)))
                    .thenReturn(Collections.emptyList());

            KnowledgeSearchRequest req = new KnowledgeSearchRequest(query, 5);
            List<KnowledgeSearchResult> results = retrievalService.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).isEqualTo("TITLE");
            assertThat(results.get(0).documentTitle()).isEqualTo("Employee Leave Policy");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private KnowledgeDocument buildDoc(String title) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setSourceType(KnowledgeSourceType.POLICY);
        doc.setStatus(KnowledgeDocumentStatus.ACTIVE);
        doc.setContent("sample content");
        // Reflectively inject a UUID since there is no real DB in this unit test
        try {
            java.lang.reflect.Field idField =
                    com.company.employeemanagement.entity.BaseEntity.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(doc, UUID.randomUUID());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return doc;
    }

    private KnowledgeChunk buildChunk(KnowledgeDocument doc, int index, String content) {
        return KnowledgeChunk.builder()
                .id(UUID.randomUUID())
                .document(doc)
                .chunkIndex(index)
                .content(content)
                .tokenCount(content.split("\\s+").length)
                .build();
    }
}
