package com.company.employeemanagement.ai.rag.service;

import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;

import java.util.List;

/**
 * Contract for RAG knowledge-base retrieval.
 *
 * <p>The interface is intentionally minimal and free of dependencies on any
 * specific storage technology, so that:
 * <ul>
 *   <li>The current {@link DatabaseKnowledgeRetrievalService} implementation can be
 *       replaced with a vector-index implementation in Phase 2B without changing
 *       any callers.</li>
 *   <li>{@code AiChatService} (Phase 2B) can inject this interface without
 *       creating a circular dependency — this interface lives in the
 *       {@code ai.rag.service} package, not in the chat package.</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
public interface KnowledgeRetrievalService {

    /**
     * Searches the knowledge base for content relevant to the given request.
     *
     * @param request the search parameters (query text, max results)
     * @return an ordered list of matching chunks; never {@code null}
     */
    List<KnowledgeSearchResult> search(KnowledgeSearchRequest request);
}
