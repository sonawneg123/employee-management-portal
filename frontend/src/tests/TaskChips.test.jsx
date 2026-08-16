/**
 * @fileoverview Tests for TaskStatusChip and TaskPriorityChip components.
 */

import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import TaskStatusChip from '@/components/tasks/TaskStatusChip';
import TaskPriorityChip from '@/components/tasks/TaskPriorityChip';

const theme = createTheme();

function Wrap({ children }) {
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
}

// ── TaskStatusChip ────────────────────────────────────────────────────────────

describe('TaskStatusChip', () => {
  it('renders ASSIGNED status', () => {
    render(<Wrap><TaskStatusChip status="ASSIGNED" /></Wrap>);
    expect(screen.getByText('Assigned')).toBeInTheDocument();
  });

  it('renders IN_PROGRESS status', () => {
    render(<Wrap><TaskStatusChip status="IN_PROGRESS" /></Wrap>);
    expect(screen.getByText('In Progress')).toBeInTheDocument();
  });

  it('renders COMPLETED status', () => {
    render(<Wrap><TaskStatusChip status="COMPLETED" /></Wrap>);
    expect(screen.getByText('Completed')).toBeInTheDocument();
  });

  it('renders DRAFT status', () => {
    render(<Wrap><TaskStatusChip status="DRAFT" /></Wrap>);
    expect(screen.getByText('Draft')).toBeInTheDocument();
  });

  it('shows Overdue when overdue=true and status is not COMPLETED', () => {
    render(<Wrap><TaskStatusChip status="ASSIGNED" overdue={true} /></Wrap>);
    expect(screen.getByText('Overdue')).toBeInTheDocument();
  });

  it('does NOT show Overdue when status is COMPLETED even if overdue=true', () => {
    render(<Wrap><TaskStatusChip status="COMPLETED" overdue={true} /></Wrap>);
    expect(screen.getByText('Completed')).toBeInTheDocument();
    expect(screen.queryByText('Overdue')).not.toBeInTheDocument();
  });

  it('renders CHANGES_REQUESTED status', () => {
    render(<Wrap><TaskStatusChip status="CHANGES_REQUESTED" /></Wrap>);
    expect(screen.getByText('Changes Requested')).toBeInTheDocument();
  });
});

// ── TaskPriorityChip ──────────────────────────────────────────────────────────

describe('TaskPriorityChip', () => {
  it('renders LOW priority', () => {
    render(<Wrap><TaskPriorityChip priority="LOW" /></Wrap>);
    expect(screen.getByText('Low')).toBeInTheDocument();
  });

  it('renders MEDIUM priority', () => {
    render(<Wrap><TaskPriorityChip priority="MEDIUM" /></Wrap>);
    expect(screen.getByText('Medium')).toBeInTheDocument();
  });

  it('renders HIGH priority', () => {
    render(<Wrap><TaskPriorityChip priority="HIGH" /></Wrap>);
    expect(screen.getByText('High')).toBeInTheDocument();
  });

  it('renders CRITICAL priority', () => {
    render(<Wrap><TaskPriorityChip priority="CRITICAL" /></Wrap>);
    expect(screen.getByText('Critical')).toBeInTheDocument();
  });
});
