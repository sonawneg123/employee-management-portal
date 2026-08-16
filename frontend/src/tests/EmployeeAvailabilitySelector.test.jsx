/**
 * @fileoverview Tests for EmployeeAvailabilitySelector component.
 */

import React, { useState } from 'react';
import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { ThemeProvider, createTheme } from '@mui/material';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import EmployeeAvailabilitySelector from '@/components/tasks/EmployeeAvailabilitySelector';

const theme = createTheme();

function Wrapper({ children }) {
  const qc = new QueryClient({ defaultOptions: { queries: { retry: false } } });
  return (
    <ThemeProvider theme={theme}>
      <QueryClientProvider client={qc}>{children}</QueryClientProvider>
    </ThemeProvider>
  );
}

const SAMPLE_EMPLOYEES = [
  {
    employeeId: 'emp-1',
    employeeName: 'Alice Smith',
    employeeCode: 'EMP001',
    checkedIn: true,
    activeTasks: 2,
    overdueCount: 0,
    workloadLevel: 'LOW',
  },
  {
    employeeId: 'emp-2',
    employeeName: 'Bob Jones',
    employeeCode: 'EMP002',
    checkedIn: false,
    activeTasks: 4,
    overdueCount: 1,
    workloadLevel: 'MEDIUM',
  },
  {
    employeeId: 'emp-3',
    employeeName: 'Carol Wang',
    employeeCode: 'EMP003',
    checkedIn: true,
    activeTasks: 7,
    overdueCount: 0,
    workloadLevel: 'HIGH',
  },
];

function ControlledSelector({ employees = SAMPLE_EMPLOYEES, error }) {
  const [value, setValue] = useState('');
  return (
    <Wrapper>
      <EmployeeAvailabilitySelector
        employees={employees}
        value={value}
        onChange={setValue}
        error={error}
      />
    </Wrapper>
  );
}

describe('EmployeeAvailabilitySelector', () => {
  it('renders the label "Assign To" by default', () => {
    render(<ControlledSelector />);
    expect(screen.getByLabelText(/assign to/i)).toBeInTheDocument();
  });

  it('disables the select when loading=true', () => {
    render(
      <Wrapper>
        <EmployeeAvailabilitySelector employees={[]} value="" onChange={() => {}} loading />
      </Wrapper>
    );
    // When loading, the Select is disabled (aria-disabled)
    const combo = screen.getByRole('combobox');
    expect(combo).toHaveAttribute('aria-disabled', 'true');
  });

  it('renders all employee options when opened', async () => {
    render(<ControlledSelector />);
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      expect(screen.getByText(/alice smith/i)).toBeInTheDocument();
      expect(screen.getByText(/bob jones/i)).toBeInTheDocument();
      expect(screen.getByText(/carol wang/i)).toBeInTheDocument();
    });
  });

  it('shows checked-in "In" badge for checked-in employees', async () => {
    render(<ControlledSelector />);
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      // Alice is checked in → should show "In" chip
      const inChips = screen.getAllByText('In');
      expect(inChips.length).toBeGreaterThan(0);
    });
  });

  it('shows "Out" badge for employees not checked in', async () => {
    render(<ControlledSelector />);
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      expect(screen.getByText('Out')).toBeInTheDocument();
    });
  });

  it('shows high-workload warning for employees with 6+ active tasks', async () => {
    render(<ControlledSelector />);
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      // Carol has 7 active tasks — should show "7 active" chip (warning color)
      expect(screen.getByText('7 active')).toBeInTheDocument();
    });
  });

  it('shows overdue count chip when overdueCount > 0', async () => {
    render(<ControlledSelector />);
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      expect(screen.getByText('1 overdue')).toBeInTheDocument();
    });
  });

  it('shows error helper text when error prop is set', () => {
    render(<ControlledSelector error="This field is required" />);
    expect(screen.getByText('This field is required')).toBeInTheDocument();
  });

  it('calls onChange with the selected employee id', async () => {
    const onChange = vi.fn();
    render(
      <Wrapper>
        <EmployeeAvailabilitySelector
          employees={SAMPLE_EMPLOYEES}
          value=""
          onChange={onChange}
        />
      </Wrapper>
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => screen.getByText(/alice smith/i));
    fireEvent.click(screen.getByText(/alice smith/i).closest('li'));
    expect(onChange).toHaveBeenCalledWith('emp-1');
  });
});
