package com.company.employeemanagement.ai.rag.repository;

import com.company.employeemanagement.ai.rag.entity.KnowledgeDocument;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for {@link KnowledgeDocument} persistence.
 *
 * @author Employee Management Portal Team
 */
@Repository
public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    /**
     * Returns all documents with the given status.
     */
    List<KnowledgeDocument> findByStatus(KnowledgeDocumentStatus status);

    /**
     * Full-text keyword search across title and content.
     * Case-insensitive LIKE query used as a simple keyword matcher until a
     * vector index is introduced in Phase 2B.
     *
     * <p>Uses a named parameter for the status enum so that JPQL resolves it
     * correctly against the {@code @Enumerated(STRING)} field — comparing a quoted
     * string literal like {@code 'ACTIVE'} is not portable across JPA providers.
     */
    @Query("""
            SELECT d FROM KnowledgeDocument d
            WHERE d.status = :activeStatus
              AND (LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%'))
                OR LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%')))
            """)
    List<KnowledgeDocument> searchByKeyword(
            @Param("query") String query,
            @Param("activeStatus") KnowledgeDocumentStatus activeStatus);
}
