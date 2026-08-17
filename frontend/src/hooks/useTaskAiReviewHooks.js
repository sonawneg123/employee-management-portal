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
} from '@/services/taskAiReviewApi';

// ── Query key factories ──────────────────────────────────────────────────────

export const aiReviewKeys = {
  all: ['task-ai-reviews'],
  latestForSubmission: (submissionId) => [...aiReviewKeys.all, 'latest', submissionId],
  allForSubmission: (submissionId) => [...aiReviewKeys.all, 'history', submissionId],
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
    enabled: Boolean(submissionId) && (opts.enabled !== false),
    staleTime: 8_000,
    // Poll while in-flight to detect completion quickly
    refetchInterval: (data) => {
      const status = data?.status;
      if (status === 'PENDING' || status === 'PROCESSING') return 5_000;
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
    enabled: Boolean(submissionId) && (opts.enabled !== false),
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
