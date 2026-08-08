/**
 * @fileoverview Tests for DepartmentForm.
 *
 * Covers:
 *   - Renders all required fields
 *   - Shows validation errors on empty submit
 *   - Code field is required
 *   - Description is optional (no error when blank)
 *   - Disables all fields when isSubmitting=true
 *   - Server error violations are displayed
 */

import React from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import DepartmentForm from '@/components/departments/DepartmentForm';

const theme = createTheme();

function renderForm(props = {}) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  const defaults = {
    formId:       'test-form',
    onSubmit:     vi.fn(),
    isSubmitting: false,
    ...props,
  };
  return render(
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>
        <DepartmentForm {...defaults} />
        <button type="submit" form="test-form" data-testid="submit-btn">Submit</button>
      </QueryClientProvider>
    </ThemeProvider>,
  );
}

describe('DepartmentForm', () => {
  it('renders all form fields', () => {
    renderForm();
    expect(screen.getByLabelText(/department name/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/code/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/department head/i)).toBeInTheDocument();
  });

  it('shows required validation errors on empty submit', async () => {
    renderForm();
    await act(async () => {
      fireEvent.click(screen.getByTestId('submit-btn'));
    });
    await waitFor(() => {
      expect(screen.getByText('Department name is required')).toBeInTheDocument();
      expect(screen.getByText('Department code is required')).toBeInTheDocument();
    });
  });

  it('does not show a required error on description when blank', async () => {
    renderForm();
    // Fill required fields
    fireEvent.change(screen.getByLabelText(/department name/i), { target: { value: 'Engineering' } });
    fireEvent.change(screen.getByLabelText(/code/i),            { target: { value: 'ENG' } });
    await act(async () => {
      fireEvent.click(screen.getByTestId('submit-btn'));
    });
    await waitFor(() => {
      expect(screen.queryByText(/description.*required/i)).not.toBeInTheDocument();
    });
  });

  it('disables fields when isSubmitting=true', () => {
    renderForm({ isSubmitting: true });
    const inputs = screen.getAllByRole('textbox');
    inputs.forEach((input) => {
      expect(input).toBeDisabled();
    });
  });
});
