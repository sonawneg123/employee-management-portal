/**
 * @fileoverview React Query hooks for AI Task Review (Phase 7B).
 *
 * Covers:
 *  - Fetching the latest AI review for a submission
 *  - Fetching all AI reviews for a submission (history)
 *  - Requesting/running a new AI review
 *  - Query invalidation so the UI stays in sync
 *
 * Authorization: ADMIN, HR, MANAGER only.
 * Hooks return 404-safe query results; callers should check isError / error.response.status.
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  getAllAiReviews,
  getLatestAiReview,
  requestAiReview,
  getEmployeeAiFeedback,
  getEmployeeAiHistory,
  getAiScoreTrend,
  getAiTaskInsights,
  getAiDashboardSummary,
} from '@/services/taskAiReviewApi';

// ── Query key factories ──────────────────────────────────────────────────────

export const aiReviewKeys = {
  all: ['task-ai-reviews'],
  latestForSubmission: (submissionId) => [...aiReviewKeys.all, 'latest', submissionId],
  allForSubmission: (submissionId) => [...aiReviewKeys.all, 'history', submissionId],
  // Phase 7D
  employeeFeedback: (submissionId) => [...aiReviewKeys.all, 'employee-feedback', submissionId],
  employeeHistory: (submissionId) => [...aiReviewKeys.all, 'employee-history', submissionId],
  scoreTrend: (taskId) => [...aiReviewKeys.all, 'score-trend', taskId],
  taskInsights: (taskId) => [...aiReviewKeys.all, 'task-insights', taskId],
  dashboardSummary: () => [...aiReviewKeys.all, 'dashboard-summary'],
};

// ── Query hooks ──────────────────────────────────────────────────────────────

/**
 * Returns the latest AI review for the given submission.
 *
 * Returns undefined when no review exists (404 is handled gracefully).
 * Polls every 10 seconds when review is PENDING/PROCESSING to detect completion.
 *
 * @param {string|null|undefined} submissionId UUID of the task submission
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useLatestAiReview(submissionId, opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.latestForSubmission(submissionId),
    queryFn: () => getLatestAiReview(submissionId),
    enabled: Boolean(submissionId) && opts.enabled !== false,
    staleTime: 8_000,
    // Poll while in-flight to detect completion quickly.
    // Also poll when there is no review yet (404) so the UI discovers the
    // automatically triggered review created after the submission commits.
    refetchInterval: (query) => {
      const status = query?.state?.data?.status;
      const isError = query?.state?.status === 'error';
      const is404 = isError && query?.state?.error?.response?.status === 404;
      if (status === 'PENDING' || status === 'PROCESSING') return 5_000;
      // Poll for up to ~60 s after a 404 to discover the auto-triggered review
      if (is404) return 5_000;
      return false;
    },
    refetchIntervalInBackground: false,
    // 404 = no review yet — expected, not a real error
    retry: (failureCount, error) => {
      if (error?.response?.status === 404) return false;
      return failureCount < 2;
    },
  });
}

/**
 * Returns all AI reviews for the given submission (history), newest first.
 *
 * @param {string|null|undefined} submissionId UUID of the task submission
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAllAiReviews(submissionId, opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.allForSubmission(submissionId),
    queryFn: () => getAllAiReviews(submissionId),
    enabled: Boolean(submissionId) && opts.enabled !== false,
    staleTime: 15_000,
    retry: (failureCount, error) => {
      if (error?.response?.status === 404) return false;
      return failureCount < 2;
    },
  });
}

// ── Mutation hooks ───────────────────────────────────────────────────────────

/**
 * Requests a new AI review for a submission.
 *
 * On success, invalidates both the latest-review and history queries so
 * the UI reflects the new review immediately.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useRunAiReview() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (submissionId) => requestAiReview(submissionId),
    onSuccess: (data) => {
      const submissionId = data?.submissionId;
      if (submissionId) {
        queryClient.invalidateQueries({
          queryKey: aiReviewKeys.latestForSubmission(submissionId),
        });
        queryClient.invalidateQueries({
          queryKey: aiReviewKeys.allForSubmission(submissionId),
        });
      } else {
        // Invalidate all AI review queries as a fallback
        queryClient.invalidateQueries({ queryKey: aiReviewKeys.all });
      }
    },
  });
}

/**
 * Phase 7D: Returns employee-safe AI feedback for the given submission.
 * Polls when status is PENDING or PROCESSING.
 * Employees may only view feedback for their own submissions (IDOR enforced backend).
 *
 * @param {string|null|undefined} submissionId UUID of the task submission
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useEmployeeAiFeedback(submissionId, opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.employeeFeedback(submissionId),
    queryFn: () => getEmployeeAiFeedback(submissionId),
    enabled: Boolean(submissionId) && opts.enabled !== false,
    staleTime: 8_000,
    // Poll while PENDING/PROCESSING or 404 (awaiting auto-trigger)
    refetchInterval: (query) => {
      const status = query?.state?.data?.status;
      const isError = query?.state?.status === 'error';
      const is404 = isError && query?.state?.error?.response?.status === 404;
      if (status === 'PENDING' || status === 'PROCESSING') return 5_000;
      if (is404) return 5_000;
      return false;
    },
    refetchIntervalInBackground: false,
    retry: (failureCount, error) => {
      if (error?.response?.status === 404) return false;
      return failureCount < 2;
    },
  });
}

/**
 * Phase 7D: Returns the AI evaluation history for the given submission (employee-safe view).
 *
 * @param {string|null|undefined} submissionId UUID of the task submission
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useEmployeeAiHistory(submissionId, opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.employeeHistory(submissionId),
    queryFn: () => getEmployeeAiHistory(submissionId),
    enabled: Boolean(submissionId) && opts.enabled !== false,
    staleTime: 15_000,
    retry: (failureCount, error) => {
      if (error?.response?.status === 404) return false;
      return failureCount < 2;
    },
  });
}

/**
 * Phase 7D: Returns the AI score trend for the given task (manager view).
 * Requires ADMIN, HR, or MANAGER role.
 *
 * @param {string|null|undefined} taskId UUID of the task
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAiScoreTrend(taskId, opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.scoreTrend(taskId),
    queryFn: () => getAiScoreTrend(taskId),
    enabled: Boolean(taskId) && opts.enabled !== false,
    staleTime: 30_000,
    retry: 1,
  });
}

/**
 * Phase 7D: Returns AI task insights for the given task (manager view).
 * No new AI API calls — reads stored evaluation data only.
 *
 * @param {string|null|undefined} taskId UUID of the task
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAiTaskInsights(taskId, opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.taskInsights(taskId),
    queryFn: () => getAiTaskInsights(taskId),
    enabled: Boolean(taskId) && opts.enabled !== false,
    staleTime: 60_000,
    retry: 1,
  });
}

/**
 * Phase 7D: Returns the AI summary for the manager dashboard.
 * Requires ADMIN, HR, or MANAGER role.
 *
 * @param {{ enabled?: boolean }} [opts={}]
 * @returns {import('@tanstack/react-query').UseQueryResult}
 */
export function useAiDashboardSummary(opts = {}) {
  return useQuery({
    queryKey: aiReviewKeys.dashboardSummary(),
    queryFn: getAiDashboardSummary,
    enabled: opts.enabled !== false,
    staleTime: 60_000,
    retry: 1,
  });
}
