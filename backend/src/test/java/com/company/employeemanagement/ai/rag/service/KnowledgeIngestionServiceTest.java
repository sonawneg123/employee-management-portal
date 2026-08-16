package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.dto.IngestDocumentRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeDocumentResponse;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingException;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;
import com.company.employeemanagement.ai.rag.exception.RagException;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
import com.company.employeemanagement.ai.rag.repository.KnowledgeDocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link KnowledgeIngestionService}.
 *
 * @author Employee Management Portal Team
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("KnowledgeIngestionService")
class KnowledgeIngestionServiceTest {

    @Mock
    private KnowledgeDocumentRepository documentRepository;

    @Mock
    private KnowledgeChunkRepository chunkRepository;

    @Mock
    private com.company.employeemanagement.ai.rag.embedding.EmbeddingService embeddingService;

    private KnowledgeIngestionService ingestionService;
    private DocumentChunkingService   chunkingService;
    private RagProperties             ragProperties;

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        ragProperties.setChunkSize(50);
        ragProperties.setChunkOverlap(10);
        // Disable embeddings by default so existing tests are unaffected
        ragProperties.getEmbedding().setEnabled(false);
        chunkingService  = new DocumentChunkingService(ragProperties);
        ingestionService = new KnowledgeIngestionService(
                documentRepository, chunkRepository, chunkingService,
                embeddingService, ragProperties);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Successful ingestion
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Successful document creation")
    class SuccessfulIngestion {

        @Test
        @DisplayName("saves document and chunks, then returns ACTIVE response")
        void savesDocumentAndChunks() {
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Remote Work Policy",
                    "Policy document",
                    KnowledgeSourceType.POLICY,
                    "policy.pdf",
                    "Remote work allows employees to work from home. ".repeat(10)
            );

            KnowledgeDocument saved = buildDocument(request, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.save(any())).thenReturn(saved);
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());

            KnowledgeDocumentResponse response = ingestionService.ingestDocument(request);

            assertThat(response.title()).isEqualTo("Remote Work Policy");
            assertThat(response.status()).isEqualTo(KnowledgeDocumentStatus.ACTIVE);
            assertThat(response.sourceType()).isEqualTo(KnowledgeSourceType.POLICY);

