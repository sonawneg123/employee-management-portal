package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
import com.company.employeemanagement.ai.rag.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Database-backed implementation of {@link KnowledgeRetrievalService}.
 *
 * <p>Uses case-insensitive LIKE queries over the {@code knowledge_documents} and
 * {@code knowledge_chunks} tables. Title matches are surfaced before content
 * matches so that the most relevant documents appear at the top.
 *
 * <p>This implementation is designed to be <em>replaced</em> in Phase 2B by a
 * vector-index-backed service without any changes to callers — the contract is
 * defined purely through the {@link KnowledgeRetrievalService} interface.
 *
 * @author Employee Management Portal Team
 */
@Service
@Transactional(readOnly = true)
public class DatabaseKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;

    public DatabaseKnowledgeRetrievalService(
            final KnowledgeDocumentRepository documentRepository,
            final KnowledgeChunkRepository chunkRepository) {
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Search strategy:
     * <ol>
     *   <li>Title-match documents: return the first chunk of each matching doc
     *       with matchType {@code "TITLE"}.</li>
     *   <li>Content-match chunks: for chunks whose content matches the query
     *       (and whose document was not already included via a title match),
     *       add them with matchType {@code "CONTENT"}.</li>
     *   <li>Results are deduplicated by chunk ID and truncated to
     *       {@link KnowledgeSearchRequest#effectiveMaxResults()}.</li>
     * </ol>
     */
    @Override
    public List<KnowledgeSearchResult> search(final KnowledgeSearchRequest request) {
        final String query = request.query().trim();
        final int maxResults = request.effectiveMaxResults();

        // Use a LinkedHashMap keyed by chunk UUID to deduplicate while preserving order
        final Map<String, KnowledgeSearchResult> results = new LinkedHashMap<>();

        // ── 1. Title matches ────────────────────────────────────────────────
        final List<KnowledgeDocument> titleMatches =
                documentRepository.searchByKeyword(query, KnowledgeDocumentStatus.ACTIVE);
        for (KnowledgeDocument doc : titleMatches) {
            if (results.size() >= maxResults) {
                break;
            }
            // Return the first chunk of a title-matched document as the representative passage
            List<KnowledgeChunk> chunks = chunkRepository.findByDocumentIdOrderByChunkIndex(doc.getId());
            if (!chunks.isEmpty()) {
                KnowledgeChunk first = chunks.get(0);
                results.put(first.getId().toString(), toResult(doc, first, "TITLE"));
            }
        }

        // ── 2. Content matches ───────────────────────────────────────────────
        if (results.size() < maxResults) {
            final List<KnowledgeChunk> contentMatches =
                    chunkRepository.searchByContentKeyword(query, KnowledgeDocumentStatus.ACTIVE);
            for (KnowledgeChunk chunk : contentMatches) {
                if (results.size() >= maxResults) {
                    break;
                }
                String key = chunk.getId().toString();
                if (!results.containsKey(key)) {
                    results.put(key, toResult(chunk.getDocument(), chunk, "CONTENT"));
                }
            }
        }

        return new ArrayList<>(results.values());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private helpers
    // ─────────────────────────────────────────────────────────────────────────

    private KnowledgeSearchResult toResult(final KnowledgeDocument doc,
                                            final KnowledgeChunk chunk,
                                            final String matchType) {
        return new KnowledgeSearchResult(
                doc.getId(),
                doc.getTitle(),
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                matchType
        );
    }
}
