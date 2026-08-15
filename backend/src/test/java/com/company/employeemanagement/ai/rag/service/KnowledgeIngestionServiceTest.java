package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.dto.IngestDocumentRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeDocumentResponse;
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

    private KnowledgeIngestionService ingestionService;
    private DocumentChunkingService chunkingService;

    @BeforeEach
    void setUp() {
        RagProperties props = new RagProperties();
        props.setChunkSize(50);
        props.setChunkOverlap(10);
        chunkingService = new DocumentChunkingService(props);
        ingestionService = new KnowledgeIngestionService(documentRepository, chunkRepository, chunkingService);
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
