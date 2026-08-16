/**
 * @fileoverview React Query hooks for Task Submission management (Phase 6B).
 *
 * Covers:
 *  - Fetching submissions for a task
 *  - Creating, resubmitting, approving, requesting changes
 *  - Query invalidation so both employee and manager views stay in sync
 */

import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import {
  approveSubmission,
  createSubmission,
  getLatestSubmission,
  getSubmissionsForTask,
  requestChanges,
  resubmit,
} from '@/services/taskSubmissionApi';
import { taskKeys } from '@/hooks/useTaskHooks';

// ── Query key factories ──────────────────────────────────────────────────────

export const submissionKeys = {
  all: ['task-submissions'],
  forTask: (taskId) => [...submissionKeys.all, 'task', taskId],
  latestForTask: (taskId) => [...submissionKeys.all, 'task', taskId, 'latest'],
};

// ── Query hooks ──────────────────────────────────────────────────────────────

/**
 * Returns all submissions for the given task.
 *
 * @param {string} taskId
 * @param {{ enabled?: boolean }} [opts={}]
 */
export function useTaskSubmissions(taskId, opts = {}) {
  return useQuery({
    queryKey: submissionKeys.forTask(taskId),
    queryFn: () => getSubmissionsForTask(taskId),
    enabled: Boolean(taskId) && (opts.enabled !== false),
    staleTime: 15_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: false,
  });
}

/**
 * Returns the latest submission for the given task.
 *
 * @param {string} taskId
 * @param {{ enabled?: boolean }} [opts={}]
 */
export function useLatestSubmission(taskId, opts = {}) {
  return useQuery({
    queryKey: submissionKeys.latestForTask(taskId),
    queryFn: () => getLatestSubmission(taskId),
    enabled: Boolean(taskId) && (opts.enabled !== false),
    staleTime: 15_000,
    refetchInterval: 20_000,
    refetchIntervalInBackground: false,
    // 404 is expected when no submission exists — don't throw
    retry: (failureCount, error) => {
      if (error?.response?.status === 404) return false;
      return failureCount < 3;
    },
  });
}

// ── Mutation hooks ───────────────────────────────────────────────────────────

/**
 * Creates a new task submission (employee submits work for review).
 * Invalidates task queries so status change is reflected immediately.
 */
export function useCreateSubmission() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ taskId, payload, file }) => createSubmission(taskId, payload, file ?? null),
    onSuccess: (data, { taskId }) => {
      // Invalidate task detail (status changed to SUBMITTED)
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
      // Invalidate submissions for this task
      queryClient.invalidateQueries({ queryKey: submissionKeys.forTask(taskId) });
      queryClient.invalidateQueries({ queryKey: submissionKeys.latestForTask(taskId) });
    },
  });
}

/**
 * Resubmits after manager requested changes.
 * Invalidates task and submission queries.
 */
export function useResubmit() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ submissionId, payload, file }) => resubmit(submissionId, payload, file ?? null),
    onSuccess: (data) => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
      queryClient.invalidateQueries({ queryKey: submissionKeys.all });
    },
  });
}

/**
 * Manager approves a task submission.
 * Invalidates task and submission queries on both sides.
 */
export function useApproveSubmission() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (submissionId) => approveSubmission(submissionId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
      queryClient.invalidateQueries({ queryKey: submissionKeys.all });
    },
  });
}

/**
 * Manager requests changes on a task submission.
 * Invalidates task and submission queries on both sides.
 */
export function useRequestChanges() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ submissionId, payload }) => requestChanges(submissionId, payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: taskKeys.all });
      queryClient.invalidateQueries({ queryKey: submissionKeys.all });
    },
  });
}
