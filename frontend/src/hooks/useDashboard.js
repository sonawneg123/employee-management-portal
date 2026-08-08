/**
 * @fileoverview React Query hooks for dashboard data.
 *
 * Three focused hooks — one per endpoint — using consistent query key
 * factories from {@link DASHBOARD_QUERY_KEYS}. All hooks auto-refresh
 * every {@link DASHBOARD_REFRESH_INTERVAL_MS} and provide loading /
 * error / data states to the consuming components.
 */

import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import {
  getDashboardSummary,
  getDashboardActivity,
  getDashboardCharts,
} from '@/services/dashboardApi';
import {
  DASHBOARD_QUERY_KEYS,
  DASHBOARD_REFRESH_INTERVAL_MS,
} from '@/constants/dashboard';

// ── useDashboardSummary ───────────────────────────────────────────────────────

/**
 * @typedef {Object} UseDashboardSummaryReturn
 * @property {import('@/services/dashboardApi').DashboardSummary | undefined} data
 * @property {boolean}  isLoading   - True on the initial fetch with no cached data.
 * @property {boolean}  isFetching  - True whenever a background refetch is in flight.
 * @property {boolean}  isError
 * @property {any}      error
 * @property {() => void} refresh   - Manually invalidates and refetches.
 */

/**
 * Fetches and caches the dashboard KPI summary.
 * Auto-refreshes every {@link DASHBOARD_REFRESH_INTERVAL_MS}.
 *
 * @returns {UseDashboardSummaryReturn}
 */
export function useDashboardSummary() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey:        DASHBOARD_QUERY_KEYS.summary(),
    queryFn:         getDashboardSummary,
    staleTime:       60_000,            // 1 minute — KPIs change frequently
    refetchInterval: DASHBOARD_REFRESH_INTERVAL_MS,
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEYS.summary() });
  }, [queryClient]);

  return {
    data:       query.data,
    isLoading:  query.isLoading,
    isFetching: query.isFetching,
    isError:    query.isError,
    error:      query.error,
    refresh,
  };
}

// ── useDashboardActivity ─────────────────────────────────────────────────────

/**
 * @typedef {Object} UseDashboardActivityReturn
 * @property {import('@/services/dashboardApi').ActivityItem[] | undefined} data
 * @property {boolean}  isLoading
 * @property {boolean}  isFetching
 * @property {boolean}  isError
 * @property {any}      error
 * @property {() => void} refresh
 */

/**
 * Fetches and caches the recent activity feed.
 *
 * @param {{ limit?: number }} [params={}]
 * @returns {UseDashboardActivityReturn}
 */
export function useDashboardActivity(params = {}) {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey:        [...DASHBOARD_QUERY_KEYS.activity(), params],
    queryFn:         () => getDashboardActivity(params),
    staleTime:       30_000,            // 30 seconds — activity is real-time
    refetchInterval: DASHBOARD_REFRESH_INTERVAL_MS,
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEYS.activity() });
  }, [queryClient]);

  return {
    data:       query.data,
    isLoading:  query.isLoading,
    isFetching: query.isFetching,
    isError:    query.isError,
    error:      query.error,
    refresh,
  };
}

// ── useDashboardCharts ───────────────────────────────────────────────────────

/**
 * @typedef {Object} UseDashboardChartsReturn
 * @property {import('@/services/dashboardApi').DashboardCharts | undefined} data
 * @property {boolean}  isLoading
 * @property {boolean}  isFetching
 * @property {boolean}  isError
 * @property {any}      error
 * @property {() => void} refresh
 */

/**
 * Fetches and caches all dashboard chart datasets.
 *
 * @returns {UseDashboardChartsReturn}
 */
export function useDashboardCharts() {
  const queryClient = useQueryClient();

  const query = useQuery({
    queryKey:        DASHBOARD_QUERY_KEYS.charts(),
    queryFn:         getDashboardCharts,
    staleTime:       2 * 60_000,        // 2 minutes — chart data changes slowly
    refetchInterval: DASHBOARD_REFRESH_INTERVAL_MS,
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEYS.charts() });
  }, [queryClient]);

  return {
    data:       query.data,
    isLoading:  query.isLoading,
    isFetching: query.isFetching,
    isError:    query.isError,
    error:      query.error,
    refresh,
  };
}

// ── useRefreshAllDashboard ───────────────────────────────────────────────────

/**
 * Returns a single function that invalidates all three dashboard queries
 * simultaneously. Used by the DashboardHeader refresh button.
 *
 * @returns {() => void}
 */
export function useRefreshAllDashboard() {
  const queryClient = useQueryClient();
  return useCallback(() => {
    queryClient.invalidateQueries({ queryKey: DASHBOARD_QUERY_KEYS.all() });
  }, [queryClient]);
}
