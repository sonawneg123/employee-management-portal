/**
 * @fileoverview Tests for TaskActivityTimeline component.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// Mock the hook
vi.mock('@/hooks/useTaskHooks', () => ({
  useTaskActivities: vi.fn(),
}));

import { useTaskActivities } from '@/hooks/useTaskHooks';
import TaskActivityTimeline from '@/components/tasks/TaskActivityTimeline';

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}

describe('TaskActivityTimeline', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('shows a loading spinner while fetching', () => {
    useTaskActivities.mockReturnValue({ data: undefined, isLoading: true, isError: false });
    render(<Wrapper><TaskActivityTimeline taskId="task-1" /></Wrapper>);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('shows an error message when the fetch fails', () => {
    useTaskActivities.mockReturnValue({ data: undefined, isLoading: false, isError: true });
    render(<Wrapper><TaskActivityTimeline taskId="task-1" /></Wrapper>);
    expect(screen.getByText(/failed to load activity timeline/i)).toBeInTheDocument();
  });

  it('shows "No activity recorded yet" when the list is empty', () => {
    useTaskActivities.mockReturnValue({ data: [], isLoading: false, isError: false });
    render(<Wrapper><TaskActivityTimeline taskId="task-1" /></Wrapper>);
    expect(screen.getByText(/no activity recorded yet/i)).toBeInTheDocument();
  });

  it('renders activity entries newest-first', () => {
    useTaskActivities.mockReturnValue({
      data: [
        { id: '1', actorName: 'Alice', description: 'created task', eventType: 'CREATED', createdAt: '2025-01-01T09:00:00' },
        { id: '2', actorName: 'Bob', description: 'started task', eventType: 'STATUS_CHANGED', createdAt: '2025-01-02T10:00:00' },
      ],
      isLoading: false,
      isError: false,
    });
    render(<Wrapper><TaskActivityTimeline taskId="task-1" /></Wrapper>);
    // Bob (newer) should appear before Alice (older) because list is reversed
    const items = screen.getAllByText(/alice|bob/i);
    expect(items[0].textContent).toMatch(/bob/i);
  });

  it('renders the activity section heading', () => {
    useTaskActivities.mockReturnValue({ data: [], isLoading: false, isError: false });
    render(<Wrapper><TaskActivityTimeline taskId="task-1" /></Wrapper>);
    expect(screen.getByText(/activity timeline/i)).toBeInTheDocument();
  });
});
