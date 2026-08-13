/**
 * @fileoverview Tests for DashboardPage.
 *
 * Verifies the three primary render states:
 *   1. Loading — DashboardSkeleton is shown.
 *   2. Error   — error alert + retry button are shown.
 *   3. Empty   — EmptyDashboard is shown when totalEmployees/Departments === 0.
 *   4. Live    — DashboardHeader + RoleDashboard are shown for populated data.
 *
 * All hooks are mocked so no real HTTP calls are made.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import DashboardPage from '@/pages/dashboard/DashboardPage';

// ── Mock hooks ────────────────────────────────────────────────────────────────

vi.mock('@/hooks/useDashboard', () => ({
  useDashboardSummary: vi.fn(),
  useDashboardActivity: vi.fn(),
  useDashboardCharts: vi.fn(),
  useRefreshAllDashboard: vi.fn(),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(() => ({
    user: {
      userId: 'user-1',
      email: 'admin@example.com',
      firstName: 'Admin',
      lastName: 'User',
      roles: ['ROLE_ADMIN'],
    },
    isAuthenticated: true,
  })),
  AuthContext: { Provider: ({ children }) => children },
}));

import {
  useDashboardSummary,
  useDashboardActivity,
  useDashboardCharts,
  useRefreshAllDashboard,
} from '@/hooks/useDashboard';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const MOCK_SUMMARY = {
  totalEmployees: 42,
  totalDepartments: 5,
  pendingLeaves: 3,
  presentToday: 38,
  activeEmployees: 40,
  onLeaveToday: 2,
  newThisMonth: 4,
  trendEmployees: 2,
  trendLeaves: -1,
  trendAttendance: 0.05,
  attendanceRate: 0.9,
};

// ── Test wrapper ──────────────────────────────────────────────────────────────

const theme = createTheme();

/**
 * @param {React.ReactNode} ui
 * @returns {ReturnType<typeof render>}
 */
function renderPage(ui) {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false } },
  });
  return render(
    <HelmetProvider>
      <ThemeProvider theme={theme}>
        <QueryClientProvider client={queryClient}>
          <MemoryRouter>{ui}</MemoryRouter>
        </QueryClientProvider>
      </ThemeProvider>
    </HelmetProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

beforeEach(() => {
  vi.clearAllMocks();
  // Default: all hooks return idle/empty
  useDashboardActivity.mockReturnValue({
    data: [],
    isLoading: false,
    isFetching: false,
    refresh: vi.fn(),
  });
  useDashboardCharts.mockReturnValue({
    data: null,
    isLoading: false,
    isFetching: false,
    refresh: vi.fn(),
  });
  useRefreshAllDashboard.mockReturnValue(vi.fn());
});

describe('DashboardPage — loading state', () => {
  it('renders DashboardSkeleton while data is loading', () => {
    useDashboardSummary.mockReturnValue({
      data: undefined,
      isLoading: true,
      isFetching: true,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });

    renderPage(<DashboardPage />);

    expect(screen.getByLabelText('Loading dashboard')).toBeInTheDocument();
  });
});

describe('DashboardPage — error state', () => {
  it('renders an error alert with a Retry button', () => {
    useDashboardSummary.mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetching: false,
      isError: true,
      error: { message: 'Service unavailable' },
      refresh: vi.fn(),
    });

    renderPage(<DashboardPage />);

    expect(screen.getByRole('alert')).toBeInTheDocument();
    expect(screen.getByText('Service unavailable')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: /retry/i })).toBeInTheDocument();
  });

  it('calls refresh when Retry is clicked', () => {
    const refresh = vi.fn();
    useDashboardSummary.mockReturnValue({
      data: undefined,
      isLoading: false,
      isFetching: false,
      isError: true,
      error: { message: 'Oops' },
      refresh,
    });

    renderPage(<DashboardPage />);
    fireEvent.click(screen.getByRole('button', { name: /retry/i }));

    expect(refresh).toHaveBeenCalledTimes(1);
  });
});

describe('DashboardPage — empty state', () => {
  it('renders EmptyDashboard when totalEmployees and totalDepartments are 0', () => {
    useDashboardSummary.mockReturnValue({
      data: { totalEmployees: 0, totalDepartments: 0 },
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });

    renderPage(<DashboardPage />);

    expect(screen.getByText('Your dashboard is empty')).toBeInTheDocument();
    expect(screen.getByRole('link', { name: /add first employee/i })).toBeInTheDocument();
  });

  it('renders EmptyDashboard when summary is null', () => {
    useDashboardSummary.mockReturnValue({
      data: null,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });

    renderPage(<DashboardPage />);

    expect(screen.getByText('Your dashboard is empty')).toBeInTheDocument();
  });
});

describe('DashboardPage — live data state', () => {
  beforeEach(() => {
    useDashboardSummary.mockReturnValue({
      data: MOCK_SUMMARY,
      isLoading: false,
      isFetching: false,
      isError: false,
      error: null,
      refresh: vi.fn(),
    });
  });

  it('renders the Dashboard page heading', () => {
    renderPage(<DashboardPage />);
    expect(screen.getByRole('heading', { name: /dashboard/i })).toBeInTheDocument();
  });

  it('renders the Live status chip', () => {
    renderPage(<DashboardPage />);
    expect(screen.getByText('Live')).toBeInTheDocument();
  });

  it('renders the global refresh button', () => {
    renderPage(<DashboardPage />);
    expect(screen.getByRole('button', { name: /refresh dashboard data/i })).toBeInTheDocument();
  });

  it('calls refreshAll when the global refresh button is clicked', () => {
    const refreshAll = vi.fn();
    useRefreshAllDashboard.mockReturnValue(refreshAll);

    renderPage(<DashboardPage />);
    fireEvent.click(screen.getByRole('button', { name: /refresh dashboard data/i }));

    expect(refreshAll).toHaveBeenCalledTimes(1);
  });

  it('does not render the loading skeleton', () => {
    renderPage(<DashboardPage />);
    expect(screen.queryByLabelText('Loading dashboard')).not.toBeInTheDocument();
  });

  it('does not render the empty state', () => {
    renderPage(<DashboardPage />);
    expect(screen.queryByText('Your dashboard is empty')).not.toBeInTheDocument();
  });
});
