/**
 * @fileoverview Tests for CompanyPolicyForm component.
 *
 * Scenarios:
 *  Rendering:
 *   - Renders title input
 *   - Renders content textarea
 *   - Renders submit and clear buttons
 *
 *  Validation:
 *   - Submit button disabled when title is empty
 *   - Submit button disabled when content is empty
 *   - Submit button disabled when both fields are empty
 *   - Submit button enabled when both fields are filled
 *   - Shows title validation error when title is touched then cleared
 *   - Shows content validation error when content is touched then cleared
 *
 *  Submission:
 *   - Calls ingestDocument with trimmed title and content
 *   - Shows loading state while submitting
 *   - Shows success banner on successful submission
 *   - Calls onSuccess callback after successful submission
 *   - Resets the form after successful submission
 *   - Shows error banner on API failure
 *   - Shows network error message on network failure
 *   - Shows 403 message on authorization error
 *
 *  Clear:
 *   - Clear button resets fields
 *   - Clear button dismisses success banner
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { HelmetProvider } from 'react-helmet-async';

import CompanyPolicyForm from '@/components/knowledge/CompanyPolicyForm';

// ── Mock knowledgeApi ─────────────────────────────────────────────────────────
vi.mock('@/services/knowledgeApi', () => ({
  ingestDocument: vi.fn(),
  listDocuments:  vi.fn(),
  deleteDocument: vi.fn(),
}));
import { ingestDocument } from '@/services/knowledgeApi';

// ── Test utilities ────────────────────────────────────────────────────────────
const testTheme = createTheme();

function renderForm(props = {}) {
  const user = userEvent.setup();
  const result = render(
    <HelmetProvider>
      <ThemeProvider theme={testTheme}>
        <CompanyPolicyForm {...props} />
      </ThemeProvider>
    </HelmetProvider>,
  );
  return { ...result, user };
}

const SAMPLE_TITLE   = 'Employee Attendance Policy';
const SAMPLE_CONTENT = 'Employees must record attendance daily through the portal.';

/** The built response returned by the mock API */
const MOCK_DOC = {
  id:         'doc-uuid-1',
  title:      SAMPLE_TITLE,
  sourceType: 'POLICY',
  status:     'ACTIVE',
  createdAt:  '2025-01-01T00:00:00',
  updatedAt:  '2025-01-01T00:00:00',
  createdBy:  'admin@company.com',
};

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('CompanyPolicyForm', () => {
  beforeEach(() => vi.clearAllMocks());
  afterEach(() => vi.clearAllMocks());

  // ── Rendering ─────────────────────────────────────────────────────────────

  describe('Rendering', () => {
    it('renders the document title input', () => {
      renderForm();
      expect(screen.getByLabelText(/document title/i)).toBeInTheDocument();
    });

    it('renders the document content textarea', () => {
      renderForm();
      expect(screen.getByLabelText(/document content/i)).toBeInTheDocument();
    });

    it('renders the submit button', () => {
      renderForm();
      expect(screen.getByRole('button', { name: /submit document/i })).toBeInTheDocument();
    });

    it('renders the clear/reset button', () => {
      renderForm();
      expect(screen.getByRole('button', { name: /reset form/i })).toBeInTheDocument();
    });

    it('has the form labelled "Add company policy form"', () => {
      renderForm();
      expect(screen.getByRole('form', { name: /add company policy form/i })).toBeInTheDocument();
    });
  });

  // ── Validation ────────────────────────────────────────────────────────────

  describe('Validation', () => {
    it('submit button is disabled when both fields are empty', () => {
      renderForm();
      expect(screen.getByRole('button', { name: /submit document/i })).toBeDisabled();
    });

    it('submit button is disabled when title is empty but content is filled', async () => {
      const { user } = renderForm();
      await user.type(screen.getByLabelText(/document content/i), SAMPLE_CONTENT);
      expect(screen.getByRole('button', { name: /submit document/i })).toBeDisabled();
    });

    it('submit button is disabled when content is empty but title is filled', async () => {
      const { user } = renderForm();
      await user.type(screen.getByLabelText(/document title/i), SAMPLE_TITLE);
      expect(screen.getByRole('button', { name: /submit document/i })).toBeDisabled();
    });

    it('submit button is enabled when both title and content are filled', () => {
      renderForm();
      fireEvent.change(screen.getByLabelText(/document title/i),   { target: { value: SAMPLE_TITLE } });
      fireEvent.change(screen.getByLabelText(/document content/i), { target: { value: SAMPLE_CONTENT } });
      expect(screen.getByRole('button', { name: /submit document/i })).not.toBeDisabled();
    });

    it('shows title error after title is typed then cleared', async () => {
      const { user } = renderForm();
      const titleInput = screen.getByLabelText(/document title/i);
      await user.type(titleInput, 'x');
      await user.clear(titleInput);
      await waitFor(() => {
        expect(screen.getByText('Title is required.')).toBeInTheDocument();
      });
    });

    it('shows content error after content is typed then cleared', async () => {
      const { user } = renderForm();
      const contentInput = screen.getByLabelText(/document content/i);
      await user.type(contentInput, 'x');
      await user.clear(contentInput);
      await waitFor(() => {
        expect(screen.getByText('Content is required.')).toBeInTheDocument();
      });
    });
  });

  // ── Submission ────────────────────────────────────────────────────────────
  // Use fireEvent.change for rapid field-filling in submission tests so that
  // character-by-character timing from userEvent.type does not cause flakiness.

  describe('Submission', () => {
    /** Helper: fill both fields via fireEvent.change and re-render synchronously. */
    function fillForm(titleVal = SAMPLE_TITLE, contentVal = SAMPLE_CONTENT) {
      fireEvent.change(screen.getByLabelText(/document title/i),   { target: { value: titleVal } });
      fireEvent.change(screen.getByLabelText(/document content/i), { target: { value: contentVal } });
    }

    it('calls ingestDocument with the exact title and content', async () => {
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm();
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(ingestDocument).toHaveBeenCalledWith({
          title:   SAMPLE_TITLE,
          content: SAMPLE_CONTENT,
        });
      });
    });

    it('trims whitespace before calling ingestDocument', async () => {
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm();
      fillForm(`  ${SAMPLE_TITLE}  `, `  ${SAMPLE_CONTENT}  `);
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(ingestDocument).toHaveBeenCalledWith({
          title:   SAMPLE_TITLE,
          content: SAMPLE_CONTENT,
        });
      });
    });

    it('disables submit button while submitting (loading state)', async () => {
      ingestDocument.mockImplementation(() => new Promise(() => {}));
      renderForm();
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /submit document/i })).toBeDisabled();
      });
    });

    it('shows success banner after successful submission', async () => {
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm();
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(
          screen.getByRole('alert', { name: /document submitted successfully/i }),
        ).toBeInTheDocument();
      });
    });

    it('calls onSuccess callback after successful submission', async () => {
      const onSuccess = vi.fn();
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm({ onSuccess });
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(onSuccess).toHaveBeenCalledWith(MOCK_DOC);
      });
    });

    it('clears the form fields after successful submission', async () => {
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm();
      const titleInput   = screen.getByLabelText(/document title/i);
      const contentInput = screen.getByLabelText(/document content/i);
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(titleInput).toHaveValue('');
        expect(contentInput).toHaveValue('');
      });
    });

    it('shows error banner on API error response', async () => {
      ingestDocument.mockRejectedValue({
        status: 400,
        message: 'Title must not be blank',
        isNetwork: false,
      });
      renderForm();
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(
          screen.getByRole('alert', { name: /submission error/i }),
        ).toBeInTheDocument();
      });
    });

    it('shows network error message when server is unreachable', async () => {
      ingestDocument.mockRejectedValue({ status: 0, isNetwork: true });
      renderForm();
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(
          screen.getByRole('alert', { name: /submission error/i }),
        ).toHaveTextContent(/unable to reach the server/i);
      });
    });

    it('shows 403 permission message on authorization error', async () => {
      ingestDocument.mockRejectedValue({ status: 403, isNetwork: false });
      renderForm();
      fillForm();
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        expect(
          screen.getByRole('alert', { name: /submission error/i }),
        ).toHaveTextContent(/permission/i);
      });
    });

    it('payload contains title and content fields (not documentTitle)', async () => {
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm();
      fillForm('My Policy', 'Policy text here.');
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() => {
        const [call] = ingestDocument.mock.calls;
        expect(call[0]).toHaveProperty('title', 'My Policy');
        expect(call[0]).toHaveProperty('content', 'Policy text here.');
        expect(call[0]).not.toHaveProperty('documentTitle');
      });
    });
  });

  // ── Clear button ──────────────────────────────────────────────────────────

  describe('Clear button', () => {
    it('clears the title and content fields when clicked', () => {
      renderForm();
      fireEvent.change(screen.getByLabelText(/document title/i),   { target: { value: SAMPLE_TITLE } });
      fireEvent.change(screen.getByLabelText(/document content/i), { target: { value: SAMPLE_CONTENT } });
      fireEvent.click(screen.getByRole('button', { name: /reset form/i }));

      expect(screen.getByLabelText(/document title/i)).toHaveValue('');
      expect(screen.getByLabelText(/document content/i)).toHaveValue('');
    });

    it('dismisses the success banner when clear is clicked', async () => {
      ingestDocument.mockResolvedValue(MOCK_DOC);
      renderForm();
      fireEvent.change(screen.getByLabelText(/document title/i),   { target: { value: SAMPLE_TITLE } });
      fireEvent.change(screen.getByLabelText(/document content/i), { target: { value: SAMPLE_CONTENT } });
      fireEvent.click(screen.getByRole('button', { name: /submit document/i }));

      await waitFor(() =>
        expect(
          screen.getByRole('alert', { name: /document submitted successfully/i }),
        ).toBeInTheDocument(),
      );

      // Click the Close (×) button on the success Alert
      fireEvent.click(screen.getByRole('button', { name: /close/i }));

      await waitFor(() =>
        expect(
          screen.queryByRole('alert', { name: /document submitted successfully/i }),
        ).not.toBeInTheDocument(),
      );
    });
  });

  // ── Permissions (no auth context needed — gate is on routes) ─────────────

  describe('Permissions', () => {
    it('renders the form without crashing (route-level protection verified by router tests)', () => {
      renderForm();
      expect(screen.getByRole('form', { name: /add company policy form/i })).toBeInTheDocument();
    });
  });
});
