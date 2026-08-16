/**
 * @fileoverview Tests for TaskPriorityChip — including new URGENT variant.
 *
 * Phase 6C: URGENT uses filled variant; CRITICAL kept as backward-compat alias.
 */

import React from 'react';
import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import TaskPriorityChip from '@/components/tasks/TaskPriorityChip';

const theme = createTheme();

function Wrap({ children }) {
  return <ThemeProvider theme={theme}>{children}</ThemeProvider>;
}

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

  it('renders URGENT priority with label "Urgent"', () => {
    render(<Wrap><TaskPriorityChip priority="URGENT" /></Wrap>);
    expect(screen.getByText('Urgent')).toBeInTheDocument();
  });

  it('renders CRITICAL as backward-compat alias', () => {
    render(<Wrap><TaskPriorityChip priority="CRITICAL" /></Wrap>);
    expect(screen.getByText('Critical')).toBeInTheDocument();
  });

  it('renders an unknown priority using the raw value', () => {
    render(<Wrap><TaskPriorityChip priority="BLOCKER" /></Wrap>);
    expect(screen.getByText('BLOCKER')).toBeInTheDocument();
  });

  it('renders with size="medium" without crashing', () => {
    render(<Wrap><TaskPriorityChip priority="URGENT" size="medium" /></Wrap>);
    expect(screen.getByText('Urgent')).toBeInTheDocument();
  });
});
