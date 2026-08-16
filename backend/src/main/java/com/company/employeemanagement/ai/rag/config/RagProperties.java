package com.company.employeemanagement.ai.rag.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Externalized configuration for the RAG knowledge-base feature.
 *
 * <p>Properties are bound from the {@code ai.rag.*} namespace in
 * {@code application.properties}:
 * <pre>
 * ai.rag.enabled=true
 * ai.rag.chunk-size=1000
 * ai.rag.chunk-overlap=150
 * ai.rag.top-k=5
 * ai.rag.retrieval-strategy=vector
 * ai.rag.similarity-threshold=0.70
 * ai.rag.embedding.enabled=true
 * ai.rag.embedding.model=nomic-ai/nomic-embed-text-v1.5
 * </pre>
 *
 * @author Employee Management Portal Team
 */
@ConfigurationProperties(prefix = "ai.rag")
public class RagProperties {

    /** Whether the RAG knowledge-base feature is active. Defaults to {@code true}. */
    private boolean enabled = true;

    /**
     * Maximum number of characters per chunk when splitting document content.
     * Defaults to 1000.
     */
    private int chunkSize = 1000;

    /**
     * Number of characters of overlap between consecutive chunks.
     * Overlap preserves context across chunk boundaries.
     * Defaults to 150.
     */
    private int chunkOverlap = 150;

    /**
     * Maximum number of knowledge chunks to retrieve per AI chat request (Phase 2B).
     * Overridden by the {@code RAG_TOP_K} environment variable or
     * {@code ai.rag.top-k} property.
     * Defaults to 5.
     */
    private int topK = 5;

    /**
     * Retrieval strategy.
     * <ul>
     *   <li>{@code vector}   — semantic embedding similarity search (Phase 3, default)</li>
     *   <li>{@code database} — keyword LIKE search (Phase 2A fallback)</li>
     * </ul>
     * Overridden by the {@code RAG_RETRIEVAL_STRATEGY} environment variable.
     */
    private String retrievalStrategy = "vector";

    /**
     * Minimum cosine similarity threshold for a chunk to be included in retrieval results.
     * Chunks whose similarity to the query is below this value are excluded.
     * Prevents injecting unrelated context into the LLM prompt.
     * Overridden by the {@code RAG_SIMILARITY_THRESHOLD} environment variable.
     * Defaults to {@code 0.70}.
     */
    private double similarityThreshold = 0.70;

    /** Embedding sub-configuration. */
    private Embedding embedding = new Embedding();

    // ── Getters / Setters ─────────────────────────────────────────────────────

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getRetrievalStrategy() {
        return retrievalStrategy;
    }

    public void setRetrievalStrategy(String retrievalStrategy) {
        this.retrievalStrategy = retrievalStrategy;
    }

    public double getSimilarityThreshold() {
        return similarityThreshold;
    }

    public void setSimilarityThreshold(double similarityThreshold) {
        this.similarityThreshold = similarityThreshold;
    }

    public Embedding getEmbedding() {
        return embedding;
    }

    public void setEmbedding(Embedding embedding) {
        this.embedding = embedding;
    }

    // ── Nested embedding config ───────────────────────────────────────────────

    /**
     * Embedding provider configuration.
     */
    public static class Embedding {

        /**
         * Whether to generate and store embeddings during document ingestion.
         * Defaults to {@code true}. Set to {@code false} to disable embedding
         * generation (e.g., when GROQ_API_KEY is not available in test environments).
         */
        private boolean enabled = true;

        /**
         * The full Hugging Face model ID to use with the Inference API
         * feature-extraction pipeline.
         * Defaults to {@code nomic-ai/nomic-embed-text-v1.5} (768-dimensional).
         *
         * <p>Groq does not offer an embeddings endpoint.
         * {@link com.company.employeemanagement.ai.rag.embedding.HuggingFaceEmbeddingService}
         * calls the HF Inference Router using {@code HF_TOKEN}.
         *
         * Overridden by the {@code RAG_EMBEDDING_MODEL} environment variable.
         */
        private String model = "nomic-ai/nomic-embed-text-v1.5";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getModel() {
            return model;
        }

        public void setModel(String model) {
            this.model = model;
        }
    }
}
