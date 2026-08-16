/**
 * @fileoverview Tests for SubmissionForm component (Phase 6B.1).
 *
 * Covers:
 *  - Renders submission notes field and submit button
 *  - Shows validation error when submissionNotes is empty
 *  - Calls onSubmit with correct payload (text-only)
 *  - Calls onSubmit with file when file is selected
 *  - Disables button when isSubmitting=true
 *  - In resubmit mode: shows manager feedback and "Resubmit" label
 *  - File input renders with correct accept attribute
 *  - Accepted file type validation works
 *  - Invalid file displays validation error
 *  - Selected filename is displayed
 *  - Remove/replace works
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import SubmissionForm from '@/components/tasks/SubmissionForm';

const theme = createTheme();

function Wrapper({ children }) {
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
}

describe('SubmissionForm', () => {
  it('renders submission notes field', () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    expect(screen.getByLabelText(/submission notes/i)).toBeInTheDocument();
  });

  it('shows "Submit for Review" button by default', () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    expect(screen.getByRole('button', { name: /submit for review/i })).toBeInTheDocument();
  });

  it('shows validation error when submissionNotes is empty', async () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    const form = document.querySelector('form');
    fireEvent.submit(form);
    await waitFor(() =>
      expect(screen.getByText(/submission notes are required/i)).toBeInTheDocument(),
    );
  });

  it('calls onSubmit with correct payload when notes are provided (text-only, no file)', async () => {
    const onSubmit = vi.fn();
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={onSubmit} />
      </Wrapper>,
    );
    fireEvent.change(screen.getByLabelText(/submission notes/i), {
      target: { value: 'Done the feature' },
    });
    const form = document.querySelector('form');
    fireEvent.submit(form);
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(
        {
          submissionNotes: 'Done the feature',
          workCompleted: null,
          additionalComments: null,
        },
        null, // no file
      ),
    );
  });

  it('disables submit button when isSubmitting=true', () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} isSubmitting={true} />
      </Wrapper>,
    );
    expect(screen.getByRole('button', { name: /submitting/i })).toBeDisabled();
  });

  it('shows resubmit label in resubmit mode', () => {
    const existingSubmission = {
      id: 'sub-1',
      submissionNotes: 'Previous notes',
      workCompleted: 'Previous work',
      additionalComments: null,
      reviewComment: 'Please add tests',
      reviewStatus: 'CHANGES_REQUESTED',
      hasAttachment: false,
    };
    render(
      <Wrapper>
        <SubmissionForm
          taskId="task-1"
          existingSubmission={existingSubmission}
          isResubmit={true}
          onSubmit={vi.fn()}
        />
      </Wrapper>,
    );
    expect(screen.getByRole('button', { name: /resubmit for review/i })).toBeInTheDocument();
  });

  it('shows manager feedback in resubmit mode', () => {
    const existingSubmission = {
      id: 'sub-1',
      submissionNotes: 'Previous notes',
      workCompleted: null,
      additionalComments: null,
      reviewComment: 'Please add unit tests',
      reviewStatus: 'CHANGES_REQUESTED',
      hasAttachment: false,
    };
    render(
      <Wrapper>
        <SubmissionForm
          taskId="task-1"
          existingSubmission={existingSubmission}
          isResubmit={true}
          onSubmit={vi.fn()}
        />
      </Wrapper>,
    );
    expect(screen.getByText(/please add unit tests/i)).toBeInTheDocument();
  });

  // ── File attachment tests ─────────────────────────────────────────────────

  it('renders file input with correct accept attribute', () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    const input = document.querySelector('input[type="file"]');
    expect(input).toBeInTheDocument();
    expect(input.getAttribute('accept')).toContain('.pdf');
    expect(input.getAttribute('accept')).toContain('.csv');
    expect(input.getAttribute('accept')).toContain('.docx');
    expect(input.getAttribute('accept')).toContain('.txt');
  });

  it('displays selected filename after valid file is chosen', async () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    const input = document.querySelector('input[type="file"]');
    const pdfFile = new File(['hello'], 'report.pdf', { type: 'application/pdf' });
    fireEvent.change(input, { target: { files: [pdfFile] } });
    await waitFor(() =>
      expect(screen.getByText('report.pdf')).toBeInTheDocument(),
    );
  });

  it('shows validation error for unsupported file type', async () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    const input = document.querySelector('input[type="file"]');
    const zipFile = new File(['data'], 'archive.zip', { type: 'application/zip' });
    fireEvent.change(input, { target: { files: [zipFile] } });
    await waitFor(() =>
      expect(screen.getByText(/unsupported file type/i)).toBeInTheDocument(),
    );
  });

  it('shows remove button after file selection and removes file on click', async () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    const input = document.querySelector('input[type="file"]');
    const pdfFile = new File(['hello'], 'report.pdf', { type: 'application/pdf' });
    fireEvent.change(input, { target: { files: [pdfFile] } });

    await waitFor(() => expect(screen.getByText('report.pdf')).toBeInTheDocument());

    // Multiple buttons with "remove file" text may exist — click the first one (icon button)
    const removeButtons = screen.getAllByRole('button', { name: /remove file/i });
    fireEvent.click(removeButtons[0]);

    await waitFor(() => expect(screen.queryByText('report.pdf')).not.toBeInTheDocument());
  });

  it('calls onSubmit with file when a valid file is selected', async () => {
    const onSubmit = vi.fn();
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={onSubmit} />
      </Wrapper>,
    );

    // Enter notes
    fireEvent.change(screen.getByLabelText(/submission notes/i), {
      target: { value: 'Work done' },
    });

    // Select a file
    const input = document.querySelector('input[type="file"]');
    const pdfFile = new File(['content'], 'report.pdf', { type: 'application/pdf' });
    fireEvent.change(input, { target: { files: [pdfFile] } });

    await waitFor(() => expect(screen.getByText('report.pdf')).toBeInTheDocument());

    const form = document.querySelector('form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ submissionNotes: 'Work done' }),
        pdfFile,
      );
    });
  });

  it('disables submit button when file has a validation error', async () => {
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={vi.fn()} />
      </Wrapper>,
    );
    const input = document.querySelector('input[type="file"]');
    const badFile = new File(['x'], 'bad.exe', { type: 'application/octet-stream' });
    // Force an unsupported extension
    Object.defineProperty(badFile, 'name', { value: 'bad.exe' });
    fireEvent.change(input, { target: { files: [badFile] } });
    await waitFor(() =>
      expect(screen.getByRole('button', { name: /submit for review/i })).toBeDisabled(),
    );
  });

  it('existing text-only submission still works without file', async () => {
    const onSubmit = vi.fn();
    render(
      <Wrapper>
        <SubmissionForm taskId="task-1" onSubmit={onSubmit} />
      </Wrapper>,
    );
    fireEvent.change(screen.getByLabelText(/submission notes/i), {
      target: { value: 'Just notes, no file' },
    });
    const form = document.querySelector('form');
    fireEvent.submit(form);
    await waitFor(() =>
      expect(onSubmit).toHaveBeenCalledWith(
        expect.objectContaining({ submissionNotes: 'Just notes, no file' }),
        null,
      ),
    );
  });

  it('shows current attachment info in resubmit mode when submission has attachment', () => {
    const existingSubmission = {
      id: 'sub-1',
      submissionNotes: 'Previous notes',
      workCompleted: null,
      additionalComments: null,
      reviewComment: 'Fix the code',
      reviewStatus: 'CHANGES_REQUESTED',
      hasAttachment: true,
      attachmentOriginalName: 'previous-report.pdf',
      attachmentSizeBytes: 2048,
    };
    render(
      <Wrapper>
        <SubmissionForm
          taskId="task-1"
          existingSubmission={existingSubmission}
          isResubmit={true}
          onSubmit={vi.fn()}
        />
      </Wrapper>,
    );
    expect(screen.getByText(/previous-report\.pdf/i)).toBeInTheDocument();
  });
});
