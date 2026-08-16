package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.dto.IngestDocumentRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeDocumentResponse;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingException;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingService;
import com.company.employeemanagement.ai.rag.embedding.VectorSimilarity;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.exception.RagException;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
import com.company.employeemanagement.ai.rag.repository.KnowledgeDocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the ingestion of documents into the RAG knowledge base.
 *
 * <h2>Phase 3 updated ingestion flow</h2>
 * <ol>
 *   <li>Validate the request.</li>
 *   <li>Persist the {@link KnowledgeDocument} in {@code PROCESSING} state.</li>
 *   <li>Split the content into chunks via {@link DocumentChunkingService}.</li>
 *   <li>Persist the {@link KnowledgeChunk} records.</li>
 *   <li>Generate embeddings for all chunks (when embedding is enabled).</li>
 *   <li>Store embeddings on each chunk.</li>
 *   <li>Transition the document to {@code ACTIVE} — or {@code ERROR} if embedding fails.</li>
 * </ol>
 *
 * <p>If embedding generation fails the document is transitioned to {@code ERROR} and
 * a {@link RagException} is thrown. The document and its chunks remain in the database
 * so that they can be re-indexed via {@link #reindexDocument(UUID)}.
 *
 * <p>All database writes are wrapped in a single transaction. If chunk persistence
 * fails, the entire operation is rolled back.
 *
 * @author Employee Management Portal Team
 */
@Service
public class KnowledgeIngestionService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeIngestionService.class);

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository    chunkRepository;
    private final DocumentChunkingService     chunkingService;
    private final EmbeddingService            embeddingService;
    private final RagProperties               ragProperties;

    public KnowledgeIngestionService(
            final KnowledgeDocumentRepository documentRepository,
            final KnowledgeChunkRepository    chunkRepository,
            final DocumentChunkingService     chunkingService,
            final EmbeddingService            embeddingService,
            final RagProperties               ragProperties) {
        this.documentRepository = documentRepository;
        this.chunkRepository    = chunkRepository;
        this.chunkingService    = chunkingService;
        this.embeddingService   = embeddingService;
        this.ragProperties      = ragProperties;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ingests a new document into the knowledge base.
     *
     * @param request the ingest request (validated by the controller layer)
     * @return a response DTO representing the newly created document
     * @throws RagException if the content is blank after trimming or if embedding fails
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

        // ── 4. Generate and store embeddings ─────────────────────────────────
        if (ragProperties.getEmbedding().isEnabled()) {
            try {
                embedAndSaveChunks(chunks);
                log.info("Embeddings generated and stored for document '{}' ({} chunk(s))",
                        document.getTitle(), chunks.size());
            } catch (EmbeddingException e) {
                log.error("Embedding failed for document '{}': {}. "
                        + "Document set to ERROR. Re-index with reindexDocument().",
                        document.getTitle(), e.getMessage());
                document.setStatus(KnowledgeDocumentStatus.ERROR);
                documentRepository.save(document);
                throw new RagException(
                        "Document was ingested but embedding generation failed: " + e.getMessage(), e);
            }
        } else {
            log.debug("Embedding disabled — skipping embedding generation for '{}'",
                    document.getTitle());
        }

        // ── 5. Transition to ACTIVE ──────────────────────────────────────────
        document.setStatus(KnowledgeDocumentStatus.ACTIVE);
        document = documentRepository.save(document);

        return toResponse(document);
    }

    /**
     * Re-generates and stores embeddings for all chunks of the given document.
     *
     * <p>Use this method to embed documents that were ingested before Phase 3,
     * or to recover documents in {@code ERROR} state after a transient embedding failure.
     *
     * @param id the document UUID
     * @return a response DTO for the updated document
     * @throws RagException if the document does not exist, or if embedding fails
     */
    @Transactional
    public KnowledgeDocumentResponse reindexDocument(final UUID id) {
        final KnowledgeDocument document = documentRepository.findById(id)
                .orElseThrow(() -> new RagException("Knowledge document not found: " + id));

        final List<KnowledgeChunk> chunks =
                chunkRepository.findByDocumentIdOrderByChunkIndex(document.getId());
        if (chunks.isEmpty()) {
            throw new RagException("Document has no chunks and cannot be re-indexed: " + id);
        }

        try {
            embedAndSaveChunks(chunks);
            log.info("Re-index complete for document '{}' ({} chunk(s))",
                    document.getTitle(), chunks.size());
        } catch (EmbeddingException e) {
            log.error("Re-index embedding failed for document '{}': {}",
                    document.getTitle(), e.getMessage());
            document.setStatus(KnowledgeDocumentStatus.ERROR);
            documentRepository.save(document);
            throw new RagException("Re-index embedding generation failed: " + e.getMessage(), e);
        }

        // Ensure document is ACTIVE after successful re-index
        if (document.getStatus() != KnowledgeDocumentStatus.ACTIVE) {
            document.setStatus(KnowledgeDocumentStatus.ACTIVE);
            documentRepository.save(document);
        }
        return toResponse(document);
    }

    /**
     * Re-indexes all ACTIVE documents by regenerating embeddings for every chunk.
     *
     * <p>Use this after switching embedding providers (e.g., from the broken Groq
     * integration to Hugging Face) so that all stored vectors are regenerated with
     * the new provider and model.
     *
     * <p>Documents that fail to embed are transitioned to {@code ERROR} status and
     * skipped; processing continues for all remaining documents. A summary of
     * successes and failures is logged.
     *
     * @return list of response DTOs for all documents that were successfully re-indexed;
     *         documents that failed are excluded but their errors are logged
     */
    @Transactional
    public List<KnowledgeDocumentResponse> reindexAllActiveDocuments() {
        final List<KnowledgeDocument> activeDocuments =
                documentRepository.findByStatus(KnowledgeDocumentStatus.ACTIVE);

        if (activeDocuments.isEmpty()) {
            log.info("Re-index all: no ACTIVE documents found — nothing to do.");
            return List.of();
        }

        log.info("Re-index all: starting re-index of {} ACTIVE document(s).", activeDocuments.size());

        final List<KnowledgeDocumentResponse> succeeded = new ArrayList<>();
        int failureCount = 0;

        for (final KnowledgeDocument document : activeDocuments) {
            try {
                final List<KnowledgeChunk> chunks =
                        chunkRepository.findByDocumentIdOrderByChunkIndex(document.getId());
                if (chunks.isEmpty()) {
                    log.warn("Re-index all: document '{}' has no chunks — skipping.",
                            document.getTitle());
                    continue;
                }
                embedAndSaveChunks(chunks);
                log.info("Re-index all: document '{}' ({} chunk(s)) re-indexed successfully.",
                        document.getTitle(), chunks.size());
                succeeded.add(toResponse(document));
            } catch (EmbeddingException e) {
                failureCount++;
                log.error("Re-index all: embedding failed for document '{}': {}. "
                        + "Document set to ERROR.",
                        document.getTitle(), e.getMessage());
                document.setStatus(KnowledgeDocumentStatus.ERROR);
                documentRepository.save(document);
            }
        }

        log.info("Re-index all complete: {}/{} document(s) succeeded, {} failed.",
                succeeded.size(), activeDocuments.size(), failureCount);
        return succeeded;
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

    /**
     * Generates embeddings for a list of chunks and stores them.
     *
     * <p>Batches all chunk texts in a single API call for efficiency.
     *
     * @param chunks the chunks to embed (their {@code embeddingVector} fields are set in-place)
     * @throws EmbeddingException if the embedding provider fails
     */
    private void embedAndSaveChunks(final List<KnowledgeChunk> chunks) {
        final List<String> texts = chunks.stream()
                .map(KnowledgeChunk::getContent)
                .toList();

        final List<float[]> vectors = embeddingService.embedBatch(texts);

        for (int i = 0; i < chunks.size(); i++) {
            chunks.get(i).setEmbeddingVector(VectorSimilarity.toBytes(vectors.get(i)));
        }
        chunkRepository.saveAll(chunks);
    }

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
