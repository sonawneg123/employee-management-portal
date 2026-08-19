/**
 * @fileoverview React Query hooks for the Analytics Dashboard — Phase 8A.
 *
 * All hooks accept an optional {@code filters} object and use TanStack Query
 * for caching and background refetch.
 *
 * Stale time: 2 minutes (analytics data doesn't need real-time updates).
 */

import { useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getAnalyticsSummary,
  getAnalyticsAttendance,
  getAnalyticsLeaves,
  getAnalyticsTasks,
  getAnalyticsPerformance,
  getAnalyticsDepartments,
} from '@/services/analyticsApi';

// ── Query keys ────────────────────────────────────────────────────────────────

export const ANALYTICS_QUERY_KEYS = {
  summary: (filters) => ['analytics', 'summary', filters],
  attendance: (filters) => ['analytics', 'attendance', filters],
  leaves: (filters) => ['analytics', 'leaves', filters],
  tasks: (filters) => ['analytics', 'tasks', filters],
  performance: (filters) => ['analytics', 'performance', filters],
  departments: () => ['analytics', 'departments'],
};

const STALE_TIME = 2 * 60 * 1000; // 2 minutes

// ── Hooks ─────────────────────────────────────────────────────────────────────

/**
 * Returns the analytics summary KPIs.
 *
 * @param {import('@/services/analyticsApi').AnalyticsFilters} [filters={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAnalyticsSummary(filters = {}) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.summary(filters),
    queryFn: () => getAnalyticsSummary(filters),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

/**
 * Returns attendance analytics with daily trend.
 *
 * @param {import('@/services/analyticsApi').AnalyticsFilters} [filters={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAnalyticsAttendance(filters = {}) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.attendance(filters),
    queryFn: () => getAnalyticsAttendance(filters),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

/**
 * Returns leave analytics with breakdown and trend.
 *
 * @param {import('@/services/analyticsApi').AnalyticsFilters} [filters={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAnalyticsLeaves(filters = {}) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.leaves(filters),
    queryFn: () => getAnalyticsLeaves(filters),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

/**
 * Returns task analytics with status breakdown.
 *
 * @param {import('@/services/analyticsApi').AnalyticsFilters} [filters={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAnalyticsTasks(filters = {}) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.tasks(filters),
    queryFn: () => getAnalyticsTasks(filters),
    staleTime: STALE_TIME,
    retry: 1,
  });
}

/**
 * Returns AI performance analytics.
 * Should only be called for ADMIN / HR / MANAGER roles.
 *
 * @param {Pick<import('@/services/analyticsApi').AnalyticsFilters,'from'|'to'>} [filters={}]
 * @param {boolean} [enabled=true] - Pass false to skip the query for EMPLOYEE role.
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAnalyticsPerformance(filters = {}, enabled = true) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.performance(filters),
    queryFn: () => getAnalyticsPerformance(filters),
    staleTime: STALE_TIME,
    enabled,
    retry: 1,
  });
}

/**
 * Returns department headcount analytics.
 * Should only be called for ADMIN / HR / MANAGER roles.
 *
 * @param {boolean} [enabled=true]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAnalyticsDepartments(enabled = true) {
  return useQuery({
    queryKey: ANALYTICS_QUERY_KEYS.departments(),
    queryFn: getAnalyticsDepartments,
    staleTime: STALE_TIME,
    enabled,
    retry: 1,
  });
}

/**
 * Returns a function that invalidates all analytics caches, triggering
 * a background refetch.
 *
 * @returns {() => Promise<void>}
 */
export function useRefreshAnalytics() {
  const queryClient = useQueryClient();
  return () => queryClient.invalidateQueries({ queryKey: ['analytics'] });
}
