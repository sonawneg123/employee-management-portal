/**
 * @fileoverview RAG Knowledge Base API service.
 *
 * Calls the existing Phase 2A RAG ingestion and listing endpoints.
 * Documents submitted here go through KnowledgeIngestionService on the backend,
 * are stored as KnowledgeDocument + KnowledgeChunk, and automatically become
 * available to the Phase 2B RAG-grounded AI Assistant.
 *
 * Endpoint reused: POST /api/ai/rag/documents (KnowledgeController)
 * Endpoint reused: GET  /api/ai/rag/documents (KnowledgeController)
 */

import axiosInstance from '@/api/axiosInstance';
import { API_ENDPOINTS } from '@/constants/api';

/**
 * @typedef {Object} KnowledgeDocumentResponse
 * @property {string}      id
 * @property {string}      title
 * @property {string|null} description
 * @property {string}      sourceType
 * @property {string|null} sourceName
 * @property {string}      status       - "ACTIVE" | "PROCESSING" | "ERROR"
 * @property {string}      createdAt
 * @property {string}      updatedAt
 * @property {string|null} createdBy
 */

/**
 * @typedef {Object} IngestDocumentPayload
 * @property {string}      title       - Human-readable document title (required).
 * @property {string}      content     - Full text content of the document (required).
 * @property {string}      [description] - Optional short description.
 * @property {string}      [sourceType]  - Source classification; defaults to "POLICY".
 * @property {string}      [sourceName]  - Optional source filename.
 */

/**
 * Ingests a new document into the RAG knowledge base.
 *
 * The document goes through KnowledgeIngestionService, is stored as
 * KnowledgeDocument + KnowledgeChunk, and becomes available to the AI Assistant.
 *
 * @param {IngestDocumentPayload} payload
 * @returns {Promise<KnowledgeDocumentResponse>}
 */
export async function ingestDocument(payload) {
  const body = {
    title: payload.title,
    content: payload.content,
    description: payload.description ?? null,
    sourceType: payload.sourceType ?? 'POLICY',
    sourceName: payload.sourceName ?? null,
  };
  const { data } = await axiosInstance.post(API_ENDPOINTS.KNOWLEDGE_DOCUMENTS, body);
  return data;
}

/**
 * Returns all knowledge documents (any status).
 * Accessible by ADMIN and HR only (enforced server-side).
 *
 * @returns {Promise<KnowledgeDocumentResponse[]>}
 */
export async function listDocuments() {
  const { data } = await axiosInstance.get(API_ENDPOINTS.KNOWLEDGE_DOCUMENTS);
  return data;
}

/**
 * Deletes a knowledge document and all its chunks.
 * Accessible by ADMIN only (enforced server-side).
 *
 * @param {string} id - Document UUID.
 * @returns {Promise<void>}
 */
export async function deleteDocument(id) {
  await axiosInstance.delete(API_ENDPOINTS.KNOWLEDGE_DOCUMENT_BY_ID(id));
}
