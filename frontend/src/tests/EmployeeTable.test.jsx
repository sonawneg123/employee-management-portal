/**
 * @fileoverview Tests for the EmployeeTable component.
 *
 * Covers:
 *   - Renders column headers
 *   - Renders employee rows from props
 *   - Shows skeleton when isLoading=true
 *   - Shows empty state when employees=[] and no filters
 *   - Shows "no results" empty state when employees=[] and hasFilters=true
 *   - Shows error state when isError=true
 *   - onView is called when a row is clicked
 *   - Sort label click calls onSort
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';

import EmployeeTable from '@/components/employees/EmployeeTable';

// ── Test helpers ──────────────────────────────────────────────────────────────

const theme = createTheme();

const defaultProps = {
  employees:     [],
  isLoading:     false,
  isFetching:    false,
  isError:       false,
  error:         null,
  sort:          'createdAt',
  direction:     'desc',
  hasFilters:    false,
  canEdit:       true,
  canDelete:     true,
  onSort:        vi.fn(),
  onView:        vi.fn(),
  onEdit:        vi.fn(),
  onDelete:      vi.fn(),
  onRetry:       vi.fn(),
  onClearFilters:vi.fn(),
  onAdd:         vi.fn(),
  canCreate:     true,
};

const EMPLOYEE = {
  id:             'emp-1',
  employeeCode:   'EMP001',
  firstName:      'Jane',
  lastName:       'Smith',
  email:          'jane@example.com',
  jobTitle:       'Engineer',
  departmentName: 'Engineering',
  status:         'ACTIVE',
  dateOfJoining:  '2022-01-15',
  salary:         85000,
};

function renderTable(overrides = {}) {
  return render(
    <ThemeProvider theme={theme}>
      <EmployeeTable {...defaultProps} {...overrides} />
    </ThemeProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('EmployeeTable', () => {
  it('renders column headers', () => {
    renderTable();
    expect(screen.getByText('Employee')).toBeInTheDocument();
    expect(screen.getByText('Job Title')).toBeInTheDocument();
    expect(screen.getByText('Department')).toBeInTheDocument();
    expect(screen.getByText('Status')).toBeInTheDocument();
  });

  it('renders employee row data', () => {
    renderTable({ employees: [EMPLOYEE] });
    expect(screen.getByText('Jane Smith')).toBeInTheDocument();
    expect(screen.getByText('Engineer')).toBeInTheDocument();
    expect(screen.getByText('EMP001')).toBeInTheDocument();
  });

  it('shows skeleton when isLoading=true', () => {
    renderTable({ isLoading: true });
    expect(screen.getByLabelText('Loading employees')).toBeInTheDocument();
  });

  it('shows empty state when no employees and no filters', () => {
    renderTable({ employees: [], hasFilters: false });
    expect(screen.getByText('No employees yet')).toBeInTheDocument();
  });

  it('shows no-results state when no employees but filters active', () => {
    renderTable({ employees: [], hasFilters: true });
    expect(screen.getByText('No employees found')).toBeInTheDocument();
  });

  it('shows error state when isError=true', () => {
    renderTable({ isError: true, error: { message: 'Server down' } });
    expect(screen.getByText('Server down')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('calls onView when a row is clicked', () => {
    const onView = vi.fn();
    renderTable({ employees: [EMPLOYEE], onView });
    fireEvent.click(screen.getByRole('row', { name: /row for jane smith/i }));
    expect(onView).toHaveBeenCalledWith(EMPLOYEE);
  });

  it('calls onSort when a sortable column header is clicked', () => {
    const onSort = vi.fn();
    renderTable({ employees: [EMPLOYEE], onSort });
    fireEvent.click(screen.getByRole('button', { name: /sort by employee/i }));
    expect(onSort).toHaveBeenCalled();
  });
});
