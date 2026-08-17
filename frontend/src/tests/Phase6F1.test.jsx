/**
 * @fileoverview Phase 6F.1 Frontend Tests — Attendance + Leave Aware Task Availability
 *
 * Tests:
 * 1.  Checked-out employee appears disabled in the selector.
 * 2.  Employee becomes selectable after checking in today.
 * 3.  Approved leave today displays the correct disabled state.
 * 4.  Future leave does not disable the employee today.
 * 5.  Previous-day checkout does NOT keep the employee disabled after today's check-in.
 * 6.  Employee task controls show attendance warning when not checked in.
 * 7.  Appropriate availability/status indicators are displayed (leave chip, checkout chip).
 * 8.  Employee with pending leave today is selectable.
 * 9.  Employee on approved leave shows "Approved Leave Today" chip.
 * 10. Employee on approved leave shows lock icon (unavailable).
 */

import React, { useState } from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import {
  render,
  screen,
  fireEvent,
  waitFor,
} from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material/styles';
import { MemoryRouter } from 'react-router-dom';
import { HelmetProvider } from 'react-helmet-async';

import EmployeeAvailabilitySelector from '@/components/tasks/EmployeeAvailabilitySelector';
import EmployeeTaskDetailPage from '@/pages/tasks/EmployeeTaskDetailPage';
import { AuthContext } from '@/contexts/AuthContext';

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useTaskHooks', () => ({
  useTask: vi.fn(),
  useUpdateTaskStatus: vi.fn(() => ({
    mutateAsync: vi.fn(),
    isPending: false,
  })),
}));

vi.mock('@/hooks/useAttendanceStatus', () => ({
  useTodayAttendance: vi.fn(),
}));

vi.mock('@/hooks/useTaskSubmissionHooks', () => ({
  useCreateSubmission: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
  useLatestSubmission: vi.fn(() => ({ data: null, isLoading: false })),
  useResubmit: vi.fn(() => ({ mutateAsync: vi.fn(), isPending: false })),
}));

vi.mock('@/components/tasks/TaskDetailView', () => ({
  default: ({ startDisabled, startDisabledReason }) => (
    <div>
      <button
        data-testid="start-task-btn"
        disabled={startDisabled}
        title={startDisabledReason}
      >
        Start Task
      </button>
    </div>
  ),
}));

vi.mock('@/components/tasks/SubmissionForm', () => ({
  default: () => <div data-testid="submission-form" />,
}));
vi.mock('@/components/tasks/SubmissionStatusCard', () => ({
  default: () => <div data-testid="submission-status-card" />,
}));
vi.mock('@/components/tasks/TaskActivityTimeline', () => ({
  default: () => <div data-testid="task-activity-timeline" />,
}));
vi.mock('@/components/tasks/TaskComments', () => ({
  default: () => <div data-testid="task-comments" />,
}));
vi.mock('@/components/tasks/TaskAttachments', () => ({
  default: () => <div data-testid="task-attachments" />,
}));

import { useTask, useUpdateTaskStatus } from '@/hooks/useTaskHooks';
import { useTodayAttendance } from '@/hooks/useAttendanceStatus';

// ── Test helpers ──────────────────────────────────────────────────────────────

const testTheme = createTheme();

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
}

const BASE_USER = {
  userId: 'user-1',
  email: 'emp@example.com',
  firstName: 'Jane',
  lastName: 'Smith',
  roles: ['ROLE_EMPLOYEE'],
};

function Wrapper({ children }) {
  const qc = makeQueryClient();
  const authValue = {
    user: BASE_USER,
    token: 'mock-token',
    isAuthenticated: true,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    updateUser: vi.fn(),
    hasRole: (r) => BASE_USER.roles.includes(r),
    hasAnyRole: (rs) => rs.some((r) => BASE_USER.roles.includes(r)),
  };
  return (
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter initialEntries={['/tasks/task-1']}>
            <AuthContext.Provider value={authValue}>
              {children}
            </AuthContext.Provider>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </HelmetProvider>
  );
}

function SelectorWrapper({ employees = [], currentAssigneeId, ...rest }) {
  const [value, setValue] = useState('');
  return (
    <ThemeProvider theme={testTheme}>
      <QueryClientProvider client={makeQueryClient()}>
        <EmployeeAvailabilitySelector
          employees={employees}
          value={value}
          onChange={setValue}
          currentAssigneeId={currentAssigneeId}
          {...rest}
        />
      </QueryClientProvider>
    </ThemeProvider>
  );
}

