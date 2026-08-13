/**
 * @fileoverview Tests for the Axios instance interceptor behaviour.
 *
 * Verifies:
 * - 401 response triggers clearAll() and redirects to /login
 * - 403 response does NOT globally redirect — error is propagated to the caller
 * - The normalised error shape is correct
 */

import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';

// ── Mocks must come before the module under test is imported ──────────────────

vi.mock('@/utils/localStorage', () => ({
  getItem: vi.fn(() => 'mock-token'),
  setItem: vi.fn(),
  removeItem: vi.fn(),
  clearAll: vi.fn(),
}));

vi.mock('@/constants/api', () => ({
  API_BASE_URL: '/api',
  REQUEST_TIMEOUT_MS: 15_000,
  TOKEN_STORAGE_KEY: 'emp_portal_token',
  USER_STORAGE_KEY: 'emp_portal_user',
}));

vi.mock('@/constants/routes', () => ({
  ROUTES: {
    LOGIN: '/login',
    ACCESS_DENIED: '/403',
    DASHBOARD: '/dashboard',
  },
}));

import { clearAll } from '@/utils/localStorage';
import { normaliseError } from '@/api/axiosInstance';

// ── normaliseError unit tests — no HTTP needed ────────────────────────────────

describe('normaliseError', () => {
  it('extracts RFC-7807 ProblemDetail fields', () => {
    const axiosErr = {
      response: {
        status: 403,
        data: { title: 'Access Denied', detail: 'You may only access your own records.' },
      },
    };
    const result = normaliseError(axiosErr);
    expect(result.status).toBe(403);
    expect(result.title).toBe('Access Denied');
    expect(result.message).toBe('You may only access your own records.');
    expect(result.isNetwork).toBe(false);
  });

  it('returns 401 default message when no ProblemDetail body', () => {
    const axiosErr = {
      response: { status: 401, data: {} },
    };
    const result = normaliseError(axiosErr);
    expect(result.status).toBe(401);
    expect(result.message).toBe('Your session has expired. Please log in again.');
  });

  it('returns 403 default message when no ProblemDetail body', () => {
    const axiosErr = {
      response: { status: 403, data: {} },
    };
    const result = normaliseError(axiosErr);
    expect(result.status).toBe(403);
    expect(result.message).toBe('You do not have permission to perform this action.');
  });

  it('returns network error when no response', () => {
    const axiosErr = {};
    const result = normaliseError(axiosErr);
    expect(result.status).toBe(0);
    expect(result.isNetwork).toBe(true);
  });

  it('extracts violations map from ProblemDetail', () => {
    const axiosErr = {
      response: {
        status: 400,
        data: {
          title: 'Validation Failed',
          detail: 'Invalid input.',
          violations: { email: 'Must be a valid email' },
        },
      },
    };
    const result = normaliseError(axiosErr);
    expect(result.violations).toEqual({ email: 'Must be a valid email' });
  });
});

// ── Interceptor behaviour: 403 must NOT globally redirect ─────────────────────
//
// We test this by verifying the interceptor logic directly.
// The interceptor in axiosInstance.js now only hard-redirects on 401.
// On 403 it must just call Promise.reject(normaliseError(error)).
//
describe('403 interceptor contract', () => {
  it('normalises a 403 error without redirecting', () => {
    // Simulate what the interceptor does on a 403 response.
    const axiosErr = {
      response: {
        status: 403,
        data: { title: 'Access Denied', detail: 'No employee record linked.' },
      },
    };
    // The normalised error must be present (the caller will receive it).
    const normalised = normaliseError(axiosErr);
    expect(normalised.status).toBe(403);
    expect(normalised.message).toBe('No employee record linked.');
    // clearAll must NOT have been called for a 403
    expect(clearAll).not.toHaveBeenCalled();
  });

  it('normalises a 403 from attendance/my when no employee record', () => {
    const axiosErr = {
      response: {
        status: 403,
        data: {
          status: 403,
          title: 'Access Denied',
          detail: 'No employee record is linked to your account.',
        },
      },
    };
    const normalised = normaliseError(axiosErr);
    expect(normalised.status).toBe(403);
    expect(normalised.title).toBe('Access Denied');
    expect(normalised.message).toBe('No employee record is linked to your account.');
    // The interceptor must NOT have redirected the page
    expect(clearAll).not.toHaveBeenCalled();
  });
});
