/**
 * @fileoverview Tests for the dashboard React Query hooks.
 *
 * Verifies that each hook:
 *   - Returns the expected data shape on success.
 *   - Surfaces isLoading / isFetching correctly.
 *   - Provides a working `refresh` callback that invalidates the cache.
 *   - useRefreshAllDashboard invalidates all dashboard queries.
 *
 * All HTTP calls are intercepted via vi.mock so no real network traffic occurs.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import {
  useDashboardSummary,
  useDashboardActivity,
  useDashboardCharts,
  useRefreshAllDashboard,
} from '@/hooks/useDashboard';

// ── Mock API services ─────────────────────────────────────────────────────────

vi.mock('@/services/dashboardApi', () => ({
  getDashboardSummary:  vi.fn(),
  getDashboardActivity: vi.fn(),
  getDashboardCharts:   vi.fn(),
}));

import {
  getDashboardSummary,
  getDashboardActivity,
  getDashboardCharts,
} from '@/services/dashboardApi';

// ── Fixtures ──────────────────────────────────────────────────────────────────

/** @type {import('@/services/dashboardApi').DashboardSummary} */
const MOCK_SUMMARY = {
  totalEmployees:   42,
  totalDepartments: 5,
  pendingLeaves:    3,
  presentToday:     38,
  activeEmployees:  40,
  onLeaveToday:     2,
  newThisMonth:     4,
  trendEmployees:   2,
  trendLeaves:      -1,
  trendAttendance:  0.05,
  attendanceRate:   0.9,
};

/** @type {import('@/services/dashboardApi').ActivityItem[]} */
const MOCK_ACTIVITY = [
  {
    id:          'act-1',
    type:        'EMPLOYEE_JOINED',
    description: 'Alice joined the team',
    timestamp:   '2024-01-15T10:00:00Z',
    actorName:   'Admin',
  },
];

/** @type {import('@/services/dashboardApi').DashboardCharts} */
const MOCK_CHARTS = {
  departmentDistribution: [
    { name: 'Engineering', count: 20, code: 'ENG' },
    { name: 'HR',          count: 5,  code: 'HR'  },
  ],
  attendanceTrend: [
    { date: '2024-01-14', present: 37, absent: 5 },
    { date: '2024-01-15', present: 38, absent: 4 },
  ],
  employeeStatusBreakdown: [
    { status: 'ACTIVE',   count: 40 },
    { status: 'INACTIVE', count: 2  },
  ],
};

// ── Test wrapper ──────────────────────────────────────────────────────────────

/**
 * Creates a fresh QueryClient + provider wrapper for each test.
 *
 * @returns {{ wrapper: React.FC, queryClient: QueryClient }}
 */
function makeWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries:   { retry: false, refetchInterval: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });

  const wrapper = ({ children }) => (
    <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>
  );

  return { wrapper, queryClient };
}

// ── useDashboardSummary ───────────────────────────────────────────────────────

describe('useDashboardSummary', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns data on successful fetch', async () => {
    getDashboardSummary.mockResolvedValue(MOCK_SUMMARY);
    const { wrapper } = makeWrapper();
    const { result }  = renderHook(() => useDashboardSummary(), { wrapper });

    expect(result.current.isLoading).toBe(true);

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual(MOCK_SUMMARY);
    expect(result.current.isError).toBe(false);
  });

  it('exposes isError when fetch fails', async () => {
    getDashboardSummary.mockRejectedValue(new Error('Network error'));
    const { wrapper } = makeWrapper();
    const { result }  = renderHook(() => useDashboardSummary(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));

    expect(result.current.data).toBeUndefined();
    expect(result.current.error.message).toBe('Network error');
  });

  it('refresh() invalidates the summary query cache', async () => {
    getDashboardSummary.mockResolvedValue(MOCK_SUMMARY);
    const { wrapper, queryClient } = makeWrapper();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useDashboardSummary(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    result.current.refresh();

    expect(invalidateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['dashboard', 'summary'] }),
    );
  });
});

// ── useDashboardActivity ──────────────────────────────────────────────────────

describe('useDashboardActivity', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns activity array on success', async () => {
    getDashboardActivity.mockResolvedValue(MOCK_ACTIVITY);
    const { wrapper } = makeWrapper();
    const { result }  = renderHook(() => useDashboardActivity({ limit: 5 }), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual(MOCK_ACTIVITY);
    expect(getDashboardActivity).toHaveBeenCalledWith({ limit: 5 });
  });

  it('returns empty array gracefully', async () => {
    getDashboardActivity.mockResolvedValue([]);
    const { wrapper } = makeWrapper();
    const { result }  = renderHook(() => useDashboardActivity(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual([]);
  });

  it('refresh() invalidates the activity query cache', async () => {
    getDashboardActivity.mockResolvedValue(MOCK_ACTIVITY);
    const { wrapper, queryClient } = makeWrapper();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useDashboardActivity(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    result.current.refresh();

    expect(invalidateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['dashboard', 'activity'] }),
    );
  });
});

// ── useDashboardCharts ────────────────────────────────────────────────────────

describe('useDashboardCharts', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns chart data on success', async () => {
    getDashboardCharts.mockResolvedValue(MOCK_CHARTS);
    const { wrapper } = makeWrapper();
    const { result }  = renderHook(() => useDashboardCharts(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));

    expect(result.current.data).toEqual(MOCK_CHARTS);
    expect(result.current.data.departmentDistribution).toHaveLength(2);
    expect(result.current.data.employeeStatusBreakdown).toHaveLength(2);
  });

  it('refresh() invalidates the charts query cache', async () => {
    getDashboardCharts.mockResolvedValue(MOCK_CHARTS);
    const { wrapper, queryClient } = makeWrapper();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useDashboardCharts(), { wrapper });
    await waitFor(() => expect(result.current.isLoading).toBe(false));

    result.current.refresh();

    expect(invalidateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['dashboard', 'charts'] }),
    );
  });
});

// ── useRefreshAllDashboard ────────────────────────────────────────────────────

describe('useRefreshAllDashboard', () => {
  it('invalidates all dashboard queries with the root key', async () => {
    const { wrapper, queryClient } = makeWrapper();
    const invalidateSpy = vi.spyOn(queryClient, 'invalidateQueries');

    const { result } = renderHook(() => useRefreshAllDashboard(), { wrapper });

    result.current();

    expect(invalidateSpy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['dashboard'] }),
    );
  });
});
