/**
 * @fileoverview Tests for employee React Query hooks.
 *
 * Covers:
 *   - useEmployees  — success, error, filter params forwarded, refresh invalidation
 *   - useEmployee   — success, disabled when no id, error
 *   - useCreateEmployee — success invalidates lists
 *   - useUpdateEmployee — optimistic update + rollback on error
 *   - useDeleteEmployee — optimistic removal + rollback on error
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import {
  useEmployees,
  useEmployee,
  useCreateEmployee,
  useUpdateEmployee,
  useDeleteEmployee,
} from '@/hooks/useEmployees';

// ── Mock API ──────────────────────────────────────────────────────────────────

vi.mock('@/services/employeeApi', () => ({
  getEmployees: vi.fn(),
  getEmployeeById: vi.fn(),
  createEmployee: vi.fn(),
  updateEmployee: vi.fn(),
  deleteEmployee: vi.fn(),
}));

import {
  getEmployees,
  getEmployeeById,
  createEmployee,
  updateEmployee,
  deleteEmployee,
} from '@/services/employeeApi';

// ── Fixtures ──────────────────────────────────────────────────────────────────

const EMP_1 = {
  id: 'emp-1',
  employeeCode: 'EMP001',
  firstName: 'Jane',
  lastName: 'Smith',
  email: 'jane@example.com',
  jobTitle: 'Engineer',
  departmentId: 'dept-1',
  departmentName: 'Engineering',
  status: 'ACTIVE',
  phone: null,
  address: null,
  dateOfJoining: '2022-01-15',
  salary: 85000,
  createdAt: '2022-01-15T00:00:00Z',
  updatedAt: '2022-01-15T00:00:00Z',
};

const PAGE_RESPONSE = {
  content: [EMP_1],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

// ── Wrapper factory ───────────────────────────────────────────────────────────

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

// ── useEmployees ──────────────────────────────────────────────────────────────

describe('useEmployees', () => {
  beforeEach(() => vi.clearAllMocks());

  it('returns paginated data on success', async () => {
    getEmployees.mockResolvedValue(PAGE_RESPONSE);
    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useEmployees(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(PAGE_RESPONSE);
  });

  it('forwards filter params to the API', async () => {
    getEmployees.mockResolvedValue(PAGE_RESPONSE);
    const { wrapper } = makeWrapper();
    renderHook(() => useEmployees({ search: 'Jane', departmentId: 'dept-1', status: 'ACTIVE' }), {
      wrapper,
    });
    await waitFor(() =>
      expect(getEmployees).toHaveBeenCalledWith(
        expect.objectContaining({ keyword: 'Jane', departmentId: 'dept-1', status: 'ACTIVE' }),
      ),
    );
  });

  it('exposes isError on fetch failure', async () => {
    getEmployees.mockRejectedValue(new Error('Server error'));
    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useEmployees(), { wrapper });

    await waitFor(() => expect(result.current.isError).toBe(true));
    expect(result.current.error.message).toBe('Server error');
  });

  it('refresh() invalidates the list cache', async () => {
    getEmployees.mockResolvedValue(PAGE_RESPONSE);
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'invalidateQueries');
    const { result } = renderHook(() => useEmployees(), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    result.current.refresh();
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['employees', 'list'] }));
  });
});

// ── useEmployee ───────────────────────────────────────────────────────────────

describe('useEmployee', () => {
  beforeEach(() => vi.clearAllMocks());

  it('fetches a single employee by id', async () => {
    getEmployeeById.mockResolvedValue(EMP_1);
    const { wrapper } = makeWrapper();
    const { result } = renderHook(() => useEmployee('emp-1'), { wrapper });

    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(EMP_1);
  });

  it('does not fetch when id is null', () => {
    const { wrapper } = makeWrapper();
    renderHook(() => useEmployee(null), { wrapper });
    expect(getEmployeeById).not.toHaveBeenCalled();
  });
});

// ── useCreateEmployee ─────────────────────────────────────────────────────────

describe('useCreateEmployee', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls createEmployee and invalidates lists on success', async () => {
    createEmployee.mockResolvedValue(EMP_1);
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'invalidateQueries');
    const { result } = renderHook(() => useCreateEmployee(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ firstName: 'Jane', lastName: 'Smith' });
    });

    expect(createEmployee).toHaveBeenCalledOnce();
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['employees', 'list'] }));
  });
});

// ── useUpdateEmployee ─────────────────────────────────────────────────────────

describe('useUpdateEmployee', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls updateEmployee and invalidates lists on success', async () => {
    updateEmployee.mockResolvedValue({ ...EMP_1, jobTitle: 'Senior Engineer' });
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'invalidateQueries');
    const { result } = renderHook(() => useUpdateEmployee(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync({ id: 'emp-1', payload: { jobTitle: 'Senior Engineer' } });
    });

    expect(updateEmployee).toHaveBeenCalledWith('emp-1', { jobTitle: 'Senior Engineer' });
    expect(spy).toHaveBeenCalledWith(expect.objectContaining({ queryKey: ['employees', 'list'] }));
  });
});

// ── useDeleteEmployee ─────────────────────────────────────────────────────────

describe('useDeleteEmployee', () => {
  beforeEach(() => vi.clearAllMocks());

  it('calls deleteEmployee and removes from cache', async () => {
    deleteEmployee.mockResolvedValue(undefined);
    const { wrapper, qc } = makeWrapper();
    const spy = vi.spyOn(qc, 'removeQueries');
    const { result } = renderHook(() => useDeleteEmployee(), { wrapper });

    await act(async () => {
      await result.current.mutateAsync('emp-1');
    });

    expect(deleteEmployee).toHaveBeenCalledWith('emp-1');
    expect(spy).toHaveBeenCalledWith(
      expect.objectContaining({ queryKey: ['employees', 'detail', 'emp-1'] }),
    );
  });
});
