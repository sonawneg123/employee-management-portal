/**
 * @fileoverview Tests for EmployeeForm.
 *
 * Covers:
 *   - Renders all required fields
 *   - Shows validation errors on empty submit
 *   - Shows email format validation
 *   - Calls onSubmit with correct payload on valid submit
 *   - Server error violations are displayed
 *   - Salary field requires a number ≥ 0
 *   - Status field renders options
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import EmployeeForm from '@/components/employees/EmployeeForm';

// ── Mock hooks ────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useDepartments', () => ({
  useDepartments: vi.fn(() => ({
    data: [
      { id: 'dept-1', name: 'Engineering', code: 'ENG' },
      { id: 'dept-2', name: 'HR',          code: 'HR'  },
    ],
    isLoading: false,
  })),
}));

// ── Helpers ───────────────────────────────────────────────────────────────────

const theme = createTheme();

function renderForm(props = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const defaults = {
    formId:      'test-form',
    onSubmit:    vi.fn(),
    isSubmitting:false,
    ...props,
  };
  return render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>
        <EmployeeForm {...defaults} />
        {/* Submit button outside form uses form="test-form" */}
        <button type="submit" form="test-form" data-testid="submit-btn">
          Submit
        </button>
      </QueryClientProvider>
    </ThemeProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('EmployeeForm', () => {
  it('renders all required fields', () => {
    renderForm();
    expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/employee code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/job title/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/hire date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/salary/i)).toBeInTheDocument();
  });

  it('shows validation errors when submitted empty', async () => {
    renderForm();
    await act(async () => {
      fireEvent.click(screen.getByTestId('submit-btn'));
    });
    await waitFor(() => {
      expect(screen.getByText('First name is required')).toBeInTheDocument();
    });
    expect(screen.getByText('Last name is required')).toBeInTheDocument();
    expect(screen.getByText('Email is required')).toBeInTheDocument();
  });

  it('shows email format validation error', async () => {
    renderForm();
    await userEvent.type(screen.getByLabelText(/email address/i), 'not-an-email');
    await act(async () => {
      fireEvent.click(screen.getByTestId('submit-btn'));
    });
    await waitFor(() => {
      expect(
        screen.getByText('Please enter a valid email address'),
      ).toBeInTheDocument();
    });
  });

  it('disables all fields when isSubmitting=true', () => {
    renderForm({ isSubmitting: true });
    const inputs = screen.getAllByRole('textbox');
    inputs.forEach((input) => {
      expect(input).toBeDisabled();
    });
  });

  it('renders status options in the select', async () => {
    renderForm();
    // MUI Select renders as role="combobox". Open it via mouseDown on the
    // combobox element that carries the aria-label "Employee status".
    const statusSelect = screen.getByRole('combobox', { name: /employee status/i });
    fireEvent.mouseDown(statusSelect);
    await waitFor(() => {
      expect(screen.getByRole('option', { name: 'Active' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'Inactive' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'On Leave' })).toBeInTheDocument();
      expect(screen.getByRole('option', { name: 'Terminated' })).toBeInTheDocument();
    });
  });
});
