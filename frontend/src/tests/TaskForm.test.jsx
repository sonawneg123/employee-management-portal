/**
 * @fileoverview Tests for TaskForm component.
 *
 * Covers:
 *  - Renders all required fields
 *  - Shows validation error when title is missing (controlled via errors prop)
 *  - Shows validation error when dueDate is missing (controlled via errors prop)
 *  - Calls onSubmit when form is submitted
 *  - Calls onCancel when Cancel is clicked
 *  - Disables buttons when isSubmitting=true
 */

import React, { useState } from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import TaskForm from '@/components/tasks/TaskForm';

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}

function ControlledForm({ onSubmit = vi.fn(), onCancel = vi.fn(), errors = {}, isSubmitting = false }) {
  const [values, setValues] = useState({
    title: '',
    description: '',
    guidelines: '',
    acceptanceCriteria: '',
    assignedEmployeeId: '',
    priority: 'MEDIUM',
    dueDate: '',
    estimatedHours: '',
    category: '',
  });
  return (
    <Wrapper>
      <TaskForm
        values={values}
        errors={errors}
        onChange={(field, value) => setValues((prev) => ({ ...prev, [field]: value }))}
        onSubmit={onSubmit}
        onCancel={onCancel}
        employees={[{ id: 'emp-1', label: 'Jane Doe (EMP-001)' }]}
        isSubmitting={isSubmitting}
        submitLabel="Create Task"
      />
    </Wrapper>
  );
}

describe('TaskForm', () => {
  it('renders all main fields', () => {
    render(<ControlledForm />);
    expect(screen.getByLabelText(/title/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/description/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/guidelines/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/acceptance criteria/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/due date/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/estimated hours/i)).toBeInTheDocument();
    expect(screen.getByLabelText(/category/i)).toBeInTheDocument();
  });

  it('shows title validation error when errors.title is set', () => {
    render(<ControlledForm errors={{ title: 'Title is required' }} />);
    expect(screen.getByText('Title is required')).toBeInTheDocument();
  });

  it('shows dueDate validation error when errors.dueDate is set', () => {
    render(<ControlledForm errors={{ dueDate: 'Due date is required' }} />);
    expect(screen.getByText('Due date is required')).toBeInTheDocument();
  });

  it('calls onSubmit when form is submitted', async () => {
    const onSubmit = vi.fn();
    render(<ControlledForm onSubmit={onSubmit} />);
    fireEvent.click(screen.getByRole('button', { name: /create task/i }));
    await waitFor(() => expect(onSubmit).toHaveBeenCalledTimes(1));
  });

  it('calls onCancel when Cancel button is clicked', () => {
    const onCancel = vi.fn();
    render(<ControlledForm onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: /cancel/i }));
    expect(onCancel).toHaveBeenCalledTimes(1);
  });

  it('disables buttons when isSubmitting=true', () => {
    render(<ControlledForm isSubmitting={true} />);
    expect(screen.getByRole('button', { name: /create task/i })).toBeDisabled();
    expect(screen.getByRole('button', { name: /cancel/i })).toBeDisabled();
  });

  it('renders employee option in Assign To dropdown', async () => {
    render(<ControlledForm />);
    // Open dropdown
    fireEvent.mouseDown(screen.getByRole('combobox', { name: /assign to/i }));
    await waitFor(() =>
      expect(screen.getByText('Jane Doe (EMP-001)')).toBeInTheDocument()
    );
  });
});
