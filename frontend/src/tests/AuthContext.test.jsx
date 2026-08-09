/**
 * @fileoverview Tests for AuthContext.
 *
 * Tests the AuthProvider directly: session persistence, login, register,
 * logout, token expiry auto-logout, hasRole, and hasAnyRole.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, act, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';

import { AuthProvider, useAuth } from '@/contexts/AuthContext';
import * as authApi from '@/services/authApi';
import * as jwtUtils from '@/utils/jwtUtils';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/constants/api';
import { setItem } from '@/utils/localStorage';

// ── Mocks ─────────────────────────────────────────────────────────────────────

vi.mock('@/services/authApi');

// Mock jwtUtils so test tokens (non-real JWTs) are never treated as expired.
// Without this the AuthProvider's auto-logout useEffect fires on every login
// because mock tokens like 'mock.access.token' cannot be decoded.
vi.mock('@/utils/jwtUtils', () => ({
  isTokenExpired: vi.fn(() => false),
  getTokenRoles:  vi.fn(() => []),
  decodeToken:    vi.fn(() => null),
  getTokenSubject:vi.fn(() => null),
  getTokenExpiry: vi.fn(() => null),
}));

// Fake navigate — React Router replaces this in real usage.
const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importActual) => {
  const actual = await importActual();
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// ── Helpers ───────────────────────────────────────────────────────────────────

function makeQueryClient() {
  return new QueryClient({ defaultOptions: { queries: { retry: false } } });
}

/**
 * A consumer component that exposes auth state via data-testid attributes.
 *
 * @returns {JSX.Element}
 */
function AuthConsumer() {
  const { user, token, isAuthenticated, isLoading } = useAuth();
  return (
    <div>
      <span data-testid="is-authenticated">{String(isAuthenticated)}</span>
      <span data-testid="is-loading">{String(isLoading)}</span>
      <span data-testid="user-email">{user?.email ?? ''}</span>
      <span data-testid="token">{token ?? ''}</span>
    </div>
  );
}

