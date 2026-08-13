/**
 * @fileoverview React Query hooks for the leave management module.
 *
 * Exports seven hooks:
 *   {@link useLeaves}          — paginated, filtered leave list
 *   {@link useMyLeaves}        — current user's own leave requests
 *   {@link useLeave}           — single leave detail
 *   {@link useCreateLeave}     — submit new leave request
 *   {@link useUpdateLeave}     — update pending request
 *   {@link useDeleteLeave}     — cancel leave request (optimistic)
 *   {@link useApproveLeave}    — approve with optimistic status update
 *   {@link useRejectLeave}     — reject with optimistic status update
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import {
  getLeaveRequests,
  getMyLeaveRequests,
  getLeaveById,
  createLeaveRequest,
  updateLeaveRequest,
  cancelLeave,
  approveLeave,
  rejectLeave,
} from '@/services/leaveApi';
import {
  LEAVE_QUERY_KEYS,
  LEAVE_DEFAULT_PAGE_SIZE,
  LEAVE_DEFAULT_SORT,
  LEAVE_DEFAULT_DIRECTION,
} from '@/constants/leaveConstants';

// ── useLeaves ─────────────────────────────────────────────────────────────────

/**
 * @typedef {Object} LeaveListParams
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sort='createdAt']
 * @property {'asc'|'desc'} [direction='desc']
 * @property {string}  [search='']
 * @property {string}  [status='']
 * @property {string}  [type='']
 * @property {string}  [employeeId='']
 */

/**
 * Fetches a paginated, filtered list of leave requests.
 *
 * @param {LeaveListParams} [params={}]
 * @returns {{
 *   data: import('@/services/leaveApi').PageResponse | undefined,
 *   isLoading: boolean, isFetching: boolean, isError: boolean, error: any, refresh: () => void
 * }}
 */
export function useLeaves(params = {}) {
  const queryClient = useQueryClient();

  const effectiveParams = {
    page: params.page ?? 0,
    size: params.size ?? LEAVE_DEFAULT_PAGE_SIZE,
    sort: params.sort ?? LEAVE_DEFAULT_SORT,
    direction: params.direction ?? LEAVE_DEFAULT_DIRECTION,
    keyword: params.search || undefined,
    status: params.status || undefined,
    type: params.type || undefined,
    employeeId: params.employeeId || undefined,
  };
  Object.keys(effectiveParams).forEach(
    (k) => effectiveParams[k] === undefined && delete effectiveParams[k],
  );

  const query = useQuery({
    queryKey: LEAVE_QUERY_KEYS.list(effectiveParams),
    queryFn: () => getLeaveRequests(effectiveParams),
    staleTime: 30_000,
    placeholderData: (prev) => prev,
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.lists() });
  }, [queryClient]);

  return {
    data: query.data,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
    refresh,
  };
}

// ── useMyLeaves ───────────────────────────────────────────────────────────────

/**
 * Fetches the currently authenticated user's own leave requests.
 * Passes the user's employeeId as a filter (if available from the JWT context).
 *
 * @param {Omit<LeaveListParams, 'employeeId'>} [params={}]
 * @returns {{
 *   data: import('@/services/leaveApi').PageResponse | undefined,
 *   isLoading: boolean, isFetching: boolean, isError: boolean, error: any, refresh: () => void
 * }}
 */
export function useMyLeaves(params = {}) {
  const queryClient = useQueryClient();

  // Use /leaves/my which the backend scopes to the authenticated user's
  // linked employee record — avoids the userId vs employeeId UUID mismatch.
  const effectiveParams = {
    page: params.page ?? 0,
    size: params.size ?? LEAVE_DEFAULT_PAGE_SIZE,
    sort: params.sort ?? LEAVE_DEFAULT_SORT,
    direction: params.direction ?? LEAVE_DEFAULT_DIRECTION,
    status: params.status || undefined,
    type: params.type || undefined,
  };
  Object.keys(effectiveParams).forEach(
    (k) => effectiveParams[k] === undefined && delete effectiveParams[k],
  );

  const query = useQuery({
    queryKey: LEAVE_QUERY_KEYS.my(effectiveParams),
    queryFn: () => getMyLeaveRequests(effectiveParams),
    staleTime: 30_000,
    placeholderData: (prev) => prev,
  });

  const refresh = useCallback(() => {
    // Invalidate by prefix (['leaves', 'my']) to match all param variants
    queryClient.invalidateQueries({ queryKey: ['leaves', 'my'] });
  }, [queryClient]);

  return {
    data: query.data,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
    refresh,
  };
}

