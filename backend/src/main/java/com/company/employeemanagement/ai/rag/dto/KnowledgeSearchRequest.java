package com.company.employeemanagement.ai.rag.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for a keyword search over the RAG knowledge base.
 *
 * <p>Accepts {@code maxResults} or the alias {@code topK} as the result-count field so
 * that callers using either name are handled transparently.
 *
 * @param query      the search query string (required)
 * @param maxResults maximum number of results to return (1–50, default 10);
 *                   also accepted as {@code topK} for backwards compatibility
 *
 * @author Employee Management Portal Team
 */
public record KnowledgeSearchRequest(

        @NotBlank(message = "Search query must not be blank")
        String query,

        @JsonAlias("topK")
        @Min(value = 1, message = "maxResults must be at least 1")
        @Max(value = 50, message = "maxResults must not exceed 50")
        Integer maxResults
) {
    /**
     * Returns the effective max-results value, falling back to 10 when not supplied.
     */
    public int effectiveMaxResults() {
        return maxResults != null ? maxResults : 10;
    }
}