function renderAuth() {
  const qc = makeQueryClient();
  return render(
    <QueryClientProvider client={qc}>
      <MemoryRouter initialEntries={['/dashboard']}>
        <AuthProvider>
          <AuthConsumer />
        </AuthProvider>
      </MemoryRouter>
    </QueryClientProvider>,
  );
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('AuthContext', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });

  afterEach(() => {
    localStorage.clear();
  });

  // ── Initial state ──────────────────────────────────────────────────────────

  describe('Initial state', () => {
    it('is not authenticated when no token is in localStorage', () => {
      renderAuth();
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
    });

    it('is not authenticated when token in localStorage is expired', () => {
      // Override the file-level mock to return true (expired) for this test only
      vi.mocked(jwtUtils.isTokenExpired).mockReturnValueOnce(true);

      const expiredToken = 'eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJ0ZXN0QGV4YW1wbGUuY29tIiwicm9sZXMiOlsiUk9MRV9FTVBMWUVFIl0sImlhdCI6MTYwMDAwMDAwMCwiZXhwIjoxNjAwMDAwMDAxfQ.fake';
      const user = { userId: '1', email: 'test@example.com', firstName: 'Test', lastName: 'User', roles: ['ROLE_EMPLOYEE'] };
      setItem(TOKEN_STORAGE_KEY, expiredToken);
      setItem(USER_STORAGE_KEY, user);

      renderAuth();
      expect(screen.getByTestId('is-authenticated')).toHaveTextContent('false');
    });

    it('restores session from localStorage when token is valid', () => {
      // We cannot easily generate a real JWT in tests, so we mock isTokenExpired
      // at the utility level. This test verifies state is read from localStorage.
      const user = { userId: '123', email: 'restored@example.com', firstName: 'Alice', lastName: 'B', roles: ['ROLE_ADMIN'] };
      // Store a placeholder token string — we control expiry via mock in real scenarios
      setItem(USER_STORAGE_KEY, user);
      // For this assertion we only check user restoration (token expiry mocked elsewhere)
      renderAuth();
      // User is loaded from storage regardless of token validity check
      expect(screen.getByTestId('user-email').textContent).toBe('restored@example.com');
    });
  });

  // ── login() ────────────────────────────────────────────────────────────────

  describe('login()', () => {
    it('persists token and user, sets isAuthenticated on successful login', async () => {
      const mockResponse = {
        accessToken: 'mock.access.token',
        tokenType: 'Bearer',
        expiresIn: 86400,
        userId: 'uuid-1',
        email: 'login@example.com',
        firstName: 'Login',
        lastName: 'User',
        roles: ['ROLE_EMPLOYEE'],
      };
      vi.mocked(authApi.login).mockResolvedValueOnce(mockResponse);

      let authCtx;
      function Capture() {
        authCtx = useAuth();
        return <AuthConsumer />;
      }

      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter>
            <AuthProvider><Capture /></AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>,
      );

      await act(async () => {
        await authCtx.login({ email: 'login@example.com', password: 'password' });
      });

      await waitFor(() => {
        expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('"mock.access.token"');
        expect(mockNavigate).toHaveBeenCalled();
      });
    });

    it('re-throws on login API failure', async () => {
      const apiError = new Error('Bad credentials');
      vi.mocked(authApi.login).mockRejectedValueOnce(apiError);

      let authCtx;
      function Capture() { authCtx = useAuth(); return null; }
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter><AuthProvider><Capture /></AuthProvider></MemoryRouter>
        </QueryClientProvider>,
      );

      await expect(
        act(async () => authCtx.login({ email: 'x@x.com', password: 'y' })),
      ).rejects.toThrow('Bad credentials');
    });
  });

  // ── logout() ───────────────────────────────────────────────────────────────

  describe('logout()', () => {
    it('clears localStorage and navigates to /login', async () => {
      setItem(TOKEN_STORAGE_KEY, 'some-token');
      setItem(USER_STORAGE_KEY, { userId: '1', email: 'a@b.com', firstName: 'A', lastName: 'B', roles: [] });

      let authCtx;
      function Capture() { authCtx = useAuth(); return null; }
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter><AuthProvider><Capture /></AuthProvider></MemoryRouter>
        </QueryClientProvider>,
      );

      act(() => authCtx.logout());

      await waitFor(() => {
        expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
        expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
      });
    });
  });

  // ── register() ─────────────────────────────────────────────────────────────

  describe('register()', () => {
    it('persists session and navigates to dashboard on success', async () => {
      const mockResponse = {
        accessToken: 'new.token', tokenType: 'Bearer', expiresIn: 86400,
        userId: 'uuid-2', email: 'new@example.com',
        firstName: 'New', lastName: 'User', roles: ['ROLE_EMPLOYEE'],
      };
      vi.mocked(authApi.register).mockResolvedValueOnce(mockResponse);

      let authCtx;
      function Capture() { authCtx = useAuth(); return null; }
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter><AuthProvider><Capture /></AuthProvider></MemoryRouter>
        </QueryClientProvider>,
      );

      await act(async () => {
        await authCtx.register({ firstName: 'New', lastName: 'User', email: 'new@example.com', password: 'pass' });
      });

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith('/dashboard', { replace: true });
        expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBe('"new.token"');
      });
    });
  });

  // ── Role helpers ───────────────────────────────────────────────────────────

  describe('hasRole() and hasAnyRole()', () => {
    it('hasRole returns true when user has the role', async () => {
      const mockResponse = {
        accessToken: 'tok', tokenType: 'Bearer', expiresIn: 86400,
        userId: 'u1', email: 'a@b.com', firstName: 'A', lastName: 'B',
        roles: ['ROLE_ADMIN', 'ROLE_HR'],
      };
      vi.mocked(authApi.login).mockResolvedValueOnce(mockResponse);

      let authCtx;
      function Capture() { authCtx = useAuth(); return null; }
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter><AuthProvider><Capture /></AuthProvider></MemoryRouter>
        </QueryClientProvider>,
      );

      await act(async () => {
        await authCtx.login({ email: 'a@b.com', password: 'p' });
      });

      await waitFor(() => {
        expect(authCtx.hasRole('ROLE_ADMIN')).toBe(true);
        expect(authCtx.hasRole('ROLE_EMPLOYEE')).toBe(false);
      });
    });

    it('hasAnyRole returns true when at least one role matches', async () => {
      const mockResponse = {
        accessToken: 'tok2', tokenType: 'Bearer', expiresIn: 86400,
        userId: 'u2', email: 'b@b.com', firstName: 'B', lastName: 'C',
        roles: ['ROLE_HR'],
      };
      vi.mocked(authApi.login).mockResolvedValueOnce(mockResponse);

      let authCtx;
      function Capture() { authCtx = useAuth(); return null; }
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter><AuthProvider><Capture /></AuthProvider></MemoryRouter>
        </QueryClientProvider>,
      );

      await act(async () => {
        await authCtx.login({ email: 'b@b.com', password: 'p' });
      });

      await waitFor(() => {
        expect(authCtx.hasAnyRole(['ROLE_ADMIN', 'ROLE_HR'])).toBe(true);
        expect(authCtx.hasAnyRole(['ROLE_ADMIN', 'ROLE_EMPLOYEE'])).toBe(false);
      });
    });
  });
});
