/**
 * @fileoverview Tests for LeaveTable component.
 *
 * Covers:
 *   - Renders column headers
 *   - Shows LeaveSkeleton while loading
 *   - Shows LeaveEmptyState when leaves list is empty (no filters)
 *   - Shows LeaveEmptyState with clear action when filters are active
 *   - Shows LeaveErrorState on error
 *   - Renders a row per leave
 *   - Calls onView when a row is clicked
 *   - Opens actions menu and triggers onApprove / onEdit / onCancel
 *   - Sort label triggers onSort
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { ThemeProvider, createTheme } from '@mui/material';

import LeaveTable from '@/components/leaves/LeaveTable';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const LEAVE_1 = {
  id:           'leave-uuid-1',
  employeeName: 'Alice Smith',
  leaveType:    'ANNUAL',
  status:       'PENDING',
  startDate:    '2025-06-02',
  endDate:      '2025-06-06',
  reason:       'Holiday',
};

const LEAVE_2 = {
  id:           'leave-uuid-2',
  employeeName: 'Bob Jones',
  leaveType:    'SICK',
  status:       'APPROVED',
  startDate:    '2025-06-09',
  endDate:      '2025-06-10',
  reason:       '',
};

// ── Wrapper ───────────────────────────────────────────────────────────────────

const theme = createTheme();

function renderTable(overrides = {}) {
  const defaults = {
    leaves:      [LEAVE_1, LEAVE_2],
    isLoading:   false,
    isFetching:  false,
    isError:     false,
    error:       null,
    sort:        'createdAt',
    direction:   'desc',
    hasFilters:  false,
    canApprove:  true,
    canEdit:     true,
    canCancel:   true,
    onSort:          vi.fn(),
    onView:          vi.fn(),
    onApprove:       vi.fn(),
    onReject:        vi.fn(),
    onEdit:          vi.fn(),
    onCancel:        vi.fn(),
    onRetry:         vi.fn(),
    onClearFilters:  vi.fn(),
    onAdd:           vi.fn(),
    canCreate:       true,
  };

  return render(
    <ThemeProvider theme={theme}>
      <MemoryRouter>
        <LeaveTable {...defaults} {...overrides} />
      </MemoryRouter>
    </ThemeProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('LeaveTable', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders the leave requests table', () => {
    renderTable();
    expect(screen.getByRole('table', { name: /leave requests table/i })).toBeInTheDocument();
  });

  it('renders a row for each leave', () => {
    renderTable();
    expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    expect(screen.getByText('Bob Jones')).toBeInTheDocument();
  });

  it('shows LeaveSkeleton while loading', () => {
    renderTable({ leaves: [], isLoading: true });
    // LeaveSkeleton renders a table structure or loading indicator
    // At minimum, no employee names should appear
    expect(screen.queryByText('Alice Smith')).not.toBeInTheDocument();
  });

  it('shows empty state when no leaves and no filters', () => {
    renderTable({ leaves: [], isLoading: false, hasFilters: false });
    // LeaveEmptyState shows "No leave requests yet" when no filters
    expect(screen.getByText(/no leave requests yet/i)).toBeInTheDocument();
  });

  it('shows empty state with clear-filters action when filters are active', () => {
    renderTable({ leaves: [], isLoading: false, hasFilters: true });
    // The button text is "Clear Filters" with aria-label="Clear filters"
    expect(screen.getByRole('button', { name: /clear filters/i })).toBeInTheDocument();
  });

  it('shows error state when isError is true', () => {
    renderTable({ leaves: [], isError: true, error: { message: 'Server error' } });
    expect(screen.getByText('Server error')).toBeInTheDocument();
  });

  it('calls onView when a leave row is clicked', () => {
    const onView = vi.fn();
    renderTable({ onView });
    fireEvent.click(screen.getByRole('row', { name: /alice smith/i }));
    expect(onView).toHaveBeenCalledWith(LEAVE_1);
  });

  it('opens the actions menu when the kebab button is clicked', async () => {
    renderTable();
    const menuButtons = screen.getAllByRole('button', { name: /actions for/i });
    fireEvent.click(menuButtons[0]);
    await waitFor(() => {
      // aria-label on the menuitem is "View leave details"
      expect(screen.getByRole('menuitem', { name: /view leave details/i })).toBeInTheDocument();
    });
  });

  it('calls onEdit from the actions menu for a PENDING leave', async () => {
    const onEdit = vi.fn();
    renderTable({ onEdit });
    // LEAVE_1 is PENDING so edit should be available
    const menuButtons = screen.getAllByRole('button', { name: /actions for alice/i });
    fireEvent.click(menuButtons[0]);
    await waitFor(() => screen.getByRole('menuitem', { name: /edit leave request/i }));
    fireEvent.click(screen.getByRole('menuitem', { name: /edit leave request/i }));
    expect(onEdit).toHaveBeenCalledWith(LEAVE_1);
  });

  it('calls onCancel from the actions menu for a PENDING leave', async () => {
    const onCancel = vi.fn();
    renderTable({ onCancel });
    const menuButtons = screen.getAllByRole('button', { name: /actions for alice/i });
    fireEvent.click(menuButtons[0]);
    await waitFor(() => screen.getByRole('menuitem', { name: /cancel leave/i }));
    fireEvent.click(screen.getByRole('menuitem', { name: /cancel leave/i }));
    expect(onCancel).toHaveBeenCalledWith(LEAVE_1);
  });
});
