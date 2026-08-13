/**
 * @fileoverview Tests for the DepartmentTable component.
 *
 * Covers:
 *   - Renders column headers
 *   - Renders department row data
 *   - Shows skeleton when isLoading=true
 *   - Shows empty state with no search
 *   - Shows no-results state with active search
 *   - Shows error state when isError=true
 *   - onView is called when a row is clicked
 *   - Sort column click calls onSort
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';

import DepartmentTable from '@/components/departments/DepartmentTable';

const theme = createTheme();

const defaultProps = {
  departments: [],
  isLoading: false,
  isFetching: false,
  isError: false,
  error: null,
  sort: 'name',
  direction: 'asc',
  hasSearch: false,
  canEdit: true,
  canDelete: true,
  onSort: vi.fn(),
  onView: vi.fn(),
  onEdit: vi.fn(),
  onDelete: vi.fn(),
  onRetry: vi.fn(),
  onClearSearch: vi.fn(),
  onAdd: vi.fn(),
  canCreate: true,
};

const DEPT = {
  id: 'dept-1',
  name: 'Engineering',
  code: 'ENG',
  description: 'Software team',
  headName: 'Alice',
  employeeCount: 20,
  createdAt: '2022-01-01T00:00:00Z',
  updatedAt: '2022-01-01T00:00:00Z',
};

function renderTable(overrides = {}) {
  return render(
    <ThemeProvider theme={theme}>
      <DepartmentTable {...defaultProps} {...overrides} />
    </ThemeProvider>,
  );
}

describe('DepartmentTable', () => {
  it('renders column headers', () => {
    renderTable();
    expect(screen.getByText('Department')).toBeInTheDocument();
    expect(screen.getByText('Code')).toBeInTheDocument();
    expect(screen.getByText('Employees')).toBeInTheDocument();
    expect(screen.getByText('Head')).toBeInTheDocument();
    expect(screen.getByText('Created')).toBeInTheDocument();
  });

  it('renders department row data', () => {
    renderTable({ departments: [DEPT] });
    expect(screen.getByText('Engineering')).toBeInTheDocument();
    expect(screen.getByText('ENG')).toBeInTheDocument();
    expect(screen.getByText('Alice')).toBeInTheDocument();
  });

  it('shows skeleton when isLoading=true', () => {
    renderTable({ isLoading: true });
    expect(screen.getByLabelText('Loading departments')).toBeInTheDocument();
  });

  it('shows empty state when no departments and no search', () => {
    renderTable({ departments: [], hasSearch: false });
    expect(screen.getByText('No departments yet')).toBeInTheDocument();
  });

  it('shows no-results state when search is active', () => {
    renderTable({ departments: [], hasSearch: true });
    expect(screen.getByText('No departments found')).toBeInTheDocument();
  });

  it('shows error state when isError=true', () => {
    renderTable({ isError: true, error: { message: 'Network error' } });
    expect(screen.getByText('Network error')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('calls onView when a row is clicked', () => {
    const onView = vi.fn();
    renderTable({ departments: [DEPT], onView });
    fireEvent.click(screen.getByRole('row', { name: /row for engineering/i }));
    expect(onView).toHaveBeenCalledWith(DEPT);
  });

  it('calls onSort when a sortable column header is clicked', () => {
    const onSort = vi.fn();
    renderTable({ departments: [DEPT], onSort });
    fireEvent.click(screen.getByRole('button', { name: /sort by department/i }));
    expect(onSort).toHaveBeenCalled();
  });
});
