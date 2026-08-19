/**
 * @fileoverview Tests for CompanyPolicyList component.
 *
 * Scenarios:
 *  Rendering:
 *   - Shows "no documents" message when list is empty
 *   - Renders document title in table
 *   - Renders document status chip
 *   - Renders document source type
 *   - Renders document created date
 *   - Shows refresh button
 *
 *  Admin role:
 *   - Admin sees delete button for each document
 *
 *  HR role:
 *   - HR does not see delete button
 *
 *  Loading:
 *   - Shows loading indicator in refresh button while loading
 *
 *  Error:
 *   - Shows error alert when error prop is set
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material/styles';

import CompanyPolicyList from '@/components/knowledge/CompanyPolicyList';

// ── Mock Auth context ─────────────────────────────────────────────────────────
vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));
import { useAuth } from '@/contexts/AuthContext';

// ── Mock knowledgeApi ─────────────────────────────────────────────────────────
vi.mock('@/services/knowledgeApi', () => ({
  ingestDocument: vi.fn(),
  listDocuments: vi.fn(),
  deleteDocument: vi.fn(),
}));
import { deleteDocument } from '@/services/knowledgeApi';

// ── Helpers ───────────────────────────────────────────────────────────────────
const theme = createTheme();

function renderList(props = {}, { isAdmin = false } = {}) {
  useAuth.mockReturnValue({
    hasAnyRole: (roles) => isAdmin && roles.includes('ROLE_ADMIN'),
    user: { firstName: 'Test', lastName: 'User', roles: [isAdmin ? 'ROLE_ADMIN' : 'ROLE_HR'] },
  });

  const defaults = {
    documents: [],
    loading: false,
    error: '',
    onRefresh: vi.fn(),
  };
  return render(
    <ThemeProvider theme={theme}>
      <CompanyPolicyList {...defaults} {...props} />
    </ThemeProvider>,
  );
}

const MOCK_DOC = {
  id: 'uuid-1',
  title: 'Employee Leave Policy',
  sourceType: 'POLICY',
  status: 'ACTIVE',
  description: null,
  createdAt: '2025-06-01T10:00:00',
  updatedAt: '2025-06-01T10:00:00',
  createdBy: 'admin@company.com',
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('CompanyPolicyList', () => {
  beforeEach(() => vi.clearAllMocks());

  // ── Empty state ───────────────────────────────────────────────────────────

  describe('Empty state', () => {
    it('shows "no documents" message when list is empty', () => {
      renderList({ documents: [] });
      expect(screen.getByText(/no documents yet/i)).toBeInTheDocument();
    });

    it('does not render a table when documents list is empty', () => {
      renderList({ documents: [] });
      expect(screen.queryByRole('table')).not.toBeInTheDocument();
    });
  });

  // ── Document table ────────────────────────────────────────────────────────

  describe('Document table', () => {
    it('renders a table when documents are present', () => {
      renderList({ documents: [MOCK_DOC] });
      expect(screen.getByRole('table')).toBeInTheDocument();
    });

    it('renders the document title', () => {
      renderList({ documents: [MOCK_DOC] });
      expect(screen.getByText('Employee Leave Policy')).toBeInTheDocument();
    });

    it('renders the document status chip', () => {
      renderList({ documents: [MOCK_DOC] });
      expect(screen.getByText('Active')).toBeInTheDocument();
    });

    it('renders the source type in a human-readable form', () => {
      renderList({ documents: [MOCK_DOC] });
      expect(screen.getByText('policy')).toBeInTheDocument();
    });

    it('renders the created date', () => {
      renderList({ documents: [MOCK_DOC] });
      // Date formatting is locale-dependent; just check it is rendered
      expect(screen.getByText(/2025/)).toBeInTheDocument();
    });

    it('renders multiple documents as multiple rows', () => {
      const docs = [
        { ...MOCK_DOC, id: '1', title: 'Leave Policy' },
        { ...MOCK_DOC, id: '2', title: 'Remote Work Policy' },
      ];
      renderList({ documents: docs });
      expect(screen.getByText('Leave Policy')).toBeInTheDocument();
      expect(screen.getByText('Remote Work Policy')).toBeInTheDocument();
    });
  });

  // ── Refresh button ────────────────────────────────────────────────────────

  describe('Refresh button', () => {
    it('renders the refresh button', () => {
      renderList();
      expect(screen.getByRole('button', { name: /refresh document list/i })).toBeInTheDocument();
    });

    it('calls onRefresh when the refresh button is clicked', () => {
      const onRefresh = vi.fn();
      renderList({ onRefresh });
      fireEvent.click(screen.getByRole('button', { name: /refresh document list/i }));
      expect(onRefresh).toHaveBeenCalledOnce();
    });

    it('disables refresh button while loading', () => {
      renderList({ loading: true });
      expect(screen.getByRole('button', { name: /refresh document list/i })).toBeDisabled();
    });
  });

  // ── Error state ───────────────────────────────────────────────────────────

  describe('Error state', () => {
    it('shows error alert when error prop is set', () => {
      renderList({ error: 'Failed to load documents.' });
      expect(screen.getByRole('alert')).toHaveTextContent('Failed to load documents.');
    });
  });

  // ── Admin: delete action ──────────────────────────────────────────────────

  describe('Admin role — delete action', () => {
    it('admin sees delete button for each document', () => {
      renderList({ documents: [MOCK_DOC] }, { isAdmin: true });
      expect(
        screen.getByRole('button', { name: /delete employee leave policy/i }),
      ).toBeInTheDocument();
    });

    it('calls deleteDocument and onRefresh after confirmed delete', async () => {
      // window.confirm → true
      vi.spyOn(window, 'confirm').mockReturnValue(true);
      deleteDocument.mockResolvedValue(undefined);
      const onRefresh = vi.fn();

      renderList({ documents: [MOCK_DOC], onRefresh }, { isAdmin: true });
      fireEvent.click(screen.getByRole('button', { name: /delete employee leave policy/i }));

      await waitFor(() => {
        expect(deleteDocument).toHaveBeenCalledWith('uuid-1');
        expect(onRefresh).toHaveBeenCalledOnce();
      });
    });

    it('does not call deleteDocument when user cancels confirm', () => {
      vi.spyOn(window, 'confirm').mockReturnValue(false);
      const onRefresh = vi.fn();
      renderList({ documents: [MOCK_DOC], onRefresh }, { isAdmin: true });
      fireEvent.click(screen.getByRole('button', { name: /delete employee leave policy/i }));
      expect(deleteDocument).not.toHaveBeenCalled();
    });
  });

  // ── HR role: no delete action ─────────────────────────────────────────────

  describe('HR role — no delete action', () => {
    it('HR user does not see delete button', () => {
      renderList({ documents: [MOCK_DOC] }, { isAdmin: false });
      expect(
        screen.queryByRole('button', { name: /delete employee leave policy/i }),
      ).not.toBeInTheDocument();
    });
  });
});
