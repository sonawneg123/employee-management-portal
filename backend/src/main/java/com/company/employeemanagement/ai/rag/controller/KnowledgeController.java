package com.company.employeemanagement.ai.rag.controller;

import com.company.employeemanagement.ai.rag.dto.IngestDocumentRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeDocumentResponse;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchRequest;
import com.company.employeemanagement.ai.rag.dto.KnowledgeSearchResult;
import com.company.employeemanagement.ai.rag.service.KnowledgeIngestionService;
import com.company.employeemanagement.ai.rag.service.KnowledgeRetrievalService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

/**
 * REST controller exposing RAG knowledge-base management and search endpoints.
 *
 * <h2>Endpoint summary</h2>
 * <ul>
 *   <li>{@code POST  /ai/rag/documents}      — ingest a document (ADMIN, HR)</li>
 *   <li>{@code GET   /ai/rag/documents}      — list all documents (ADMIN, HR)</li>
 *   <li>{@code GET   /ai/rag/documents/{id}} — get one document  (ADMIN, HR)</li>
 *   <li>{@code DELETE /ai/rag/documents/{id}} — delete a document (ADMIN)</li>
 *   <li>{@code POST  /ai/rag/search}         — keyword search    (any authenticated user)</li>
 * </ul>
 *
 * @author Employee Management Portal Team
 */
@RestController
@RequestMapping("/ai/rag")
public class KnowledgeController {

    private final KnowledgeIngestionService ingestionService;
    private final KnowledgeRetrievalService retrievalService;

    public KnowledgeController(final KnowledgeIngestionService ingestionService,
                                final KnowledgeRetrievalService retrievalService) {
        this.ingestionService = ingestionService;
        this.retrievalService = retrievalService;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Document management (ADMIN / HR)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Ingests a new document into the knowledge base.
     *
     * @param request validated ingest payload
     * @return the created document (HTTP 201)
     */
    @PostMapping("/documents")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<KnowledgeDocumentResponse> ingestDocument(
            @Valid @RequestBody final IngestDocumentRequest request) {
        KnowledgeDocumentResponse response = ingestionService.ingestDocument(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Returns all documents in the knowledge base.
     *
     * @return list of documents (HTTP 200)
     */
    @GetMapping("/documents")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<KnowledgeDocumentResponse>> listDocuments() {
        return ResponseEntity.ok(ingestionService.listAll());
    }

    /**
     * Returns a single document by ID.
     *
     * @param id document UUID
     * @return the document (HTTP 200) or 400 if not found
     */
    @GetMapping("/documents/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<KnowledgeDocumentResponse> getDocument(
            @PathVariable final UUID id) {
        return ResponseEntity.ok(ingestionService.getDocument(id));
    }

    /**
     * Deletes a document and all its chunks.
     *
     * @param id document UUID
     * @return HTTP 204 on success
     */
    @DeleteMapping("/documents/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteDocument(@PathVariable final UUID id) {
        ingestionService.deleteDocument(id);
        return ResponseEntity.noContent().build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Search (any authenticated user)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Searches the knowledge base using keyword matching.
     *
     * @param request validated search payload
     * @return ordered list of matching results (HTTP 200)
     */
    @PostMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<KnowledgeSearchResult>> search(
            @Valid @RequestBody final KnowledgeSearchRequest request) {
        return ResponseEntity.ok(retrievalService.search(request));
    }
}
