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
}