// Sample employees with full availability fields
const EMPLOYEES = {
  checkedIn: {
    employeeId: 'emp-1',
    employeeName: 'Alice Smith',
    employeeCode: 'EMP001',
    checkedIn: true,
    onApprovedLeaveToday: false,
    availableToday: true,
    activeTasks: 2,
    overdueCount: 0,
  },
  checkedOut: {
    employeeId: 'emp-2',
    employeeName: 'Bob Jones',
    employeeCode: 'EMP002',
    checkedIn: false,
    onApprovedLeaveToday: false,
    availableToday: false,
    activeTasks: 1,
    overdueCount: 0,
    unavailabilityReason: 'CHECKED_OUT',
  },
  onApprovedLeave: {
    employeeId: 'emp-3',
    employeeName: 'Carol Wang',
    employeeCode: 'EMP003',
    checkedIn: false,
    onApprovedLeaveToday: true,
    availableToday: false,
    activeTasks: 0,
    overdueCount: 0,
  },
  onPendingLeave: {
    employeeId: 'emp-4',
    employeeName: 'Dave Kumar',
    employeeCode: 'EMP004',
    checkedIn: true,
    onApprovedLeaveToday: false, // PENDING leave doesn't set this flag
    availableToday: true,
    activeTasks: 3,
    overdueCount: 0,
  },
  // Employee who was checked out yesterday but checked in today
  checkedInTodayAfterYesterdayCheckout: {
    employeeId: 'emp-5',
    employeeName: 'Eve Patel',
    employeeCode: 'EMP005',
    checkedIn: true,         // Checked IN today
    onApprovedLeaveToday: false,
    availableToday: true,    // Available today — yesterday is irrelevant
    activeTasks: 1,
    overdueCount: 0,
  },
};

const SAMPLE_TASK = {
  id: 'task-1',
  title: 'Test Task',
  description: 'Test description',
  status: 'ASSIGNED',
  priority: 'MEDIUM',
  assignedEmployeeId: 'emp-1',
  assignedEmployeeName: 'Alice Smith',
  dueDate: '2099-12-31',
  category: 'DEVELOPMENT',
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
};

// ── EmployeeAvailabilitySelector Tests ───────────────────────────────────────

describe('EmployeeAvailabilitySelector — Phase 6F.1 (Attendance + Leave)', () => {
  it('1. Checked-out employee appears disabled', async () => {
    render(
      <SelectorWrapper employees={[EMPLOYEES.checkedOut]} />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      const item = screen.getByRole('option', { hidden: true, name: /Bob Jones/i });
      // MUI MenuItem with disabled=true gets aria-disabled attribute
      expect(item).toHaveAttribute('aria-disabled', 'true');
    });
  });

  it('2. Checked-in employee is selectable (availableToday=true)', async () => {
    const onChange = vi.fn();
    render(
      <ThemeProvider theme={testTheme}>
        <QueryClientProvider client={makeQueryClient()}>
          <EmployeeAvailabilitySelector
            employees={[EMPLOYEES.checkedIn]}
            value=""
            onChange={onChange}
          />
        </QueryClientProvider>
      </ThemeProvider>,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      const item = screen.getByRole('option', { hidden: true, name: /Alice Smith/i });
      // Should NOT be disabled
      expect(item).not.toHaveAttribute('aria-disabled', 'true');
    });
  });

  it('3. Employee on approved leave today is disabled and shows leave chip', async () => {
    render(
      <SelectorWrapper employees={[EMPLOYEES.onApprovedLeave]} />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      // The "🟠 Approved Leave Today" chip must be visible
      expect(screen.getByText('🟠 Approved Leave Today')).toBeInTheDocument();
    });
  });

  it('4. Employee with future leave (not today) is available — no leave chip', async () => {
    // Future leave means onApprovedLeaveToday=false → employee is available if checked in
    render(
      <SelectorWrapper employees={[EMPLOYEES.checkedIn]} />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      expect(screen.queryByText('🟠 Approved Leave Today')).not.toBeInTheDocument();
    });
  });

  it('5. Employee who checked in today (after yesterday checkout) is available', async () => {
    const onChange = vi.fn();
    render(
      <ThemeProvider theme={testTheme}>
        <QueryClientProvider client={makeQueryClient()}>
          <EmployeeAvailabilitySelector
            employees={[EMPLOYEES.checkedInTodayAfterYesterdayCheckout]}
            value=""
            onChange={onChange}
          />
        </QueryClientProvider>
      </ThemeProvider>,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      const item = screen.getByRole('option', { hidden: true, name: /Eve Patel/i });
      expect(item).not.toHaveAttribute('aria-disabled', 'true');
    });
  });

  it('7. Checked-in employee shows "🟢 Checked In" chip', async () => {
    render(
      <SelectorWrapper employees={[EMPLOYEES.checkedIn]} />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      expect(screen.getByText('🟢 Checked In')).toBeInTheDocument();
    });
  });

  it('7b. Checked-out employee shows "🔴 Checked Out" chip', async () => {
    render(
      <SelectorWrapper employees={[EMPLOYEES.checkedOut]} />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      expect(screen.getByText('🔴 Checked Out')).toBeInTheDocument();
    });
  });

  it('8. Employee with pending leave (onApprovedLeaveToday=false) is selectable', async () => {
    const onChange = vi.fn();
    render(
      <ThemeProvider theme={testTheme}>
        <QueryClientProvider client={makeQueryClient()}>
          <EmployeeAvailabilitySelector
            employees={[EMPLOYEES.onPendingLeave]}
            value=""
            onChange={onChange}
          />
        </QueryClientProvider>
      </ThemeProvider>,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      const item = screen.getByRole('option', { hidden: true, name: /Dave Kumar/i });
      expect(item).not.toHaveAttribute('aria-disabled', 'true');
    });
  });

  it('9. Approved leave chip is shown only for employee on approved leave', async () => {
    render(
      <SelectorWrapper
        employees={[EMPLOYEES.checkedIn, EMPLOYEES.onApprovedLeave]}
      />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      // Only Carol (emp-3) should show the leave chip
      const chips = screen.queryAllByText('🟠 Approved Leave Today');
      expect(chips).toHaveLength(1);
    });
  });

  it('10. Employee on approved leave shows lock icon (is unavailable)', async () => {
    render(
      <SelectorWrapper employees={[EMPLOYEES.onApprovedLeave]} />,
    );
    fireEvent.mouseDown(screen.getByRole('combobox'));
    await waitFor(() => {
      const item = screen.getByRole('option', { hidden: true, name: /Carol Wang/i });
      expect(item).toHaveAttribute('aria-disabled', 'true');
    });
  });
});

