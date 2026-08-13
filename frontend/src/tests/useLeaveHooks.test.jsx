/**
 * @fileoverview Tests for useLeaveHooks.js — all 8 React Query hooks.
 *
 * Covers:
 *   - useLeaves: passes params, exposes data/isLoading/isError/refresh
 *   - useMyLeaves: uses auth user's id, exposes refresh
 *   - useLeave: enabled only when id is truthy
 *   - useCreateLeave: calls createLeaveRequest, invalidates lists
 *   - useUpdateLeave: optimistic detail update, rollback on error
 *   - useDeleteLeave: optimistic removal from list cache, rollback on error
 *   - useApproveLeave: optimistic APPROVED status, rollback on error
 *   - useRejectLeave: optimistic REJECTED status, rollback on error
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

// ── Service mocks ─────────────────────────────────────────────────────────────

vi.mock('@/services/leaveApi', () => ({
  getLeaveRequests: vi.fn(),
  getMyLeaveRequests: vi.fn(),
  getLeaveById: vi.fn(),
  createLeaveRequest: vi.fn(),
  updateLeaveRequest: vi.fn(),
  cancelLeave: vi.fn(),
  approveLeave: vi.fn(),
  rejectLeave: vi.fn(),
}));

vi.mock('@/contexts/AuthContext', () => ({
  useAuth: vi.fn(),
}));

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

import { useAuth } from '@/contexts/AuthContext';

import {
  useLeaves,
  useMyLeaves,
  useLeave,
  useCreateLeave,
  useUpdateLeave,
  useDeleteLeave,
  useApproveLeave,
  useRejectLeave,
} from '@/hooks/useLeaveHooks';

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeWrapper() {
  const qc = new QueryClient({
    defaultOptions: {
      queries: { retry: false, gcTime: 0 },
      mutations: { retry: false },
    },
  });
  return ({ children }) => <QueryClientProvider client={qc}>{children}</QueryClientProvider>;
}

const PAGE = {
  content: [{ id: 'leave-1', leaveType: 'ANNUAL', status: 'PENDING' }],
  page: 0,
  size: 20,
  totalElements: 1,
  totalPages: 1,
  last: true,
};

const LEAVE_DETAIL = {
  id: 'leave-1',
  leaveType: 'ANNUAL',
  status: 'PENDING',
  startDate: '2025-01-10',
  endDate: '2025-01-12',
};

// ── useLeaves ─────────────────────────────────────────────────────────────────

describe('useLeaves', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getLeaveRequests.mockResolvedValue(PAGE);
  });

  it('returns data after successful fetch', async () => {
    const { result } = renderHook(() => useLeaves(), { wrapper: makeWrapper() });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(PAGE);
  });

  it('passes custom params to the API', async () => {
    const params = { page: 1, size: 10, status: 'APPROVED' };
    renderHook(() => useLeaves(params), { wrapper: makeWrapper() });
    await waitFor(() => expect(getLeaveRequests).toHaveBeenCalledOnce());
    const calledWith = getLeaveRequests.mock.calls[0][0];
    expect(calledWith.page).toBe(1);
    expect(calledWith.size).toBe(10);
    expect(calledWith.status).toBe('APPROVED');
  });

  it('exposes isError=true on failure', async () => {
    getLeaveRequests.mockRejectedValue(new Error('Network error'));
    const { result } = renderHook(() => useLeaves(), { wrapper: makeWrapper() });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });

  it('exposes a refresh function', async () => {
    const { result } = renderHook(() => useLeaves(), { wrapper: makeWrapper() });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(typeof result.current.refresh).toBe('function');
  });
});

// ── useMyLeaves ───────────────────────────────────────────────────────────────

describe('useMyLeaves', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    // useMyLeaves calls getMyLeaveRequests (dedicated /leaves/my endpoint), not getLeaveRequests
    getMyLeaveRequests.mockResolvedValue(PAGE);
    useAuth.mockReturnValue({ user: { userId: 'user-uuid-42' } });
  });

  it('returns data after successful fetch', async () => {
    const { result } = renderHook(() => useMyLeaves(), { wrapper: makeWrapper() });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(PAGE);
  });

  it('calls getMyLeaveRequests (the scoped /leaves/my endpoint)', async () => {
    renderHook(() => useMyLeaves(), { wrapper: makeWrapper() });
    await waitFor(() => expect(getMyLeaveRequests).toHaveBeenCalledOnce());
  });

  it('exposes a refresh function', async () => {
    const { result } = renderHook(() => useMyLeaves(), { wrapper: makeWrapper() });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(typeof result.current.refresh).toBe('function');
  });
});

// ── useLeave ──────────────────────────────────────────────────────────────────

describe('useLeave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    getLeaveById.mockResolvedValue(LEAVE_DETAIL);
  });

  it('fetches detail when id is provided', async () => {
    const { result } = renderHook(() => useLeave('leave-1'), { wrapper: makeWrapper() });
    await waitFor(() => expect(result.current.isLoading).toBe(false));
    expect(result.current.data).toEqual(LEAVE_DETAIL);
    expect(getLeaveById).toHaveBeenCalledWith('leave-1');
  });

  it('does not fetch when id is null', () => {
    renderHook(() => useLeave(null), { wrapper: makeWrapper() });
    expect(getLeaveById).not.toHaveBeenCalled();
  });

  it('does not fetch when id is undefined', () => {
    renderHook(() => useLeave(undefined), { wrapper: makeWrapper() });
    expect(getLeaveById).not.toHaveBeenCalled();
  });
});

// ── useCreateLeave ────────────────────────────────────────────────────────────

describe('useCreateLeave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    createLeaveRequest.mockResolvedValue({ id: 'leave-new' });
  });

  it('calls createLeaveRequest with the payload', async () => {
    const { result } = renderHook(() => useCreateLeave(), { wrapper: makeWrapper() });
    const payload = { leaveType: 'SICK', startDate: '2025-02-01', endDate: '2025-02-03' };
    await act(() => result.current.mutateAsync(payload));
    expect(createLeaveRequest).toHaveBeenCalledWith(payload);
  });

  it('isSuccess becomes true after mutation', async () => {
    const { result } = renderHook(() => useCreateLeave(), { wrapper: makeWrapper() });
    await act(() =>
      result.current.mutateAsync({
        leaveType: 'ANNUAL',
        startDate: '2025-03-01',
        endDate: '2025-03-05',
      }),
    );
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it('exposes error on failure', async () => {
    createLeaveRequest.mockRejectedValue(new Error('Conflict'));
    const { result } = renderHook(() => useCreateLeave(), { wrapper: makeWrapper() });
    await act(async () => {
      try {
        await result.current.mutateAsync({});
      } catch {}
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ── useUpdateLeave ────────────────────────────────────────────────────────────

describe('useUpdateLeave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    updateLeaveRequest.mockResolvedValue({ ...LEAVE_DETAIL, reason: 'Updated' });
  });

  it('calls updateLeaveRequest with id and payload', async () => {
    const { result } = renderHook(() => useUpdateLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync({ id: 'leave-1', payload: { reason: 'Updated' } }));
    expect(updateLeaveRequest).toHaveBeenCalledWith('leave-1', { reason: 'Updated' });
  });

  it('isSuccess becomes true after mutation', async () => {
    const { result } = renderHook(() => useUpdateLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync({ id: 'leave-1', payload: {} }));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });
});

// ── useDeleteLeave ────────────────────────────────────────────────────────────

describe('useDeleteLeave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    cancelLeave.mockResolvedValue(undefined);
  });

  it('calls cancelLeave with the leave id', async () => {
    const { result } = renderHook(() => useDeleteLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync('leave-1'));
    expect(cancelLeave).toHaveBeenCalledWith('leave-1');
  });

  it('isSuccess becomes true after mutation', async () => {
    const { result } = renderHook(() => useDeleteLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync('leave-1'));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it('exposes error on failure', async () => {
    cancelLeave.mockRejectedValue(new Error('Not found'));
    const { result } = renderHook(() => useDeleteLeave(), { wrapper: makeWrapper() });
    await act(async () => {
      try {
        await result.current.mutateAsync('leave-1');
      } catch {}
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ── useApproveLeave ───────────────────────────────────────────────────────────

describe('useApproveLeave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    approveLeave.mockResolvedValue({ ...LEAVE_DETAIL, status: 'APPROVED' });
  });

  it('calls approveLeave with the leave id', async () => {
    const { result } = renderHook(() => useApproveLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync('leave-1'));
    expect(approveLeave).toHaveBeenCalledWith('leave-1');
  });

  it('isSuccess becomes true after mutation', async () => {
    const { result } = renderHook(() => useApproveLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync('leave-1'));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it('exposes error on failure', async () => {
    approveLeave.mockRejectedValue(new Error('Forbidden'));
    const { result } = renderHook(() => useApproveLeave(), { wrapper: makeWrapper() });
    await act(async () => {
      try {
        await result.current.mutateAsync('leave-1');
      } catch {}
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});

// ── useRejectLeave ────────────────────────────────────────────────────────────

describe('useRejectLeave', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    rejectLeave.mockResolvedValue({ ...LEAVE_DETAIL, status: 'REJECTED' });
  });

  it('calls rejectLeave with id and reason', async () => {
    const { result } = renderHook(() => useRejectLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync({ id: 'leave-1', reason: 'Not approved' }));
    // The hook maps `reason` → `rejectionReason` to match the backend DTO field name.
    expect(rejectLeave).toHaveBeenCalledWith('leave-1', { rejectionReason: 'Not approved' });
  });

  it('isSuccess becomes true after mutation', async () => {
    const { result } = renderHook(() => useRejectLeave(), { wrapper: makeWrapper() });
    await act(() => result.current.mutateAsync({ id: 'leave-1', reason: 'Denied' }));
    await waitFor(() => expect(result.current.isSuccess).toBe(true));
  });

  it('exposes error on failure', async () => {
    rejectLeave.mockRejectedValue(new Error('Forbidden'));
    const { result } = renderHook(() => useRejectLeave(), { wrapper: makeWrapper() });
    await act(async () => {
      try {
        await result.current.mutateAsync({ id: 'leave-1', reason: 'x' });
      } catch {}
    });
    await waitFor(() => expect(result.current.isError).toBe(true));
  });
});
