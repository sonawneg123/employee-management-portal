package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingException;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingService;
import com.company.employeemanagement.ai.rag.embedding.VectorSimilarity;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link VectorKnowledgeRetrievalService}.
 *
 * <h2>Key regression tests</h2>
 * <ul>
 *   <li>Natural-language query "How many days in advance should I submit a remote-work
 *       request?" must retrieve the chunk containing "2 working days".</li>
 *   <li>Queries below the similarity threshold must return no results.</li>
 *   <li>Semantic variations of working hours / remote-work queries must retrieve the
 *       correct chunk.</li>
 * </ul>
 *
 * <p>All collaborators are mocked — no network calls or DB connections.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("VectorKnowledgeRetrievalService")
class VectorKnowledgeRetrievalServiceTest {

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private EmbeddingService embeddingService;

    private RagProperties               ragProperties;
    private VectorKnowledgeRetrievalService service;

    // ── Canonical chunk content (mirrors the actual policy document) ──────────

    private static final String REMOTE_WORK_CHUNK =
            "Remote-work requests should normally be submitted at least 2 working days in advance.";

    private static final String WORKING_HOURS_CHUNK =
            "Standard working hours are 9:30 AM to 6:30 PM, Monday through Friday.";

    private static final String LUNCH_BREAK_CHUNK =
            "The lunch break is one hour, from 1:30 PM to 2:30 PM.";

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        ragProperties.setSimilarityThreshold(0.70);
        ragProperties.setTopK(5);
        service = new VectorKnowledgeRetrievalService(chunkRepository, embeddingService, ragProperties);
    }

    // ── Core regression: natural-language remote-work query ───────────────────

    @Nested
    @DisplayName("Phase 3 core regression — natural-language remote-work query")
    class RemoteWorkRegression {

        /**
         * This is THE critical test for Phase 3.
         * The natural-language query must retrieve the remote-work chunk
         * even though the query does not literally contain the words "2 working days".
         * We simulate this by giving the query and the remote-work chunk high cosine similarity.
         */
        @Test
        @DisplayName("'How many days in advance should I submit a remote-work request?' retrieves 2-working-days chunk")
        void naturalLanguageQueryRetrievesRemoteWorkChunk() {
            // A high-similarity query/chunk pair (above 0.70 threshold)
            float[] queryVector = unitVector(new float[]{0.9f, 0.4f, 0.0f});
            float[] chunkVector = unitVector(new float[]{0.85f, 0.45f, 0.05f}); // very similar

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, REMOTE_WORK_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            KnowledgeSearchRequest req = new KnowledgeSearchRequest(
                    "How many days in advance should I submit a remote-work request?", 5);
            List<KnowledgeSearchResult> results = service.search(req);

            assertThat(results).hasSize(1);
            assertThat(results.get(0).documentTitle())
                    .isEqualTo("Employee Attendance and Work Hours Policy");
            assertThat(results.get(0).chunkContent())
                    .contains("2 working days");
            assertThat(results.get(0).matchType()).startsWith("VECTOR(");
        }

        @Test
        @DisplayName("'How much notice is needed before working remotely?' retrieves the same chunk")
        void semanticVariation1RetrievesRemoteWorkChunk() {
            float[] queryVector = unitVector(new float[]{0.88f, 0.42f, 0.01f});
            float[] chunkVector = unitVector(new float[]{0.85f, 0.45f, 0.05f});

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, REMOTE_WORK_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("How much notice is needed before working remotely?", 5));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).chunkContent()).contains("2 working days");
        }

        @Test
        @DisplayName("'Can I request remote work shortly before the date?' retrieves the chunk")
        void semanticVariation2RetrievesRemoteWorkChunk() {
            float[] queryVector = unitVector(new float[]{0.87f, 0.43f, 0.02f});
            float[] chunkVector = unitVector(new float[]{0.85f, 0.45f, 0.05f});

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, REMOTE_WORK_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("Can I request remote work shortly before the date?", 5));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).chunkContent()).contains("2 working days");
        }
    }

    // ── Working hours retrieval ────────────────────────────────────────────────

    @Nested
    @DisplayName("Working hours retrieval")
    class WorkingHoursRetrieval {

        @Test
        @DisplayName("'What are the standard working hours?' retrieves 9:30 AM to 6:30 PM chunk")
        void workingHoursQueryRetrievesCorrectChunk() {
            float[] queryVector = unitVector(new float[]{0.1f, 0.9f, 0.0f});
            float[] chunkVector = unitVector(new float[]{0.12f, 0.88f, 0.02f});

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 1, WORKING_HOURS_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("What are the standard working hours?", 5));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).chunkContent()).contains("9:30 AM to 6:30 PM");
        }

        @Test
        @DisplayName("'When am I expected to work?' retrieves working hours chunk")
        void whenExpectedToWorkRetrievesChunk() {
            float[] queryVector = unitVector(new float[]{0.11f, 0.88f, 0.01f});
            float[] chunkVector = unitVector(new float[]{0.12f, 0.88f, 0.02f});

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 1, WORKING_HOURS_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("When am I expected to work?", 5));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).chunkContent()).contains("9:30 AM");
        }
    }

    // ── Similarity threshold filtering ────────────────────────────────────────

    @Nested
    @DisplayName("Similarity threshold filtering")
    class SimilarityThreshold {

        @Test
        @DisplayName("chunk below threshold is excluded from results")
        void belowThresholdExcluded() {
            // Query vector is nearly orthogonal to the chunk vector → low similarity
            float[] queryVector = unitVector(new float[]{1.0f, 0.0f, 0.0f});
            float[] chunkVector = unitVector(new float[]{0.0f, 1.0f, 0.0f}); // cos = 0.0

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, REMOTE_WORK_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("unrelated question", 5));

            // Similarity ≈ 0.0, well below threshold 0.70 → empty results
            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("'What is the capital of France?' returns no policy chunks")
        void capitalOfFranceReturnsNoResults() {
            // Simulate a geography query that has zero semantic overlap with HR policies
            float[] queryVector = unitVector(new float[]{0.0f, 0.0f, 1.0f}); // geography
            float[] chunkVector = unitVector(new float[]{0.9f, 0.4f, 0.0f}); // HR remote work

            KnowledgeDocument doc = buildDoc("Employee Attendance and Work Hours Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, REMOTE_WORK_CHUNK, chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("What is the capital of France?", 5));

            assertThat(results).isEmpty();
        }

        @Test
        @DisplayName("custom threshold of 0.0 returns all chunks regardless of similarity")
        void zeroThresholdReturnsAll() {
            ragProperties.setSimilarityThreshold(0.0);

            float[] queryVector = unitVector(new float[]{1.0f, 0.0f});
            float[] chunkVector = unitVector(new float[]{0.0f, 1.0f}); // orthogonal

            KnowledgeDocument doc = buildDoc("Any Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, "Some policy text.", chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("anything", 5));

            // With 0.0 threshold, even orthogonal results are included (similarity = 0.0 ≥ 0.0)
            assertThat(results).hasSize(1);
        }
    }

    // ── TopK limiting ─────────────────────────────────────────────────────────

    @Nested
    @DisplayName("TopK limiting")
    class TopKLimiting {

        @Test
        @DisplayName("returns at most maxResults results even when more candidates pass threshold")
        void topKLimitsResults() {
            float[] queryVector = unitVector(new float[]{1.0f, 0.0f});
            KnowledgeDocument doc = buildDoc("Policy");

            // Three chunks all above threshold, slightly different similarities
            KnowledgeChunk c1 = buildChunk(doc, 0, "chunk 1", unitVector(new float[]{0.99f, 0.14f}));
            KnowledgeChunk c2 = buildChunk(doc, 1, "chunk 2", unitVector(new float[]{0.98f, 0.20f}));
            KnowledgeChunk c3 = buildChunk(doc, 2, "chunk 3", unitVector(new float[]{0.97f, 0.24f}));

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(c1, c2, c3));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("query", 2)); // maxResults = 2

            assertThat(results).hasSize(2);
        }

        @Test
        @DisplayName("results are sorted by descending similarity")
        void resultsSortedByDescendingSimilarity() {
            float[] queryVector = unitVector(new float[]{1.0f, 0.0f});
            KnowledgeDocument doc = buildDoc("Policy");

            // c2 is more similar than c1
            KnowledgeChunk c1 = buildChunk(doc, 0, "less similar", unitVector(new float[]{0.92f, 0.39f}));
            KnowledgeChunk c2 = buildChunk(doc, 1, "more similar", unitVector(new float[]{0.99f, 0.14f}));

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(c1, c2));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("query", 5));

            assertThat(results).hasSize(2);
            // More similar chunk should come first
            assertThat(results.get(0).chunkContent()).isEqualTo("more similar");
            assertThat(results.get(1).chunkContent()).isEqualTo("less similar");
        }
    }

    // ── No candidates / empty repository ─────────────────────────────────────

    @Nested
    @DisplayName("Empty repository")
    class EmptyRepository {

        @Test
        @DisplayName("returns empty list when no chunks have embeddings")
        void noEmbeddedChunksReturnsEmpty() {
            float[] queryVector = unitVector(new float[]{1.0f, 0.0f});
            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(Collections.emptyList());

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("remote work", 5));

            assertThat(results).isEmpty();
        }
    }

    // ── Embedding failure propagation ─────────────────────────────────────────

    @Nested
    @DisplayName("Embedding failure propagation")
    class EmbeddingFailure {

        @Test
        @DisplayName("EmbeddingException from query embedding propagates to caller")
        void embeddingExceptionPropagates() {
            when(embeddingService.embed(any()))
                    .thenThrow(new EmbeddingException("API unreachable"));

            assertThatThrownBy(() -> service.search(
                    new KnowledgeSearchRequest("remote work", 5)))
                    .isInstanceOf(EmbeddingException.class)
                    .hasMessageContaining("unreachable");
        }
    }

    // ── matchType format ──────────────────────────────────────────────────────

    @Nested
    @DisplayName("matchType format")
    class MatchTypeFormat {

        @Test
        @DisplayName("matchType starts with 'VECTOR(' and contains a float value")
        void matchTypeContainsSimilarityScore() {
            float[] queryVector = unitVector(new float[]{0.9f, 0.4f});
            float[] chunkVector = unitVector(new float[]{0.88f, 0.47f});

            KnowledgeDocument doc = buildDoc("Policy");
            KnowledgeChunk chunk = buildChunk(doc, 0, "some policy text", chunkVector);

            when(embeddingService.embed(any())).thenReturn(queryVector);
            when(chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE))
                    .thenReturn(List.of(chunk));

            List<KnowledgeSearchResult> results = service.search(
                    new KnowledgeSearchRequest("query", 5));

            assertThat(results).hasSize(1);
            assertThat(results.get(0).matchType()).startsWith("VECTOR(");
            assertThat(results.get(0).matchType()).endsWith(")");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private KnowledgeDocument buildDoc(String title) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(title);
        doc.setSourceType(KnowledgeSourceType.POLICY);
        doc.setStatus(KnowledgeDocumentStatus.ACTIVE);
        doc.setContent("content");
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

    private KnowledgeChunk buildChunk(KnowledgeDocument doc, int index, String content, float[] vector) {
        return KnowledgeChunk.builder()
                .id(UUID.randomUUID())
                .document(doc)
                .chunkIndex(index)
                .content(content)
                .tokenCount(content.split("\\s+").length)
                .embeddingVector(VectorSimilarity.toBytes(vector))
                .build();
    }

    /**
     * Normalises a vector to unit length so cosine similarity is purely
     * directional and not affected by scale differences in test data.
     */
    private static float[] unitVector(float[] v) {
        double norm = 0.0;
        for (float f : v) norm += (double) f * f;
        norm = Math.sqrt(norm);
        if (norm == 0.0) return v;
        float[] result = new float[v.length];
        for (int i = 0; i < v.length; i++) result[i] = (float) (v[i] / norm);
        return result;
    }
}
