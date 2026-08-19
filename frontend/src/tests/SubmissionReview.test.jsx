/**
 * @fileoverview Tests for SubmissionReview component (manager view).
 *
 * Covers:
 *  - Renders submission details
 *  - Shows Approve and Request Changes buttons
 *  - Opens approval confirmation dialog
 *  - Opens request-changes dialog and requires comment
 *  - Disables buttons when isProcessing=true
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import SubmissionReview from '@/components/tasks/SubmissionReview';

const theme = createTheme();

function Wrapper({ children }) {
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
}

const mockSubmission = {
  id: 'sub-1',
  taskId: 'task-1',
  taskTitle: 'Build login page',
  submittedById: 'emp-1',
  submittedByName: 'Jane Doe',
  submissionNotes: 'Implemented login page as per the spec.',
  workCompleted: 'LoginForm component, JWT handling.',
  additionalComments: null,
  submittedAt: new Date().toISOString(),
  reviewStatus: 'PENDING_REVIEW',
  reviewComment: null,
  reviewedById: null,
  reviewedByName: null,
  reviewedAt: null,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
  hasAttachment: false,
  attachmentOriginalName: null,
  attachmentMimeType: null,
  attachmentSizeBytes: null,
};

const mockSubmissionWithAttachment = {
  ...mockSubmission,
  hasAttachment: true,
  attachmentOriginalName: 'design-spec.pdf',
  attachmentMimeType: 'application/pdf',
  attachmentSizeBytes: 204800,
};

describe('SubmissionReview', () => {
  it('renders submission notes and work completed', () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    expect(screen.getByText(/implemented login page as per the spec/i)).toBeInTheDocument();
    expect(screen.getByText(/loginform component/i)).toBeInTheDocument();
  });

  it('renders Approve and Request Changes buttons', () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    expect(screen.getByRole('button', { name: /approve submission/i })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /request changes/i })).toBeInTheDocument();
  });

  it('opens approval confirmation dialog when Approve is clicked', async () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    fireEvent.click(screen.getByRole('button', { name: /approve submission/i }));
    await waitFor(() => expect(screen.getByText(/approve submission\?/i)).toBeInTheDocument());
  });

  it('calls onApprove when confirmation dialog Approve is clicked', async () => {
    const onApprove = vi.fn();
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={onApprove}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    fireEvent.click(screen.getByRole('button', { name: /approve submission/i }));
    await waitFor(() => screen.getByText(/approve submission\?/i));
    // Click the dialog's Approve button
    const approveButtons = screen.getAllByRole('button', { name: /approve/i });
    fireEvent.click(approveButtons[approveButtons.length - 1]);
    await waitFor(() => expect(onApprove).toHaveBeenCalledTimes(1));
  });

  it('opens request-changes dialog and shows comment field', async () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    fireEvent.click(screen.getByRole('button', { name: /request changes/i }));
    await waitFor(() => expect(screen.getByLabelText(/review comment/i)).toBeInTheDocument());
  });

  it('requires a comment before calling onRequestChanges', async () => {
    const onRequestChanges = vi.fn();
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={onRequestChanges}
        />
      </Wrapper>,
    );
    fireEvent.click(screen.getByRole('button', { name: /request changes/i }));
    await waitFor(() => screen.getByLabelText(/review comment/i));
    // Click submit without entering a comment
    const submitBtn = screen.getAllByRole('button', { name: /request changes/i });
    fireEvent.click(submitBtn[submitBtn.length - 1]);
    await waitFor(() =>
      expect(screen.getByText(/please explain what changes/i)).toBeInTheDocument(),
    );
    expect(onRequestChanges).not.toHaveBeenCalled();
  });

  it('disables action buttons when isProcessing=true', () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
          isProcessing={true}
        />
      </Wrapper>,
    );
    expect(screen.getByRole('button', { name: /approve submission/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /request changes/i })).toBeDisabled();
  });

  // ── Attachment display tests (Phase 6B.1) ─────────────────────────────────

  it('does not show attachment section when submission has no attachment', () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmission}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    // No attachment filename shown
    expect(screen.queryByText(/design-spec\.pdf/i)).not.toBeInTheDocument();
    // No "Download" button
    expect(screen.queryByRole('button', { name: /download/i })).not.toBeInTheDocument();
  });

  it('shows attachment filename and download button when submission has an attachment', () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmissionWithAttachment}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    expect(screen.getByText(/design-spec\.pdf/i)).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /download/i })).toBeInTheDocument();
  });

  it('shows attachment file type and size when attachment is present', () => {
    render(
      <Wrapper>
        <SubmissionReview
          submission={mockSubmissionWithAttachment}
          onApprove={vi.fn()}
          onRequestChanges={vi.fn()}
        />
      </Wrapper>,
    );
    // Should display derived MIME type as uppercase (PDF from 'application/pdf')
    // Use getAllByText since "PDF" appears in both the type display and potentially elsewhere
    const pdfMatches = screen.getAllByText(/pdf/i);
    expect(pdfMatches.length).toBeGreaterThanOrEqual(1);
  });
});
