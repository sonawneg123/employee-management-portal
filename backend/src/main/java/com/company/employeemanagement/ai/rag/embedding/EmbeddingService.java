package com.company.employeemanagement.ai.rag.embedding;

import java.util.List;

/**
 * Contract for generating dense vector embeddings from text.
 *
 * <p>Implementations are responsible for:
 * <ul>
 *   <li>Calling the configured embedding provider API.</li>
 *   <li>Returning a normalised float vector for each text input.</li>
 *   <li>Handling provider errors and translating them to {@link EmbeddingException}.</li>
 *   <li>Never logging API keys or raw embedding vectors.</li>
 * </ul>
 *
 * <p>This interface is intentionally free of HTTP/provider details so that
 * {@code VectorKnowledgeRetrievalService} and {@code KnowledgeIngestionService}
 * remain decoupled from any specific embedding provider.
 *
 * @author Employee Management Portal Team
 */
public interface EmbeddingService {

    /**
     * Generates an embedding for a single text input.
     *
     * @param text the text to embed; must not be {@code null} or blank
     * @return a non-empty float array representing the dense embedding vector
     * @throws EmbeddingException if the embedding provider fails or is misconfigured
     */
    float[] embed(String text);

    /**
     * Generates embeddings for a batch of text inputs.
     *
     * @param texts the texts to embed; must not be {@code null} or empty
     * @return a list of float arrays, one per input text, in the same order
     * @throws EmbeddingException if the embedding provider fails or is misconfigured
     */
    List<float[]> embedBatch(List<String> texts);
}
