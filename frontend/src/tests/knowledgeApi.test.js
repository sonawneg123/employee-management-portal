/**
 * @fileoverview Tests for knowledgeApi service.
 *
 * Verifies that:
 * - ingestDocument POSTs to the correct endpoint with expected payload
 * - listDocuments GETs from the correct endpoint
 * - deleteDocument DELETEs the correct endpoint
 * - Correct field names are used (title, content, sourceType — matching IngestDocumentRequest)
 * - Default sourceType is 'POLICY' when not provided
 */

import { describe, it, expect, vi, beforeEach } from 'vitest';

// ── Mock axiosInstance ────────────────────────────────────────────────────────
vi.mock('@/api/axiosInstance', () => ({
  default: {
    post: vi.fn(),
    get: vi.fn(),
    delete: vi.fn(),
  },
}));
import axiosInstance from '@/api/axiosInstance';
import { ingestDocument, listDocuments, deleteDocument } from '@/services/knowledgeApi';

const ENDPOINT_DOCS = '/ai/rag/documents';

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('knowledgeApi', () => {
  beforeEach(() => vi.clearAllMocks());

  // ── ingestDocument ────────────────────────────────────────────────────────

  describe('ingestDocument', () => {
    it('POSTs to /ai/rag/documents', async () => {
      axiosInstance.post.mockResolvedValue({
        data: { id: '1', title: 'Policy', status: 'ACTIVE' },
      });

      await ingestDocument({ title: 'Policy', content: 'Policy text.' });

      expect(axiosInstance.post).toHaveBeenCalledWith(ENDPOINT_DOCS, expect.any(Object));
    });

    it('sends title and content in the request body', async () => {
      axiosInstance.post.mockResolvedValue({ data: { id: '1' } });

      await ingestDocument({ title: 'Leave Policy', content: 'Leave content here.' });

      const [, body] = axiosInstance.post.mock.calls[0];
      expect(body).toMatchObject({
        title: 'Leave Policy',
        content: 'Leave content here.',
      });
    });

    it('defaults sourceType to "POLICY" when not provided', async () => {
      axiosInstance.post.mockResolvedValue({ data: { id: '1' } });

      await ingestDocument({ title: 'Test', content: 'Body.' });

      const [, body] = axiosInstance.post.mock.calls[0];
      expect(body.sourceType).toBe('POLICY');
    });

    it('uses provided sourceType when given', async () => {
      axiosInstance.post.mockResolvedValue({ data: { id: '1' } });

      await ingestDocument({ title: 'FAQ', content: 'FAQ text.', sourceType: 'FAQ' });

      const [, body] = axiosInstance.post.mock.calls[0];
      expect(body.sourceType).toBe('FAQ');
    });

    it('includes optional description when provided', async () => {
      axiosInstance.post.mockResolvedValue({ data: { id: '1' } });

      await ingestDocument({
        title: 'Policy',
        content: 'Body.',
        description: 'Short summary.',
      });

      const [, body] = axiosInstance.post.mock.calls[0];
      expect(body.description).toBe('Short summary.');
    });

    it('sends null description when not provided', async () => {
      axiosInstance.post.mockResolvedValue({ data: { id: '1' } });

      await ingestDocument({ title: 'Policy', content: 'Body.' });

      const [, body] = axiosInstance.post.mock.calls[0];
      expect(body.description).toBeNull();
    });

    it('returns the response data from the API', async () => {
      const mockResponse = { id: 'uuid-1', title: 'Policy', status: 'ACTIVE' };
      axiosInstance.post.mockResolvedValue({ data: mockResponse });

      const result = await ingestDocument({ title: 'Policy', content: 'Body.' });

      expect(result).toEqual(mockResponse);
    });

    it('does not use "documentTitle" — must use "title"', async () => {
      // Verifies exact field name expected by IngestDocumentRequest backend record
      axiosInstance.post.mockResolvedValue({ data: { id: '1' } });

      await ingestDocument({ title: 'Policy', content: 'Body.' });

      const [, body] = axiosInstance.post.mock.calls[0];
      expect(body).toHaveProperty('title');
      expect(body).not.toHaveProperty('documentTitle');
    });
  });

  // ── listDocuments ─────────────────────────────────────────────────────────

  describe('listDocuments', () => {
    it('GETs /ai/rag/documents', async () => {
      axiosInstance.get.mockResolvedValue({ data: [] });

      await listDocuments();

      expect(axiosInstance.get).toHaveBeenCalledWith(ENDPOINT_DOCS);
    });

    it('returns the response data array', async () => {
      const docs = [{ id: '1', title: 'Policy', status: 'ACTIVE' }];
      axiosInstance.get.mockResolvedValue({ data: docs });

      const result = await listDocuments();

      expect(result).toEqual(docs);
    });
  });

  // ── deleteDocument ────────────────────────────────────────────────────────

  describe('deleteDocument', () => {
    it('DELETEs /ai/rag/documents/:id', async () => {
      axiosInstance.delete.mockResolvedValue({ data: undefined });

      await deleteDocument('doc-uuid-123');

      expect(axiosInstance.delete).toHaveBeenCalledWith(`${ENDPOINT_DOCS}/doc-uuid-123`);
    });
  });
});
