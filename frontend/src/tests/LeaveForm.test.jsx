/**
 * @fileoverview Tests for LeaveForm component.
 *
 * Covers:
 *   - Renders all form fields (Leave Type combobox, Start Date, End Date, Reason, Emergency, Attachment URL)
 *   - Shows validation error when Start Date is missing on submit
 *   - Shows validation error when End Date is missing on submit
 *   - Shows working-days chip after valid future date range is entered
 *   - Disables all fields when isSubmitting is true
 *   - Applies server-side errors via serverErrors prop
 *   - Attachment URL field rejects invalid URL
 *   - Calls onSubmit with correct data when form is valid
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { ThemeProvider, createTheme } from '@mui/material';
import dayjs from 'dayjs';

import LeaveForm from '@/components/leaves/LeaveForm';

// ── Helpers ───────────────────────────────────────────────────────────────────

const theme = createTheme();

function renderForm(overrides = {}) {
  const defaults = {
    formId:        'test-leave-form',
    defaultValues:  {},
    onSubmit:       vi.fn(),
    isSubmitting:   false,
    serverErrors:   undefined,
  };
  return render(
    <ThemeProvider theme={theme}>
      <LeaveForm {...defaults} {...overrides} />
    </ThemeProvider>,
  );
}

/** Returns today + N days formatted as YYYY-MM-DD */
function futureDate(days = 1) {
  return dayjs().add(days, 'day').format('YYYY-MM-DD');
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('LeaveForm', () => {
  beforeEach(() => vi.clearAllMocks());

  it('renders the Leave Type combobox', () => {
    renderForm();
    // MUI Select renders as a combobox role
    expect(screen.getByRole('combobox')).toBeInTheDocument();
  });

  it('renders the Start Date field', () => {
    renderForm();
    expect(screen.getByLabelText(/start date/i)).toBeInTheDocument();
  });

  it('renders the End Date field', () => {
    renderForm();
    expect(screen.getByLabelText(/end date/i)).toBeInTheDocument();
  });

  it('renders the Reason textarea', () => {
    renderForm();
    expect(screen.getByLabelText(/reason/i)).toBeInTheDocument();
  });

  it('renders the Emergency leave checkbox', () => {
    renderForm();
    expect(screen.getByRole('checkbox', { name: /emergency leave/i })).toBeInTheDocument();
  });

  it('renders the Attachment URL field', () => {
    renderForm();
    expect(screen.getByLabelText(/attachment url/i)).toBeInTheDocument();
  });

  it('disables date and text fields when isSubmitting is true', () => {
    renderForm({ isSubmitting: true });
    expect(screen.getByLabelText(/start date/i)).toBeDisabled();
    expect(screen.getByLabelText(/end date/i)).toBeDisabled();
    expect(screen.getByLabelText(/reason/i)).toBeDisabled();
  });

  it('shows working-days chip after valid future dates are entered', async () => {
    renderForm();
    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: futureDate(1) } });
    fireEvent.change(screen.getByLabelText(/end date/i),   { target: { value: futureDate(5) } });
    await waitFor(() => {
      // The chip label contains "working day"
      expect(screen.getByLabelText(/working day/i)).toBeInTheDocument();
    });
  });

  it('shows required error for Start Date when form is submitted empty', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit });
    // Submit the form — startDate is empty by default
    const form = document.getElementById('test-leave-form');
    fireEvent.submit(form);
    await waitFor(() => {
      expect(screen.getByText(/start date is required/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('shows required error for End Date when form is submitted with only start date', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit });
    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: futureDate(1) } });
    const form = document.getElementById('test-leave-form');
    fireEvent.submit(form);
    await waitFor(() => {
      expect(screen.getByText(/end date is required/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });

  it('calls onSubmit with correct data when form is valid', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit });

    // LEAVE_FORM_DEFAULTS already sets leaveType=ANNUAL — no need to select it.
    // Just fill the required date fields.
    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: futureDate(2) } });
    fireEvent.change(screen.getByLabelText(/end date/i),   { target: { value: futureDate(5) } });

    const form = document.getElementById('test-leave-form');
    fireEvent.submit(form);

    await waitFor(() => expect(onSubmit).toHaveBeenCalledOnce());
    const submitted = onSubmit.mock.calls[0][0];
    expect(submitted.leaveType).toBe('ANNUAL');
    expect(submitted.startDate).toBe(futureDate(2));
    expect(submitted.endDate).toBe(futureDate(5));
  });

  it('applies server-side error to a field', async () => {
    renderForm({ serverErrors: { startDate: 'Start date conflicts with approved leave.' } });
    await waitFor(() => {
      expect(screen.getByText(/conflicts with approved leave/i)).toBeInTheDocument();
    });
  });

  it('shows URL validation error for an invalid attachment URL', async () => {
    const onSubmit = vi.fn();
    renderForm({ onSubmit });

    // Fill required date fields to pass those checks
    fireEvent.change(screen.getByLabelText(/start date/i), { target: { value: futureDate(2) } });
    fireEvent.change(screen.getByLabelText(/end date/i),   { target: { value: futureDate(5) } });

    // Enter invalid URL
    fireEvent.change(screen.getByLabelText(/attachment url/i), { target: { value: 'not-a-url' } });

    const form = document.getElementById('test-leave-form');
    fireEvent.submit(form);

    await waitFor(() => {
      expect(screen.getByText(/must be a valid url/i)).toBeInTheDocument();
    });
    expect(onSubmit).not.toHaveBeenCalled();
  });
});
