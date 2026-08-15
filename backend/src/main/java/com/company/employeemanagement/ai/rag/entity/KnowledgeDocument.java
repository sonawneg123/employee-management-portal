package com.company.employeemanagement.ai.rag.entity;

import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeDocumentStatus;
import com.company.employeemanagement.ai.rag.entity.enums.KnowledgeSourceType;
import com.company.employeemanagement.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a document stored in the RAG knowledge base.
 *
 * <p>Each document has a title, optional description, raw content, a source type
 * (classifying where the document came from), and a lifecycle status. The document
 * is chunked by {@link com.company.employeemanagement.ai.rag.service.KnowledgeIngestionService}
 * into {@link KnowledgeChunk} records for retrieval.
 *
 * @author Employee Management Portal Team
 */
@Entity
@Table(name = "knowledge_documents")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KnowledgeDocument extends BaseEntity {

    /**
     * Human-readable title of the document (e.g., "Remote Work Policy 2024").
     */
    @Column(name = "title", nullable = false, length = 500)
    private String title;

    /**
     * Optional short description summarising the document's purpose.
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * Category describing the origin or nature of the document.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private KnowledgeSourceType sourceType;

    /**
     * Optional name of the source system or file (e.g., "HR-Policy-2024.pdf").
     */
    @Column(name = "source_name", length = 500)
    private String sourceName;

    /**
     * Full text content of the document. Stored as a LONGTEXT column to
     * accommodate large documents.
     */
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    /**
     * Current lifecycle status of this document.
     */
    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private KnowledgeDocumentStatus status = KnowledgeDocumentStatus.PROCESSING;
}
