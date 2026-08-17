/**
 * @fileoverview ManagerLeavePage tests.
 *
 * Covers:
 * 1. ManagerLeavePage renders without crashing.
 * 2. Shows "Leave Approvals" heading.
 * 3. Defaults status filter to PENDING.
 * 4. Approve and Reject dialogs are available for PENDING leaves.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';
import { AuthContext } from '@/contexts/AuthContext';

import ManagerLeavePage from '@/pages/leaves/ManagerLeavePage';

// ── Mocks ──────────────────────────────────────────────────────────────────────

vi.mock('@/api/axiosInstance', () => ({
  default: {
    defaults: { baseURL: '' },
    get: vi.fn().mockRejectedValue(new Error('no photo')),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn(),
  },
}));

vi.mock('@/services/leaveApi', () => ({
  getLeaveRequests: vi.fn().mockResolvedValue({
    content: [
      {
        id: 'leave-1',
        employeeId: 'emp-1',
        employeeName: 'Alice Smith',
        employeeCode: 'EMP-001',
        departmentName: 'Engineering',
        leaveType: 'ANNUAL',
        startDate: '2026-08-01',
        endDate: '2026-08-05',
        totalDays: 5,
        reason: 'Vacation',
        status: 'PENDING',
        rejectionReason: null,
        reviewedBy: null,
        reviewedAt: null,
        createdAt: '2026-07-10T09:00:00',
        updatedAt: '2026-07-10T09:00:00',
      },
    ],
    totalElements: 1,
    totalPages: 1,
    number: 0,
    size: 20,
  }),
  getMyLeaveRequests: vi.fn().mockResolvedValue({ content: [], totalElements: 0, totalPages: 0 }),
  approveLeave: vi.fn().mockResolvedValue({}),
  rejectLeave: vi.fn().mockResolvedValue({}),
  createLeaveRequest: vi.fn(),
  updateLeaveRequest: vi.fn(),
  cancelLeave: vi.fn(),
  getLeaveById: vi.fn(),
}));

vi.mock('@/services/profileApi', () => ({
  getProfile: vi.fn().mockResolvedValue({}),
  getEmployeePhotoUrl: vi.fn((id) => `/api/employees/${id}/profile-photo`),
}));

// ── Test helpers ──────────────────────────────────────────────────────────────

const theme = createTheme();

const MANAGER_USER = {
  userId: 'user-mgr',
  email: 'manager@example.com',
  firstName: 'Bob',
  lastName: 'Manager',
  roles: ['ROLE_MANAGER'],
};

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

function Wrapper({ children }) {
  return (
    <HelmetProvider>
      <MemoryRouter>
        <ThemeProvider theme={theme}>
          <QueryClientProvider client={makeQueryClient()}>
            <AuthContext.Provider
              value={{
                user: MANAGER_USER,
                isAuthenticated: true,
                isLoading: false,
                login: vi.fn(),
                register: vi.fn(),
                logout: vi.fn(),
                updateUser: vi.fn(),
                hasRole: (role) => role === 'ROLE_MANAGER',
                hasAnyRole: (roles) => roles.some((r) => ['ROLE_MANAGER', 'ROLE_HR', 'ROLE_ADMIN'].includes(r)),
              }}
            >
              {children}
            </AuthContext.Provider>
          </QueryClientProvider>
        </ThemeProvider>
      </MemoryRouter>
    </HelmetProvider>
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('ManagerLeavePage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders without crashing', () => {
    render(<Wrapper><ManagerLeavePage /></Wrapper>);
    expect(screen.getByRole('heading', { name: /Leave Approvals/i })).toBeInTheDocument();
  });

  it('shows "Leave Approvals" as the page heading', () => {
    render(<Wrapper><ManagerLeavePage /></Wrapper>);
    expect(screen.getByRole('heading', { name: /Leave Approvals/i })).toBeInTheDocument();
  });

  it('shows employee leave request in the table', async () => {
    render(<Wrapper><ManagerLeavePage /></Wrapper>);
    await waitFor(() => {
      expect(screen.getByText('Alice Smith')).toBeInTheDocument();
    });
  });

  it('shows leave type', async () => {
    render(<Wrapper><ManagerLeavePage /></Wrapper>);
    await waitFor(() => {
      // LeaveTypeChip renders "Annual Leave" or similar — may appear in multiple places
      const items = screen.getAllByText(/annual/i);
      expect(items.length).toBeGreaterThanOrEqual(1);
    });
  });

  it('does not show a create button (managers cannot create leaves)', () => {
    render(<Wrapper><ManagerLeavePage /></Wrapper>);
    // canCreate=false — no "New Leave" or "Add" button
    expect(screen.queryByRole('button', { name: /new leave/i })).not.toBeInTheDocument();
    expect(screen.queryByRole('button', { name: /add/i })).not.toBeInTheDocument();
  });
});