// ── useLeave ──────────────────────────────────────────────────────────────────

/**
 * Fetches a single leave request by UUID.
 *
 * @param {string | null | undefined} id
 * @returns {{ data, isLoading, isFetching, isError, error }}
 */
export function useLeave(id) {
  const query = useQuery({
    queryKey: LEAVE_QUERY_KEYS.detail(id),
    queryFn: () => getLeaveById(id),
    enabled: Boolean(id),
    staleTime: 60_000,
  });
  return {
    data: query.data,
    isLoading: query.isLoading,
    isFetching: query.isFetching,
    isError: query.isError,
    error: query.error,
  };
}

// ── useCreateLeave ────────────────────────────────────────────────────────────

/**
 * Mutation to submit a new leave request.
 * On success invalidates all leave lists + my-leaves cache.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useCreateLeave() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (payload) => createLeaveRequest(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.all() });
      // Also invalidate the my-leaves prefix so the self-service timeline refreshes
      queryClient.invalidateQueries({ queryKey: ['leaves', 'my'] });
    },
  });
}

// ── useUpdateLeave ────────────────────────────────────────────────────────────

/**
 * Mutation to update a pending leave request.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useUpdateLeave() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, payload }) => updateLeaveRequest(id, payload),
    onMutate: async ({ id, payload }) => {
      await queryClient.cancelQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      const previous = queryClient.getQueryData(LEAVE_QUERY_KEYS.detail(id));
      queryClient.setQueryData(LEAVE_QUERY_KEYS.detail(id), (old) => ({ ...old, ...payload }));
      return { previous, id };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.previous) queryClient.setQueryData(LEAVE_QUERY_KEYS.detail(ctx.id), ctx.previous);
    },
    onSettled: (_data, _err, { id }) => {
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.lists() });
    },
  });
}

// ── useDeleteLeave (cancel) ───────────────────────────────────────────────────

/**
 * Mutation to cancel (delete) a leave request.
 * Optimistically removes the row from all list caches.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useDeleteLeave() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => cancelLeave(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: LEAVE_QUERY_KEYS.lists() });
      const previousLists = queryClient.getQueriesData({ queryKey: LEAVE_QUERY_KEYS.lists() });
      queryClient.setQueriesData({ queryKey: LEAVE_QUERY_KEYS.lists() }, (old) => {
        if (!old?.content) return old;
        return {
          ...old,
          content: old.content.filter((l) => l.id !== id),
          totalElements: Math.max(0, (old.totalElements ?? 1) - 1),
        };
      });
      return { previousLists };
    },
    onError: (_err, _id, ctx) => {
      ctx?.previousLists?.forEach(([key, data]) => queryClient.setQueryData(key, data));
    },
    onSettled: (_data, _err, id) => {
      queryClient.removeQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.all() });
    },
  });
}

// ── useApproveLeave ───────────────────────────────────────────────────────────

/**
 * Mutation to approve a pending leave request.
 * Optimistically updates the status to APPROVED in the detail cache.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useApproveLeave() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: (id) => approveLeave(id),
    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      const previous = queryClient.getQueryData(LEAVE_QUERY_KEYS.detail(id));
      queryClient.setQueryData(LEAVE_QUERY_KEYS.detail(id), (old) => ({
        ...old,
        status: 'APPROVED',
      }));
      return { previous, id };
    },
    onError: (_err, _id, ctx) => {
      if (ctx?.previous) queryClient.setQueryData(LEAVE_QUERY_KEYS.detail(ctx.id), ctx.previous);
    },
    onSettled: (_data, _err, id) => {
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.lists() });
    },
  });
}

// ── useRejectLeave ────────────────────────────────────────────────────────────

/**
 * Mutation to reject a pending leave request.
 * Optimistically updates the status to REJECTED in the detail cache.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useRejectLeave() {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn: ({ id, reason }) => rejectLeave(id, { rejectionReason: reason }),
    onMutate: async ({ id }) => {
      await queryClient.cancelQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      const previous = queryClient.getQueryData(LEAVE_QUERY_KEYS.detail(id));
      queryClient.setQueryData(LEAVE_QUERY_KEYS.detail(id), (old) => ({
        ...old,
        status: 'REJECTED',
      }));
      return { previous, id };
    },
    onError: (_err, _vars, ctx) => {
      if (ctx?.previous) queryClient.setQueryData(LEAVE_QUERY_KEYS.detail(ctx.id), ctx.previous);
    },
    onSettled: (_data, _err, { id }) => {
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: LEAVE_QUERY_KEYS.lists() });
    },
  });
}
