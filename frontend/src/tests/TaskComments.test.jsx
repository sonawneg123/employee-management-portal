/**
 * @fileoverview Tests for TaskComments component.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

vi.mock('@/hooks/useTaskHooks', () => ({
  useTaskComments: vi.fn(),
  useCreateTaskComment: vi.fn(),
}));

import { useTaskComments, useCreateTaskComment } from '@/hooks/useTaskHooks';
import TaskComments from '@/components/tasks/TaskComments';

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}

const mockMutateAsync = vi.fn();

// jsdom does not implement scrollIntoView — mock it globally
beforeEach(() => {
  window.HTMLElement.prototype.scrollIntoView = vi.fn();
});

function setupMocks({ comments = [], isLoading = false, isError = false } = {}) {
  useTaskComments.mockReturnValue({ data: comments, isLoading, isError });
  useCreateTaskComment.mockReturnValue({ mutateAsync: mockMutateAsync, isPending: false });
}

describe('TaskComments', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockMutateAsync.mockResolvedValue({});
  });

  it('shows loading spinner', () => {
    setupMocks({ isLoading: true });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('shows error message when fetch fails', () => {
    setupMocks({ isError: true });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    expect(screen.getByText(/failed to load comments/i)).toBeInTheDocument();
  });

  it('shows "No comments yet" when there are no comments', () => {
    setupMocks({ comments: [] });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    expect(screen.getByText(/no comments yet/i)).toBeInTheDocument();
  });

  it('renders existing comments', () => {
    setupMocks({
      comments: [
        {
          id: 'c1',
          authorName: 'Jane Manager',
          authorRole: 'ROLE_MANAGER',
          content: 'Please review the requirements.',
          edited: false,
          createdAt: '2025-08-01T10:00:00',
        },
      ],
    });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    expect(screen.getByText('Jane Manager')).toBeInTheDocument();
    expect(screen.getByText('Please review the requirements.')).toBeInTheDocument();
  });

  it('shows "edited" badge when comment.edited is true', () => {
    setupMocks({
      comments: [
        {
          id: 'c1',
          authorName: 'Alice',
          content: 'Updated comment',
          edited: true,
          createdAt: '2025-08-01T10:00:00',
        },
      ],
    });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    expect(screen.getByText(/\(edited\)/i)).toBeInTheDocument();
  });

  it('calls createTaskComment mutation when Post is clicked with content', async () => {
    setupMocks({ comments: [] });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    const textarea = screen.getByPlaceholderText(/write a comment/i);
    fireEvent.change(textarea, { target: { value: 'Hello world' } });
    fireEvent.click(screen.getByRole('button', { name: /post/i }));
    await waitFor(() =>
      expect(mockMutateAsync).toHaveBeenCalledWith({ taskId: 'task-1', content: 'Hello world' }),
    );
  });

  it('Post button is disabled when content is empty', () => {
    setupMocks({ comments: [] });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    expect(screen.getByRole('button', { name: /post/i })).toBeDisabled();
  });

  it('shows comment count chip', () => {
    setupMocks({
      comments: [{ id: 'c1', authorName: 'Bob', content: 'Hi', createdAt: '2025-01-01T00:00:00' }],
    });
    render(
      <Wrapper>
        <TaskComments taskId="task-1" />
      </Wrapper>,
    );
    // chip label = '1'
    expect(screen.getByText('1')).toBeInTheDocument();
  });
});
