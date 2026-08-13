/**
 * @fileoverview React Query hooks for the department management module.
 *
 * Exposes five focused hooks:
 *   - {@link useDepartmentList}     — paginated, filtered department list (management table)
 *   - {@link useDepartment}         — single department detail
 *   - {@link useCreateDepartment}   — create mutation
 *   - {@link useUpdateDepartment}   — update mutation with optimistic update
 *   - {@link useDeleteDepartment}   — delete mutation with optimistic removal
 *
 * All mutations also invalidate the shared ['departments','list'] key so
 * the employee module's form dropdowns stay in sync.
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import {
  getDepartmentsPaged,
  getDepartmentById,
  createDepartment,
  updateDepartment,
  deleteDepartment,
} from '@/services/departmentApi';
import {
  DEPT_QUERY_KEYS,
  DEPARTMENT_DEFAULT_PAGE_SIZE,
  DEPARTMENT_DEFAULT_SORT,
  DEPARTMENT_DEFAULT_DIRECTION,
} from '@/constants/departmentConstants';
// Shared key — invalidate so employee dropdowns refresh too
import { DEPARTMENT_QUERY_KEYS } from '@/constants/employeeConstants';

// ── useDepartmentList ─────────────────────────────────────────────────────────

/**
 * @typedef {Object} DeptListParams
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sort='name']
 * @property {'asc'|'desc'} [direction='asc']
 * @property {string}  [search='']
 */

/**
 * Fetches a paginated, filtered list of departments for the management table.
 *
 * @param {DeptListParams} [params={}]
 * @returns {{
 *   data: import('@/services/departmentApi').DepartmentPageResponse | undefined,
 *   isLoading: boolean,
 *   isFetching: boolean,
 *   isError: boolean,
 *   error: any,
 *   refresh: () => void
 * }}
 */
export function useDepartmentList(params = {}) {
  const queryClient = useQueryClient();

  const effectiveParams = {
    page: params.page ?? 0,
    size: params.size ?? DEPARTMENT_DEFAULT_PAGE_SIZE,
    sort: params.sort ?? DEPARTMENT_DEFAULT_SORT,
    direction: params.direction ?? DEPARTMENT_DEFAULT_DIRECTION,
    keyword: params.search || undefined,
  };

  // Strip undefined keys
  Object.keys(effectiveParams).forEach(
    (k) => effectiveParams[k] === undefined && delete effectiveParams[k],
  );

  const query = useQuery({
    queryKey: DEPT_QUERY_KEYS.list(effectiveParams),
    queryFn: () => getDepartmentsPaged(effectiveParams),
    staleTime: 30_000,
    placeholderData: (prev) => prev,
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: DEPT_QUERY_KEYS.lists() });
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

// ── useDepartment ─────────────────────────────────────────────────────────────

/**
 * Fetches a single department by UUID.
 *
 * @param {string | null | undefined} id
 * @returns {{
 *   data: import('@/services/departmentApi').DepartmentResponse | undefined,
 *   isLoading: boolean,
 *   isFetching: boolean,
 *   isError: boolean,
 *   error: any
 * }}
 */
export function useDepartment(id) {
  const query = useQuery({
    queryKey: DEPT_QUERY_KEYS.detail(id),
    queryFn: () => getDepartmentById(id),
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

// ── useCreateDepartment ───────────────────────────────────────────────────────

/**
 * Mutation to create a new department.
 * On success, invalidates both the management list and the shared dropdown cache.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useCreateDepartment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload) => createDepartment(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: DEPT_QUERY_KEYS.lists() });
      // Keep employee-module dropdown fresh
      queryClient.invalidateQueries({ queryKey: DEPARTMENT_QUERY_KEYS.list() });
    },
  });
}

// ── useUpdateDepartment ───────────────────────────────────────────────────────

/**
 * Mutation to update an existing department.
 * Optimistically patches the detail cache; rolls back on error.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useUpdateDepartment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }) => updateDepartment(id, payload),

    onMutate: async ({ id, payload }) => {
      await queryClient.cancelQueries({ queryKey: DEPT_QUERY_KEYS.detail(id) });
      const previous = queryClient.getQueryData(DEPT_QUERY_KEYS.detail(id));
      queryClient.setQueryData(DEPT_QUERY_KEYS.detail(id), (old) => ({
        ...old,
        ...payload,
      }));
      return { previous, id };
    },

    onError: (_err, _vars, context) => {
      if (context?.previous) {
        queryClient.setQueryData(DEPT_QUERY_KEYS.detail(context.id), context.previous);
      }
    },

    onSettled: (_data, _err, { id }) => {
      queryClient.invalidateQueries({ queryKey: DEPT_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: DEPT_QUERY_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: DEPARTMENT_QUERY_KEYS.list() });
    },
  });
}

// ── useDeleteDepartment ───────────────────────────────────────────────────────

/**
 * Mutation to delete a department by UUID.
 * Optimistically removes the row from all list caches; rolls back on error.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useDeleteDepartment() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id) => deleteDepartment(id),

    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: DEPT_QUERY_KEYS.lists() });
      const previousLists = queryClient.getQueriesData({ queryKey: DEPT_QUERY_KEYS.lists() });

      queryClient.setQueriesData({ queryKey: DEPT_QUERY_KEYS.lists() }, (old) => {
        if (!old?.content) return old;
        return {
          ...old,
          content: old.content.filter((d) => d.id !== id),
          totalElements: Math.max(0, (old.totalElements ?? 1) - 1),
        };
      });

      return { previousLists };
    },

    onError: (_err, _id, context) => {
      context?.previousLists?.forEach(([queryKey, data]) => {
        queryClient.setQueryData(queryKey, data);
      });
    },

    onSettled: (_data, _err, id) => {
      queryClient.removeQueries({ queryKey: DEPT_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: DEPT_QUERY_KEYS.lists() });
      queryClient.invalidateQueries({ queryKey: DEPARTMENT_QUERY_KEYS.list() });
    },
  });
}
