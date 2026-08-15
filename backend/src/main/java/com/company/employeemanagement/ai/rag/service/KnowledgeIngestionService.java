package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.dto.IngestDocumentRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeDocumentResponse;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.exception.RagException;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
import com.company.employeemanagement.ai.rag.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the ingestion of documents into the RAG knowledge base.
 *
 * <p>A single call to {@link #ingestDocument(IngestDocumentRequest)} atomically:
 * <ol>
 *   <li>Validates the request.</li>
 *   <li>Persists the {@link KnowledgeDocument} in {@code PROCESSING} state.</li>
 *   <li>Splits the content into chunks via {@link DocumentChunkingService}.</li>
 *   <li>Persists the resulting {@link KnowledgeChunk} records.</li>
 *   <li>Transitions the document to {@code ACTIVE} (or {@code ERROR} on failure).</li>
 * </ol>
 *
 * <p>All database writes are wrapped in a single transaction. If chunk persistence
 * fails, the entire operation is rolled back.
 *
 * @author Employee Management Portal Team
 */
@Service
public class KnowledgeIngestionService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final DocumentChunkingService chunkingService;

    public KnowledgeIngestionService(
            final KnowledgeDocumentRepository documentRepository,
            final KnowledgeChunkRepository chunkRepository,
            final DocumentChunkingService chunkingService) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.chunkingService = chunkingService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ingests a new document into the knowledge base.
     *
     * @param request the ingest request (validated by the controller layer)
     * @return a response DTO representing the newly created document
     * @throws RagException if the content is blank after trimming
     */
    @Transactional
    public KnowledgeDocumentResponse ingestDocument(final IngestDocumentRequest request) {
        final String content = request.content().trim();
        if (content.isEmpty()) {
            throw new RagException("Document content must not be blank after trimming.");
        }

        // ── 1. Persist the document in PROCESSING state ─────────────────────
        KnowledgeDocument document = KnowledgeDocument.builder()
                .title(request.title().trim())
                .description(request.description())
                .sourceType(request.sourceType())
                .sourceName(request.sourceName())
                .content(content)
                .status(KnowledgeDocumentStatus.PROCESSING)
                .build();
        document = documentRepository.save(document);

        // ── 2. Chunk the content ─────────────────────────────────────────────
        final List<String> chunkTexts = chunkingService.chunk(content);

        // ── 3. Persist chunks ────────────────────────────────────────────────
        final List<KnowledgeChunk> chunks = new ArrayList<>(chunkTexts.size());
        for (int i = 0; i < chunkTexts.size(); i++) {
            final String chunkText = chunkTexts.get(i);
            chunks.add(KnowledgeChunk.builder()
                    .document(document)
                    .chunkIndex(i)
                    .content(chunkText)
                    .tokenCount(chunkingService.estimateTokenCount(chunkText))
                    .build());
        }
        chunkRepository.saveAll(chunks);

        // ── 4. Transition to ACTIVE ──────────────────────────────────────────
        document.setStatus(KnowledgeDocumentStatus.ACTIVE);
        document = documentRepository.save(document);

        return toResponse(document);
    }

    /**
     * Returns all documents regardless of status.
     */
    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> listAll() {
        return documentRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Returns a single document by ID, or throws if not found.
     *
     * @param id document UUID
     * @return response DTO
     * @throws RagException if no document exists with the given ID
     */
    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse getDocument(final UUID id) {
        return documentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new RagException("Knowledge document not found: " + id));
    }

    /**
     * Deletes a document and all of its chunks.
     *
     * @param id document UUID
     * @throws RagException if no document exists with the given ID
     */
    @Transactional
    public void deleteDocument(final UUID id) {
        if (!documentRepository.existsById(id)) {
            throw new RagException("Knowledge document not found: " + id);
        }
        chunkRepository.deleteByDocumentId(id);
        documentRepository.deleteById(id);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private KnowledgeDocumentResponse toResponse(final KnowledgeDocument doc) {
        return new KnowledgeDocumentResponse(
                doc.getId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getSourceType(),
                doc.getSourceName(),
                doc.getStatus(),
                doc.getCreatedAt(),
                doc.getUpdatedAt(),
                doc.getCreatedBy()
        );
    }
}
