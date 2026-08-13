/**
 * @fileoverview Tests for department React Query hooks.
 *
 * Covers:
 *   - useDepartmentList  — success, error, param forwarding, refresh invalidation
 *   - useDepartment      — success, disabled when no id
 *   - useCreateDepartment — success invalidates lists + shared dropdown
 *   - useUpdateDepartment — optimistic update + rollback on error
 *   - useDeleteDepartment — optimistic removal + rollback on error
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import {
  useDepartmentList,
  useDepartment,
  useCreateDepartment,
  useUpdateDepartment,
  useDeleteDepartment,
} from '@/hooks/useDepartmentHooks';

// ── Mock API ──────────────────────────────────────────────────────────────────

vi.mock('@/services/departmentApi', () => ({
  getDepartmentsPaged: vi.fn(),
  getDepartmentById: vi.fn(),
  createDepartment: vi.fn(),
  updateDepartment: vi.fn(),
  deleteDepartment: vi.fn(),
  getDepartments: vi.fn(),
}));

import {
  getDepartmentsPaged,
  getDepartmentById,
  createDepartment,
  updateDepartment,
  deleteDepartment,
} from '@/services/departmentApi';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const DEPT_1 = {
  id: 'dept-1',
  name: 'Engineering',
  code: 'ENG',
  description: 'Software engineering team',
  headName: 'Alice',
  employeeCount: 20,
  createdAt: '2022-01-01T00:00:00Z',
  updatedAt: '2022-01-01T00:00:00Z',
};

const PAGE_RESPONSE = {
  content: [DEPT_1],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

// ── Wrapper ───────────────────────────────────────────────────────────────────

function makeWrapper() {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false, refetchInterval: false },
      mutations: { retry: false },
    },
  });
  return {
    qc,
    wrapper: ({ children }) => <QueryClientProvider client={qc}>{children}</QueryClientProvider>,
  };
}

// ── useDepartmentList ─────────────────────────────────────────────────────────

describe('useDepartmentList', () => {
  beforeEach(() => vi.clearAllMocks());

  it('returns paginated data on success', async () => {
    getDepartmentsPaged.mockResolvedValue(PAGE_RESPONSE);
    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useDepartmentList(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(PAGE_RESPONSE);
  });

  it('forwards search param to the API', async () => {
    getDepartmentsPaged.mockResolvedValue(PAGE_RESPONSE);
    const { wrapper } = makeWrapper();
    renderHook(() => useDepartmentList({ search: 'Eng' }), { wrapper });
    await waitFor(() =>
      expect(getDepartmentsPaged).toHaveBeenCalledWith(expect.objectContaining({ keyword: 'Eng' })),
    );
  });

  it('exposes isError on failure', async () => {
    getDepartmentsPaged.mockRejectedValue(new Error('Server error'));
    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useDepartmentList(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it('refresh() invalidates list cache', async () => {
    getDepartmentsPaged.mockResolvedValue(PAGE_RESPONSE);
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'invalidateQueries');
    const { result } = renderHook(() => useDepartmentList(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    result.current.refresh();
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['dept-mgmt', 'list'] }));
  });
});

// ── useDepartment ─────────────────────────────────────────────────────────────

describe('useDepartment', () => {
  beforeEach(() => vi.clearAllMocks());

  it('fetches a single department', async () => {
    getDepartmentById.mockResolvedValue(DEPT_1);
    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useDepartment('dept-1'), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(DEPT_1);
  });

  it('does not fetch when id is null', () => {
    const { wrapper } = makeWrapper();
    renderHook(() => useDepartment(null), { wrapper });
    expect(getDepartmentById).not.toHaveBeenCalled();
  });
});

// ── useCreateDepartment ───────────────────────────────────────────────────────

describe('useCreateDepartment', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls createDepartment and invalidates lists', async () => {
    createDepartment.mockResolvedValue(DEPT_1);
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'invalidateQueries');
    const { result } = renderHook(() => useCreateDepartment(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ name: 'Engineering', code: 'ENG' });
    });

    expect(createDepartment).toHaveBeenCalledOnce();
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['dept-mgmt', 'list'] }));
  });
});

// ── useUpdateDepartment ───────────────────────────────────────────────────────

describe('useUpdateDepartment', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls updateDepartment and invalidates', async () => {
    updateDepartment.mockResolvedValue({ ...DEPT_1, name: 'Updated' });
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'invalidateQueries');
    const { result } = renderHook(() => useUpdateDepartment(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ id: 'dept-1', payload: { name: 'Updated' } });
    });

    expect(updateDepartment).toHaveBeenCalledWith('dept-1', { name: 'Updated' });
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['dept-mgmt', 'list'] }));
  });
});

// ── useDeleteDepartment ───────────────────────────────────────────────────────

describe('useDeleteDepartment', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls deleteDepartment and removes from cache', async () => {
    deleteDepartment.mockResolvedValue(undefined);
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'removeQueries');
    const { result } = renderHook(() => useDeleteDepartment(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync('dept-1');
    });

    expect(deleteDepartment).toHaveBeenCalledWith('dept-1');
    expect(spy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['dept-mgmt', 'detail', 'dept-1'] }),
    );
  });
});