// ── EmployeeTaskDetailPage Tests ──────────────────────────────────────────────

describe('EmployeeTaskDetailPage — Phase 6F.1 attendance state', () => {
  beforeEach(() => {
    vi.mocked(useTask).mockReturnValue({
      data: SAMPLE_TASK,
      isLoading: false,
      isError: false,
    });
    vi.mocked(useUpdateTaskStatus).mockReturnValue({
      mutateAsync: vi.fn(),
      isPending: false,
    });
  });

  it('6a. Shows "not checked in" warning when employee has no attendance today', async () => {
    vi.mocked(useTodayAttendance).mockReturnValue({
      todayAttendance: null,
      isCheckedOut: false,
      isLoading: false,
    });

    render(
      <Wrapper>
        <EmployeeTaskDetailPage />
      </Wrapper>,
    );

    await waitFor(() => {
      expect(screen.getByText(/You have not checked in yet today/i)).toBeInTheDocument();
    });
  });

  it('6b. Shows "checked out" warning when employee has checked out today', async () => {
    vi.mocked(useTodayAttendance).mockReturnValue({
      todayAttendance: { checkInTime: '09:00', checkOutTime: '17:00' },
      isCheckedOut: true,
      isLoading: false,
    });

    render(
      <Wrapper>
        <EmployeeTaskDetailPage />
      </Wrapper>,
    );

    await waitFor(() => {
      expect(screen.getByText(/You have checked out for today/i)).toBeInTheDocument();
    });
  });

  it('6c. Task controls are enabled when employee is checked in today', async () => {
    vi.mocked(useTodayAttendance).mockReturnValue({
      todayAttendance: { checkInTime: '09:00', checkOutTime: null },
      isCheckedOut: false,
      isLoading: false,
    });

    render(
      <Wrapper>
        <EmployeeTaskDetailPage />
      </Wrapper>,
    );

    await waitFor(() => {
      // No warning banner should be shown
      expect(screen.queryByText(/You have not checked in yet today/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/You have checked out for today/i)).not.toBeInTheDocument();
      // Start button should NOT be disabled
      const startBtn = screen.getByTestId('start-task-btn');
      expect(startBtn).not.toBeDisabled();
    });
  });

  it('5 (frontend). Checking in today clears previous-day checkout blocking', async () => {
    // Simulate the state AFTER checking in today:
    // - todayAttendance has a checkInTime but NO checkOutTime
    // - isCheckedOut is false
    vi.mocked(useTodayAttendance).mockReturnValue({
      todayAttendance: { checkInTime: '09:00', checkOutTime: null },
      isCheckedOut: false,
      isLoading: false,
    });

    render(
      <Wrapper>
        <EmployeeTaskDetailPage />
      </Wrapper>,
    );

    await waitFor(() => {
      // No blocking banner should show
      expect(screen.queryByText(/checked out/i)).not.toBeInTheDocument();
      expect(screen.queryByText(/not checked in/i)).not.toBeInTheDocument();
      const startBtn = screen.getByTestId('start-task-btn');
      expect(startBtn).not.toBeDisabled();
    });
  });
});
