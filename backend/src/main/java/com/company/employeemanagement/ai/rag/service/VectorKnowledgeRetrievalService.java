package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.config.RagProperties;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingException;
import com.company.employeemanagement.ai.rag.embedding.EmbeddingService;
import com.company.employeemanagement.ai.rag.embedding.VectorSimilarity;
import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.repository.KnowledgeChunkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Semantic vector implementation of {@link KnowledgeRetrievalService}.
 *
 * <h2>Retrieval algorithm</h2>
 * <ol>
 *   <li>Generate a dense embedding for the user query via {@link EmbeddingService}.</li>
 *   <li>Load all ACTIVE chunks that have a stored embedding vector.</li>
 *   <li>Compute cosine similarity between the query vector and each chunk vector.</li>
 *   <li>Filter out chunks whose similarity is below the configured threshold
 *       ({@link RagProperties#getSimilarityThreshold()}).</li>
 *   <li>Sort survivors by descending similarity and return the top-K results.</li>
 * </ol>
 *
 * <h2>Fallback behavior</h2>
 * If the embedding provider fails (e.g., API key not set, provider down), this
 * service throws {@link EmbeddingException} so that the caller
 * ({@link com.company.employeemanagement.ai.service.AiChatService}) can fall back
 * to the no-context path cleanly via its existing exception handler.
 *
 * <h2>No-embedding-yet behavior</h2>
 * Chunks without an embedding vector are excluded from semantic search. They are
 * identifiable via
 * {@link KnowledgeChunkRepository#findAllWithoutEmbedding(KnowledgeDocumentStatus)}.
 *
 * <p>This class is {@code @Transactional(readOnly = true)} — it only reads chunk
 * data and never modifies the knowledge base.
 *
 * @author Employee Management Portal Team
 */
@Service("vectorKnowledgeRetrievalService")
@Transactional(readOnly = true)
public class VectorKnowledgeRetrievalService implements KnowledgeRetrievalService {

    private static final Logger log = LoggerFactory.getLogger(VectorKnowledgeRetrievalService.class);

    private final KnowledgeChunkRepository chunkRepository;
    private final EmbeddingService         embeddingService;
    private final RagProperties            ragProperties;

    public VectorKnowledgeRetrievalService(
            final KnowledgeChunkRepository chunkRepository,
            final EmbeddingService         embeddingService,
            final RagProperties            ragProperties) {
        this.chunkRepository  = chunkRepository;
        this.embeddingService = embeddingService;
        this.ragProperties    = ragProperties;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Performs semantic vector retrieval using cosine similarity.
     * Returns at most {@link KnowledgeSearchRequest#effectiveMaxResults()} chunks
     * whose cosine similarity to the query exceeds the configured threshold.
     */
    @Override
    public List<KnowledgeSearchResult> search(final KnowledgeSearchRequest request) {
        final String query     = request.query().trim();
        final int    maxResults = request.effectiveMaxResults();
        final double threshold  = ragProperties.getSimilarityThreshold();

        // ── 1. Embed the query ────────────────────────────────────────────────
        log.debug("Generating query embedding for vector retrieval");
        final float[] queryVector = embeddingService.embed(query);

        // ── 2. Load all embedded ACTIVE chunks ───────────────────────────────
        final List<KnowledgeChunk> candidates =
                chunkRepository.findAllEmbeddedByDocumentStatus(KnowledgeDocumentStatus.ACTIVE);

        log.debug("Vector retrieval: {} candidate chunk(s) available for query '{}'",
                candidates.size(), abbreviate(query));

        if (candidates.isEmpty()) {
            log.info("No embedded chunks found in the knowledge base. "
                    + "Ensure documents are ingested with embeddings enabled.");
            return List.of();
        }

        // ── 3. Score all candidates ───────────────────────────────────────────
        final List<ScoredChunk> scored = new ArrayList<>(candidates.size());
        for (final KnowledgeChunk chunk : candidates) {
            try {
                final float[] chunkVector = VectorSimilarity.fromBytes(chunk.getEmbeddingVector());
                final double  similarity  = VectorSimilarity.cosineSimilarity(queryVector, chunkVector);
                scored.add(new ScoredChunk(chunk, similarity));
            } catch (IllegalArgumentException e) {
                log.warn("Skipping chunk {} — invalid or corrupt embedding vector: {}",
                        chunk.getId(), e.getMessage());
            }
        }

        // ── 4. Filter by threshold ────────────────────────────────────────────
        final long aboveThreshold = scored.stream().filter(s -> s.similarity >= threshold).count();
        log.debug("Vector retrieval: {}/{} chunk(s) above similarity threshold {}",
                aboveThreshold, scored.size(), threshold);

        // ── 5. Sort descending and take top-K ────────────────────────────────
        final List<KnowledgeSearchResult> results = scored.stream()
                .filter(s -> s.similarity >= threshold)
                .sorted(Comparator.comparingDouble(ScoredChunk::similarity).reversed())
                .limit(maxResults)
                .map(s -> toResult(s.chunk, s.similarity))
                .toList();

        log.info("Vector retrieval returned {} result(s) for query '{}'",
                results.size(), abbreviate(query));
        return results;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private KnowledgeSearchResult toResult(final KnowledgeChunk chunk, final double similarity) {
        // matchType encodes the similarity score for diagnostics (without leaking vectors)
        final String matchType = String.format("VECTOR(%.3f)", similarity);
        return new KnowledgeSearchResult(
                chunk.getDocument().getId(),
                chunk.getDocument().getTitle(),
                chunk.getId(),
                chunk.getChunkIndex(),
                chunk.getContent(),
                matchType
        );
    }

    /** Shortens a query string for log messages to avoid verbose output. */
    private static String abbreviate(final String s) {
        return s.length() <= 60 ? s : s.substring(0, 60) + "…";
    }

    /** Simple carrier to pair a chunk with its computed similarity score. */
    private record ScoredChunk(KnowledgeChunk chunk, double similarity) { }
}
