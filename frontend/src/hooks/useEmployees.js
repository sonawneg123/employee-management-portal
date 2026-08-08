/**
 * @fileoverview React Query hooks for the employee management module.
 *
 * Exposes five focused hooks:
 *   - {@link useEmployees}       — paginated, filtered employee list
 *   - {@link useEmployee}        — single employee detail
 *   - {@link useCreateEmployee}  — create mutation with cache invalidation
 *   - {@link useUpdateEmployee}  — update mutation with optimistic update
 *   - {@link useDeleteEmployee}  — delete mutation with optimistic update
 *
 * All mutations auto-invalidate the list and detail caches on settlement.
 */

import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { useCallback } from 'react';
import {
  getEmployees,
  getEmployeeById,
  createEmployee,
  updateEmployee,
  deleteEmployee,
} from '@/services/employeeApi';
import {
  EMPLOYEE_QUERY_KEYS,
  EMPLOYEE_DEFAULT_PAGE_SIZE,
  EMPLOYEE_DEFAULT_SORT,
  EMPLOYEE_DEFAULT_DIRECTION,
} from '@/constants/employeeConstants';

// ── useEmployees ──────────────────────────────────────────────────────────────

/**
 * @typedef {Object} EmployeeListParams
 * @property {number}  [page=0]
 * @property {number}  [size=20]
 * @property {string}  [sort='createdAt']
 * @property {'asc'|'desc'} [direction='desc']
 * @property {string}  [search='']
 * @property {string}  [departmentId='']
 * @property {string}  [status='']
 */

/**
 * Fetches a paginated, filtered list of employees.
 *
 * @param {EmployeeListParams} [params={}]
 * @returns {{
 *   data: import('@/services/employeeApi').PageResponse<import('@/services/employeeApi').EmployeeResponse> | undefined,
 *   isLoading: boolean,
 *   isFetching: boolean,
 *   isError: boolean,
 *   error: any,
 *   refresh: () => void
 * }}
 */
export function useEmployees(params = {}) {
  const queryClient = useQueryClient();

  const effectiveParams = {
    page:         params.page      ?? 0,
    size:         params.size      ?? EMPLOYEE_DEFAULT_PAGE_SIZE,
    sort:         params.sort      ?? EMPLOYEE_DEFAULT_SORT,
    direction:    params.direction ?? EMPLOYEE_DEFAULT_DIRECTION,
    keyword:      params.search    ?? undefined,
    departmentId: params.departmentId || undefined,
    status:       params.status       || undefined,
  };

  // Strip undefined keys so Axios doesn't send empty params
  Object.keys(effectiveParams).forEach(
    (k) => effectiveParams[k] === undefined && delete effectiveParams[k],
  );

  const query = useQuery({
    queryKey: EMPLOYEE_QUERY_KEYS.list(effectiveParams),
    queryFn:  () => getEmployees(effectiveParams),
    staleTime: 30_000,
    placeholderData: (prev) => prev, // keep previous data visible while fetching
  });

  const refresh = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: EMPLOYEE_QUERY_KEYS.lists() });
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

// ── useEmployee ───────────────────────────────────────────────────────────────

/**
 * Fetches a single employee record by UUID.
 *
 * @param {string | null | undefined} id - Employee UUID.
 * @returns {{
 *   data: import('@/services/employeeApi').EmployeeResponse | undefined,
 *   isLoading: boolean,
 *   isFetching: boolean,
 *   isError: boolean,
 *   error: any
 * }}
 */
export function useEmployee(id) {
  const query = useQuery({
    queryKey: EMPLOYEE_QUERY_KEYS.detail(id),
    queryFn:  () => getEmployeeById(id),
    enabled:  Boolean(id),
    staleTime: 60_000,
  });

  return {
    data:       query.data,
    isLoading:  query.isLoading,
    isFetching: query.isFetching,
    isError:    query.isError,
    error:      query.error,
  };
}

// ── useCreateEmployee ─────────────────────────────────────────────────────────

/**
 * Mutation to create a new employee.
 * On success, invalidates the full employee list cache.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useCreateEmployee() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload) => createEmployee(payload),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: EMPLOYEE_QUERY_KEYS.lists() });
    },
  });
}

// ── useUpdateEmployee ─────────────────────────────────────────────────────────

/**
 * Mutation to update an existing employee.
 * Applies an optimistic update to the detail cache immediately, then
 * invalidates both the detail and list caches on settlement.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useUpdateEmployee() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ id, payload }) => updateEmployee(id, payload),

    onMutate: async ({ id, payload }) => {
      // Cancel in-flight queries for this employee
      await queryClient.cancelQueries({ queryKey: EMPLOYEE_QUERY_KEYS.detail(id) });

      // Snapshot the previous value
      const previous = queryClient.getQueryData(EMPLOYEE_QUERY_KEYS.detail(id));

      // Optimistically update the detail cache
      queryClient.setQueryData(EMPLOYEE_QUERY_KEYS.detail(id), (old) => ({
        ...old,
        ...payload,
      }));

      return { previous, id };
    },

    onError: (_err, _vars, context) => {
      // Roll back on error
      if (context?.previous) {
        queryClient.setQueryData(
          EMPLOYEE_QUERY_KEYS.detail(context.id),
          context.previous,
        );
      }
    },

    onSettled: (_data, _err, { id }) => {
      queryClient.invalidateQueries({ queryKey: EMPLOYEE_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: EMPLOYEE_QUERY_KEYS.lists() });
    },
  });
}

// ── useDeleteEmployee ─────────────────────────────────────────────────────────

/**
 * Mutation to delete an employee by UUID.
 * Applies an optimistic removal from all matching list caches,
 * then invalidates lists and removes the detail entry on settlement.
 *
 * @returns {import('@tanstack/react-query').UseMutationResult}
 */
export function useDeleteEmployee() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (id) => deleteEmployee(id),

    onMutate: async (id) => {
      await queryClient.cancelQueries({ queryKey: EMPLOYEE_QUERY_KEYS.lists() });

      // Snapshot all list caches
      const previousLists = queryClient.getQueriesData({
        queryKey: EMPLOYEE_QUERY_KEYS.lists(),
      });

      // Optimistically remove the deleted row from every cached page
      queryClient.setQueriesData(
        { queryKey: EMPLOYEE_QUERY_KEYS.lists() },
        (old) => {
          if (!old?.content) return old;
          return {
            ...old,
            content:       old.content.filter((e) => e.id !== id),
            totalElements: Math.max(0, (old.totalElements ?? 1) - 1),
          };
        },
      );

      return { previousLists };
    },

    onError: (_err, _id, context) => {
      // Roll back list caches on error
      context?.previousLists?.forEach(([queryKey, data]) => {
        queryClient.setQueryData(queryKey, data);
      });
    },

    onSettled: (_data, _err, id) => {
      queryClient.removeQueries({ queryKey: EMPLOYEE_QUERY_KEYS.detail(id) });
      queryClient.invalidateQueries({ queryKey: EMPLOYEE_QUERY_KEYS.lists() });
    },
  });
}
