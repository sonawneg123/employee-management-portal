/**
 * @fileoverview Tests for TaskAttachments component.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/hooks/useTaskHooks', () => ({
  useTaskAttachments: vi.fn(),
  useUploadTaskAttachment: vi.fn(),
  useDeleteTaskAttachment: vi.fn(),
}));

vi.mock('@/services/taskApi', () => ({
  downloadTaskAttachment: vi.fn().mockResolvedValue(undefined),
}));

import { useTaskAttachments, useUploadTaskAttachment, useDeleteTaskAttachment } from '@/hooks/useTaskHooks';
import TaskAttachments from '@/components/tasks/TaskAttachments';

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}

const mockUpload = vi.fn().mockResolvedValue({});
const mockDelete = vi.fn().mockResolvedValue(undefined);

function setupMocks({ attachments = [], isLoading = false, isError = false } = {}) {
  useTaskAttachments.mockReturnValue({ data: attachments, isLoading, isError });
  useUploadTaskAttachment.mockReturnValue({ mutateAsync: mockUpload, isPending: false });
  useDeleteTaskAttachment.mockReturnValue({ mutateAsync: mockDelete, isPending: false });
}

describe('TaskAttachments', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows loading spinner while fetching', () => {
    setupMocks({ isLoading: true });
    render(<Wrapper><TaskAttachments taskId="task-1" isManager /></Wrapper>);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('shows error message when fetch fails', () => {
    setupMocks({ isError: true });
    render(<Wrapper><TaskAttachments taskId="task-1" /></Wrapper>);
    expect(screen.getByText(/failed to load attachments/i)).toBeInTheDocument();
  });

  it('shows "No attachments yet" when empty', () => {
    setupMocks({ attachments: [] });
    render(<Wrapper><TaskAttachments taskId="task-1" /></Wrapper>);
    expect(screen.getByText(/no attachments yet/i)).toBeInTheDocument();
  });

  it('renders attachment list with filename', () => {
    setupMocks({
      attachments: [
        { id: 'att-1', originalName: 'spec.pdf', sizeBytes: 12345, uploadedAt: '2025-08-01T10:00:00', uploadedByName: 'Manager' },
      ],
    });
    render(<Wrapper><TaskAttachments taskId="task-1" /></Wrapper>);
    expect(screen.getByText('spec.pdf')).toBeInTheDocument();
  });

  it('shows Upload button for managers', () => {
    setupMocks({ attachments: [] });
    render(<Wrapper><TaskAttachments taskId="task-1" isManager /></Wrapper>);
    expect(screen.getByRole('button', { name: /upload/i })).toBeInTheDocument();
  });

  it('does NOT show Upload button for non-managers', () => {
    setupMocks({ attachments: [] });
    render(<Wrapper><TaskAttachments taskId="task-1" isManager={false} /></Wrapper>);
    expect(screen.queryByRole('button', { name: /upload/i })).not.toBeInTheDocument();
  });

  it('shows delete button for managers', () => {
    setupMocks({
      attachments: [
        { id: 'att-1', originalName: 'report.docx', sizeBytes: 5000, uploadedAt: '2025-08-01T10:00:00' },
      ],
    });
    render(<Wrapper><TaskAttachments taskId="task-1" isManager /></Wrapper>);
    expect(screen.getByLabelText(/delete attachment/i)).toBeInTheDocument();
  });

  it('does NOT show delete button for employees', () => {
    setupMocks({
      attachments: [
        { id: 'att-1', originalName: 'report.docx', sizeBytes: 5000, uploadedAt: '2025-08-01T10:00:00' },
      ],
    });
    render(<Wrapper><TaskAttachments taskId="task-1" isManager={false} /></Wrapper>);
    expect(screen.queryByLabelText(/delete attachment/i)).not.toBeInTheDocument();
  });

  it('shows formatted file size', () => {
    setupMocks({
      attachments: [
        { id: 'att-1', originalName: 'data.csv', sizeBytes: 2048, uploadedAt: '2025-08-01T10:00:00' },
      ],
    });
    render(<Wrapper><TaskAttachments taskId="task-1" /></Wrapper>);
    expect(screen.getByText(/2\.0 KB/i)).toBeInTheDocument();
  });

  it('shows supported formats helper text for managers', () => {
    setupMocks({ attachments: [] });
    render(<Wrapper><TaskAttachments taskId="task-1" isManager /></Wrapper>);
    expect(screen.getByText(/PDF, CSV, DOCX, TXT/i)).toBeInTheDocument();
  });
});
