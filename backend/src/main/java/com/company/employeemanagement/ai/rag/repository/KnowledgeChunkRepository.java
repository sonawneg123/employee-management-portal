package com.company.employeemanagement.ai.rag.repository;

import com.company.employeemanagement.ai.rag.entity.KnowledgeChunk;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link KnowledgeChunk} persistence.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    /**
     * Returns all chunks for the given document, ordered by chunk index.
     */
    List<KnowledgeChunk> findByDocumentIdOrderByChunkIndex(UUID documentId);

    /**
     * Deletes all chunks belonging to the given document.
     * Called during document re-ingestion or deletion.
     */
    void deleteByDocumentId(UUID documentId);

    /**
     * Keyword search within chunk content, joining back to the parent document
     * to filter by ACTIVE status only.
     *
     * <p>Uses a named parameter for the status enum so that JPQL resolves it
     * correctly against the {@code @Enumerated(STRING)} field — comparing a quoted
     * string literal like {@code 'ACTIVE'} is not portable across JPA providers.
     */
    @Query("""
            SELECT c FROM KnowledgeChunk c
            JOIN c.document d
            WHERE d.status = :activeStatus
              AND LOWER(c.content) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY d.id, c.chunkIndex
            """)
    List<KnowledgeChunk> searchByContentKeyword(
            @Param("query") String query,
            @Param("activeStatus") KnowledgeDocumentStatus activeStatus);

    /**
     * Returns all chunks belonging to ACTIVE documents that have a stored embedding vector.
     * Used by {@link com.company.employeemanagement.ai.rag.service.VectorKnowledgeRetrievalService}
     * to perform in-memory cosine similarity search.
     */
    @Query("""
            SELECT c FROM KnowledgeChunk c
            JOIN FETCH c.document d
            WHERE d.status = :activeStatus
              AND c.embeddingVector IS NOT NULL
            ORDER BY d.id, c.chunkIndex
            """)
    List<KnowledgeChunk> findAllEmbeddedByDocumentStatus(
            @Param("activeStatus") KnowledgeDocumentStatus activeStatus);

    /**
     * Returns all chunks belonging to ACTIVE documents that do NOT yet have
     * an embedding vector. Used to identify documents that need to be re-indexed.
     */
    @Query("""
            SELECT c FROM KnowledgeChunk c
            JOIN c.document d
            WHERE d.status = :activeStatus
              AND c.embeddingVector IS NULL
            ORDER BY d.id, c.chunkIndex
            """)
    List<KnowledgeChunk> findAllWithoutEmbedding(
            @Param("activeStatus") KnowledgeDocumentStatus activeStatus);
}