            // document saved at least twice (PROCESSING + ACTIVE)
            verify(documentRepository, times(2)).save(any());
            // chunks were persisted
            verify(chunkRepository).saveAll(any());
        }

        @Test
        @DisplayName("short document produces a single chunk")
        void shortDocumentProducesSingleChunk() {
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Short Doc", null, KnowledgeSourceType.FAQ, null, "Short content here"
            );
            KnowledgeDocument saved = buildDocument(request, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.save(any())).thenReturn(saved);
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());

            ingestionService.ingestDocument(request);

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List> chunksCaptor = ArgumentCaptor.forClass(List.class);
            verify(chunkRepository).saveAll(chunksCaptor.capture());
            assertThat(chunksCaptor.getValue()).hasSize(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Validation failures
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Invalid content")
    class InvalidContent {

        @Test
        @DisplayName("whitespace-only content throws RagException")
        void whitespaceContentThrows() {
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Title", null, KnowledgeSourceType.OTHER, null, "   "
            );
            assertThatThrownBy(() -> ingestionService.ingestDocument(request))
                    .isInstanceOf(RagException.class)
                    .hasMessageContaining("blank");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // listAll / getDocument / deleteDocument
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Query operations")
    class QueryOperations {

        @Test
        @DisplayName("listAll returns all documents")
        void listAllReturnsDocs() {
            IngestDocumentRequest req = new IngestDocumentRequest(
                    "Doc1", null, KnowledgeSourceType.HANDBOOK, null, "content"
            );
            KnowledgeDocument doc = buildDocument(req, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.findAll()).thenReturn(List.of(doc));

            List<KnowledgeDocumentResponse> result = ingestionService.listAll();

            assertThat(result).hasSize(1);
            assertThat(result.get(0).title()).isEqualTo("Doc1");
        }

        @Test
        @DisplayName("getDocument returns response for existing ID")
        void getDocumentFound() {
            UUID id = UUID.randomUUID();
            IngestDocumentRequest req = new IngestDocumentRequest(
                    "Doc1", null, KnowledgeSourceType.HR_DOCUMENT, null, "content"
            );
            KnowledgeDocument doc = buildDocument(req, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.findById(id)).thenReturn(Optional.of(doc));

            KnowledgeDocumentResponse response = ingestionService.getDocument(id);

            assertThat(response.title()).isEqualTo("Doc1");
        }

        @Test
        @DisplayName("getDocument throws RagException for unknown ID")
        void getDocumentNotFound() {
            UUID id = UUID.randomUUID();
            when(documentRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ingestionService.getDocument(id))
                    .isInstanceOf(RagException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("deleteDocument removes chunks then document")
        void deleteDocumentRemovesChunksAndDoc() {
            UUID id = UUID.randomUUID();
            when(documentRepository.existsById(id)).thenReturn(true);

            ingestionService.deleteDocument(id);

            verify(chunkRepository).deleteByDocumentId(id);
            verify(documentRepository).deleteById(id);
        }

        @Test
        @DisplayName("deleteDocument throws RagException for unknown ID")
        void deleteDocumentNotFound() {
            UUID id = UUID.randomUUID();
            when(documentRepository.existsById(id)).thenReturn(false);

            assertThatThrownBy(() -> ingestionService.deleteDocument(id))
                    .isInstanceOf(RagException.class)
                    .hasMessageContaining(id.toString());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3 — Embedding integration tests
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 3 — Embedding during ingestion")
    class EmbeddingDuringIngestion {

        @BeforeEach
        void enableEmbedding() {
            ragProperties.getEmbedding().setEnabled(true);
            ingestionService = new KnowledgeIngestionService(
                    documentRepository, chunkRepository, chunkingService,
                    embeddingService, ragProperties);
        }

        @Test
        @DisplayName("embedding is generated for each chunk when enabled")
        void embeddingGeneratedForEachChunk() {
            String content = "Short content here";
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Policy", null, KnowledgeSourceType.POLICY, null, content);
            KnowledgeDocument saved = buildDocument(request, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.save(any())).thenReturn(saved);
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());
            // Return a 3-dim vector for each chunk text
            when(embeddingService.embedBatch(any()))
                    .thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));

            ingestionService.ingestDocument(request);

            verify(embeddingService).embedBatch(any());
            // chunkRepository.saveAll called at least twice: once for chunks, once for embeddings
            verify(chunkRepository, times(2)).saveAll(any());
        }

        @Test
        @DisplayName("document is ACTIVE after successful embedding")
        void documentActiveAfterEmbedding() {
            String content = "Policy content";
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Policy", null, KnowledgeSourceType.POLICY, null, content);
            KnowledgeDocument saved = buildDocument(request, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.save(any())).thenReturn(saved);
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());
            when(embeddingService.embedBatch(any()))
                    .thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));

            KnowledgeDocumentResponse response = ingestionService.ingestDocument(request);

            assertThat(response.status()).isEqualTo(KnowledgeDocumentStatus.ACTIVE);
        }

        @Test
        @DisplayName("document set to ERROR and RagException thrown when embedding fails")
        void embeddingFailureSetsErrorStatus() {
            String content = "Policy content";
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Policy", null, KnowledgeSourceType.POLICY, null, content);
            KnowledgeDocument processingDoc = buildDocument(request, KnowledgeDocumentStatus.PROCESSING);
            when(documentRepository.save(any())).thenReturn(processingDoc);
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());
            when(embeddingService.embedBatch(any()))
                    .thenThrow(new EmbeddingException("API down"));

            assertThatThrownBy(() -> ingestionService.ingestDocument(request))
                    .isInstanceOf(RagException.class)
                    .hasMessageContaining("embedding generation failed");

            // document.setStatus(ERROR) + save must have been called
            ArgumentCaptor<KnowledgeDocument> docCaptor =
                    ArgumentCaptor.forClass(KnowledgeDocument.class);
            verify(documentRepository, times(2)).save(docCaptor.capture());
            // The second save should be for the ERROR state
            List<KnowledgeDocument> savedDocs = docCaptor.getAllValues();
            assertThat(savedDocs.get(1).getStatus()).isEqualTo(KnowledgeDocumentStatus.ERROR);
        }

        @Test
        @DisplayName("embedding not called when embedding is disabled")
        void embeddingNotCalledWhenDisabled() {
            ragProperties.getEmbedding().setEnabled(false);
            ingestionService = new KnowledgeIngestionService(
                    documentRepository, chunkRepository, chunkingService,
                    embeddingService, ragProperties);

            String content = "Policy content";
            IngestDocumentRequest request = new IngestDocumentRequest(
                    "Policy", null, KnowledgeSourceType.POLICY, null, content);
            KnowledgeDocument saved = buildDocument(request, KnowledgeDocumentStatus.ACTIVE);
            when(documentRepository.save(any())).thenReturn(saved);
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());

            ingestionService.ingestDocument(request);

            verify(embeddingService, never()).embedBatch(any());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Phase 3 — Re-index
    // ─────────────────────────────────────────────────────────────────────────

    @Nested
    @DisplayName("Phase 3 — reindexDocument")
    class ReindexDocument {

        @BeforeEach
        void enableEmbedding() {
            ragProperties.getEmbedding().setEnabled(true);
            ingestionService = new KnowledgeIngestionService(
                    documentRepository, chunkRepository, chunkingService,
                    embeddingService, ragProperties);
        }

        @Test
        @DisplayName("reindexDocument generates embeddings for all chunks")
        void reindexGeneratesEmbeddings() {
            UUID docId = UUID.randomUUID();
            IngestDocumentRequest req = new IngestDocumentRequest(
                    "Policy", null, KnowledgeSourceType.POLICY, null, "content");
            KnowledgeDocument doc = buildDocument(req, KnowledgeDocumentStatus.ACTIVE);
            try {
                java.lang.reflect.Field idField =
                        com.company.employeemanagement.entity.BaseEntity.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(doc, docId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .id(UUID.randomUUID()).document(doc).chunkIndex(0)
                    .content("policy content").tokenCount(2).build();

            when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(docId))
                    .thenReturn(List.of(chunk));
            when(embeddingService.embedBatch(any()))
                    .thenReturn(List.of(new float[]{0.1f, 0.2f, 0.3f}));
            when(chunkRepository.saveAll(any())).thenReturn(Collections.emptyList());
            // documentRepository.save() is only called when status != ACTIVE;
            // doc is already ACTIVE so that stub is not needed here.

            KnowledgeDocumentResponse response = ingestionService.reindexDocument(docId);

            assertThat(response).isNotNull();
            verify(embeddingService).embedBatch(any());
        }

        @Test
        @DisplayName("reindexDocument throws RagException for unknown ID")
        void reindexUnknownIdThrows() {
            UUID id = UUID.randomUUID();
            when(documentRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> ingestionService.reindexDocument(id))
                    .isInstanceOf(RagException.class)
                    .hasMessageContaining(id.toString());
        }

        @Test
        @DisplayName("reindexDocument throws RagException when embedding fails")
        void reindexEmbeddingFailureThrows() {
            UUID docId = UUID.randomUUID();
            IngestDocumentRequest req = new IngestDocumentRequest(
                    "Policy", null, KnowledgeSourceType.POLICY, null, "content");
            KnowledgeDocument doc = buildDocument(req, KnowledgeDocumentStatus.ACTIVE);
            try {
                java.lang.reflect.Field idField =
                        com.company.employeemanagement.entity.BaseEntity.class.getDeclaredField("id");
                idField.setAccessible(true);
                idField.set(doc, docId);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            KnowledgeChunk chunk = KnowledgeChunk.builder()
                    .id(UUID.randomUUID()).document(doc).chunkIndex(0)
                    .content("policy content").tokenCount(2).build();

            when(documentRepository.findById(docId)).thenReturn(Optional.of(doc));
            when(chunkRepository.findByDocumentIdOrderByChunkIndex(docId))
                    .thenReturn(List.of(chunk));
            when(embeddingService.embedBatch(any()))
                    .thenThrow(new EmbeddingException("provider down"));
            when(documentRepository.save(any())).thenReturn(doc);

            assertThatThrownBy(() -> ingestionService.reindexDocument(docId))
                    .isInstanceOf(RagException.class)
                    .hasMessageContaining("Re-index embedding generation failed");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private KnowledgeDocument buildDocument(IngestDocumentRequest req,
                                             KnowledgeDocumentStatus status) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTitle(req.title());
        doc.setDescription(req.description());
        doc.setSourceType(req.sourceType());
        doc.setSourceName(req.sourceName());
        doc.setContent(req.content());
        doc.setStatus(status);
        // Simulate auditing fields
        doc.setCreatedAt(LocalDateTime.now());
        doc.setUpdatedAt(LocalDateTime.now());
        return doc;
    }
}
