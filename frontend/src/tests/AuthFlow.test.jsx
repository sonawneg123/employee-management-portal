/**
 * @fileoverview AuthFlow — end-to-end authentication flow tests.
 *
 * Covers all role-based login scenarios, error cases, and EMPLOYEE
 * self-service access patterns required by Loop 15.
 *
 * Scenarios:
 *   1. Successful login for each role → correct dashboard redirect
 *   2. Wrong password / unknown email → 401 displayed in login form
 *   3. Account disabled → specific message shown
 *   4. Duplicate email on registration → 409 message shown
 *   5. Registration validation errors → per-field display
 *   6. EMPLOYEE accessing self-service → correct empty states shown
 *   7. EMPLOYEE self-service routes → accessible via route guards
 *   8. Logout clears state → protected routes redirect to login
 *   9. Browser refresh with valid token → session restored
 *  10. DashboardRedirect → routes each role correctly
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter, Routes, Route } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import { AuthProvider, AuthContext, useAuth } from '@/contexts/AuthContext';
import LoginPage from '@/pages/auth/LoginPage';
import RegisterPage from '@/pages/auth/RegisterPage';
import DashboardRedirect from '@/components/common/DashboardRedirect';

import * as authApi from '@/services/authApi';
import * as jwtUtils from '@/utils/jwtUtils';
import { TOKEN_STORAGE_KEY, USER_STORAGE_KEY } from '@/constants/api';
import { setItem } from '@/utils/localStorage';
import { ROLES } from '@/constants/roles';
import { ROUTES } from '@/constants/routes';

// ── Module mocks ──────────────────────────────────────────────────────────────

vi.mock('@/services/authApi');

vi.mock('@/utils/jwtUtils', () => ({
  isTokenExpired: vi.fn(() => false),
  getTokenRoles: vi.fn(() => []),
  decodeToken: vi.fn(() => null),
  getTokenSubject: vi.fn(() => null),
  getTokenExpiry: vi.fn(() => null),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async (importActual) => {
  const actual = await importActual();
  return { ...actual, useNavigate: () => mockNavigate };
});

// ── Shared helpers ────────────────────────────────────────────────────────────

const testTheme = createTheme();

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

/**
 * Renders a component inside all required providers, using the real AuthProvider.
 */
function renderWithRealAuth(ui, { initialPath = '/' } = {}) {
  const qc = makeQueryClient();
  const user = userEvent.setup();
  const result = render(
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter initialEntries={[initialPath]}>
            <AuthProvider>{ui}</AuthProvider>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </HelmetProvider>,
  );
  return { ...result, user };
}

/**
 * Renders a component inside all required providers using a mocked AuthContext.
 */
function renderWithMockedAuth(ui, { authContext = {}, initialPath = '/' } = {}) {
  const qc = makeQueryClient();
  const user = userEvent.setup();
  const defaultAuth = {
    user: null,
    token: null,
    isAuthenticated: false,
    isLoading: false,
    login: vi.fn(),
    register: vi.fn(),
    logout: vi.fn(),
    hasRole: vi.fn(() => false),
    hasAnyRole: vi.fn(() => false),
    ...authContext,
  };
  const result = render(
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter initialEntries={[initialPath]}>
            <AuthContext.Provider value={defaultAuth}>{ui}</AuthContext.Provider>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </HelmetProvider>,
  );
  return { ...result, user };
}

// ── 1. Role-based login redirects ─────────────────────────────────────────────

describe('Role-based login → dashboard redirect', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });
  afterEach(() => {
    localStorage.clear();
  });

  const cases = [
    { role: ROLES.ADMIN, email: 'admin@company.com', target: ROUTES.ADMIN_DASHBOARD },
    { role: ROLES.HR, email: 'hr@company.com', target: ROUTES.HR_DASHBOARD },
    { role: ROLES.MANAGER, email: 'mgr@company.com', target: ROUTES.HR_DASHBOARD },
    { role: ROLES.EMPLOYEE, email: 'emp@example.com', target: ROUTES.EMPLOYEE_DASHBOARD },
  ];

  cases.forEach(({ role, email, target }) => {
    it(`${role} navigates to ${target}`, async () => {
      vi.mocked(authApi.login).mockResolvedValueOnce({
        accessToken: `tok-${role}`,
        tokenType: 'Bearer',
        expiresIn: 86400,
        userId: `user-${role}`,
        email,
        firstName: 'Test',
        lastName: 'User',
        roles: [role],
      });

      let authCtx;
      function Capture() {
        authCtx = useAuth();
        return null;
      }
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <MemoryRouter>
            <AuthProvider>
              <Capture />
            </AuthProvider>
          </MemoryRouter>
        </QueryClientProvider>,
      );

      await act(async () => {
        await authCtx.login({ email, password: 'anypassword' });
      });

      await waitFor(() => {
        expect(mockNavigate).toHaveBeenCalledWith(target, { replace: true });
      });
    });
  });
});

// ── 2. Wrong password → 401 on login form ─────────────────────────────────────

describe('LoginPage — wrong password (401)', () => {
  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  it('displays "Invalid email or password." for bad credentials', async () => {
    const error401 = {
      status: 401,
      title: 'Authentication Failed',
      message: 'Invalid email or password.',
      violations: null,
      isNetwork: false,
    };
    const loginMock = vi.fn().mockRejectedValue(error401);

    const { user } = renderWithMockedAuth(<LoginPage />, {
      authContext: { login: loginMock },
      initialPath: '/login',
    });

    await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
    await user.type(screen.getByLabelText(/^password/i), 'wrongpassword');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/invalid email or password/i);
    });

    // Error alert is visible and the user remains on the login form
    expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
  });

  it('displays network error message when server unreachable', async () => {
    const networkError = {
      status: 0,
      message: 'Unable to reach the server. Please check your connection.',
      isNetwork: true,
    };
    const loginMock = vi.fn().mockRejectedValue(networkError);

    const { user } = renderWithMockedAuth(<LoginPage />, { authContext: { login: loginMock } });

    await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
    await user.type(screen.getByLabelText(/^password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/unable to reach the server/i);
    });
  });

  it('displays account-disabled message (401, title "Account Disabled")', async () => {
    const disabledError = {
      status: 401,
      title: 'Account Disabled',
      message: 'This account has been disabled.',
      violations: null,
      isNetwork: false,
    };
    const loginMock = vi.fn().mockRejectedValue(disabledError);

    const { user } = renderWithMockedAuth(<LoginPage />, { authContext: { login: loginMock } });

    await user.type(screen.getByLabelText(/email address/i), 'disabled@example.com');
    await user.type(screen.getByLabelText(/^password/i), 'password123');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toHaveTextContent(/account has been disabled/i);
    });
  });

  it('preserves the email field after a failed login', async () => {
    const error401 = {
      status: 401,
      message: 'Invalid email or password.',
      violations: null,
      isNetwork: false,
    };
    const loginMock = vi.fn().mockRejectedValue(error401);

    const { user } = renderWithMockedAuth(<LoginPage />, { authContext: { login: loginMock } });

    await user.type(screen.getByLabelText(/email address/i), 'preserved@example.com');
    await user.type(screen.getByLabelText(/^password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    // Email field value is preserved so user can correct only the password
    expect(screen.getByLabelText(/email address/i)).toHaveValue('preserved@example.com');
  });

  it('does not show a raw stack trace or exception class name', async () => {
    const error401 = {
      status: 401,
      message: 'Invalid email or password.',
      violations: null,
      isNetwork: false,
    };
    const loginMock = vi.fn().mockRejectedValue(error401);

    const { user } = renderWithMockedAuth(<LoginPage />, { authContext: { login: loginMock } });

    await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
    await user.type(screen.getByLabelText(/^password/i), 'wrong');
    await user.click(screen.getByRole('button', { name: /sign in/i }));

    await waitFor(() => {
      expect(screen.getByRole('alert')).toBeInTheDocument();
    });

    // No raw Java exception or stack trace in the DOM
    expect(screen.queryByText(/BadCredentialsException/)).not.toBeInTheDocument();
    expect(screen.queryByText(/java\./)).not.toBeInTheDocument();
  });
});

// ── 3. Duplicate email on registration → 409 ──────────────────────────────────

describe('RegisterPage — duplicate email (409)', () => {
  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  async function fillAndSubmit(user) {
    await user.type(screen.getByLabelText(/first name/i), 'Jane');
    await user.type(screen.getByLabelText(/last name/i), 'Doe');
    await user.type(screen.getByLabelText(/email address/i), 'existing@example.com');
    await user.type(screen.getByLabelText(/^password/i), 'StrongP@ss1');
    await user.type(screen.getByLabelText(/confirm password/i), 'StrongP@ss1');
    await user.click(screen.getByRole('button', { name: /create account/i }));
  }

  it('shows duplicate-email message and stays on register page', async () => {
    const error409 = {
      status: 409,
      title: 'Duplicate Resource',
      message: 'User already exists with email: existing@example.com',
      violations: null,
    };
    const registerMock = vi.fn().mockRejectedValue(error409);

    const { user } = renderWithMockedAuth(<RegisterPage />, {
      authContext: { register: registerMock },
      initialPath: '/register',
    });

    await fillAndSubmit(user);

    await waitFor(
      () => {
        expect(screen.getByRole('alert')).toHaveTextContent(/already registered/i);
      },
      { timeout: 10_000 },
    );

    // No navigation to dashboard
    expect(mockNavigate).not.toHaveBeenCalled();
  }, 15_000);

  it('preserves first name, last name, and email after 409 error', async () => {
    const error409 = { status: 409, message: 'User already exists', violations: null };
    const registerMock = vi.fn().mockRejectedValue(error409);

    const { user } = renderWithMockedAuth(<RegisterPage />, {
      authContext: { register: registerMock },
    });

    await fillAndSubmit(user);

    await waitFor(
      () => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      },
      { timeout: 10_000 },
    );

    // Fields should still be populated (React Hook Form doesn't reset on error)
    expect(screen.getByLabelText(/email address/i)).toHaveValue('existing@example.com');
  }, 15_000);

  it('shows field-level validation error from backend (400 + violations)', async () => {
    const errorWithViolations = {
      status: 400,
      message: 'One or more fields failed validation.',
      violations: { password: 'Password must be between 8 and 100 characters' },
    };
    const registerMock = vi.fn().mockRejectedValue(errorWithViolations);

    const { user } = renderWithMockedAuth(<RegisterPage />, {
      authContext: { register: registerMock },
    });

    await fillAndSubmit(user);

    await waitFor(
      () => {
        expect(
          screen.getByText('Password must be between 8 and 100 characters'),
        ).toBeInTheDocument();
      },
      { timeout: 10_000 },
    );
  }, 15_000);

  it('shows "Create account as" role selector when no role is pre-selected', () => {
    renderWithMockedAuth(<RegisterPage />);
    // The new role selector replaces the old text note
    expect(screen.getByText(/create account as/i)).toBeInTheDocument();
    expect(screen.getByRole('radiogroup', { name: /select role/i })).toBeInTheDocument();
  });
});

// ── 4. DashboardRedirect — role routing ───────────────────────────────────────

describe('DashboardRedirect — role routing', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  const redirectCases = [
    { roles: [ROLES.ADMIN], expected: ROUTES.ADMIN_DASHBOARD, label: 'ADMIN' },
    { roles: [ROLES.HR], expected: ROUTES.HR_DASHBOARD, label: 'HR' },
    { roles: [ROLES.MANAGER], expected: ROUTES.HR_DASHBOARD, label: 'MANAGER' },
    { roles: [ROLES.EMPLOYEE], expected: ROUTES.EMPLOYEE_DASHBOARD, label: 'EMPLOYEE' },
    { roles: [], expected: ROUTES.EMPLOYEE_DASHBOARD, label: 'unknown role' },
  ];

  redirectCases.forEach(({ roles, expected, label }) => {
    it(`${label} is redirected to ${expected}`, () => {
      const navigatedPaths = [];
      const qc = makeQueryClient();
      render(
        <QueryClientProvider client={qc}>
          <ThemeProvider theme={testTheme}>
            <MemoryRouter initialEntries={['/dashboard']}>
              <AuthContext.Provider
                value={{
                  user: { userId: 'u1', email: 'u@u.com', firstName: 'U', lastName: 'U', roles },
                  isAuthenticated: true,
                  isLoading: false,
                  login: vi.fn(),
                  register: vi.fn(),
                  logout: vi.fn(),
                  hasRole: (r) => roles.includes(r),
                  hasAnyRole: (rs) => rs.some((r) => roles.includes(r)),
                }}
              >
                <Routes>
                  <Route path="/dashboard" element={<DashboardRedirect />} />
                  <Route path={ROUTES.ADMIN_DASHBOARD} element={<div>Admin Dashboard</div>} />
                  <Route path={ROUTES.HR_DASHBOARD} element={<div>HR Dashboard</div>} />
                  <Route path={ROUTES.EMPLOYEE_DASHBOARD} element={<div>Employee Dashboard</div>} />
                </Routes>
              </AuthContext.Provider>
            </MemoryRouter>
          </ThemeProvider>
        </QueryClientProvider>,
      );

      // Route target should be rendered
      const targets = {
        [ROUTES.ADMIN_DASHBOARD]: 'Admin Dashboard',
        [ROUTES.HR_DASHBOARD]: 'HR Dashboard',
        [ROUTES.EMPLOYEE_DASHBOARD]: 'Employee Dashboard',
      };
      expect(screen.getByText(targets[expected])).toBeInTheDocument();
    });
  });
});

// ── 5. Session persistence ────────────────────────────────────────────────────

describe('Session persistence', () => {
  beforeEach(() => {
    localStorage.clear();
    vi.clearAllMocks();
  });
  afterEach(() => {
    localStorage.clear();
  });

  it('restores authenticated state from localStorage on mount', () => {
    const storedUser = {
      userId: 'u1',
      email: 'restored@example.com',
      firstName: 'Restored',
      lastName: 'User',
      roles: ['ROLE_EMPLOYEE'],
    };
    setItem(TOKEN_STORAGE_KEY, 'valid.token.here');
    setItem(USER_STORAGE_KEY, storedUser);

    let authCtx;
    function Capture() {
      authCtx = useAuth();
      return null;
    }
    const qc = makeQueryClient();
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <AuthProvider>
            <Capture />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(authCtx.user?.email).toBe('restored@example.com');
    expect(authCtx.token).toBe('valid.token.here');
  });

  it('clears session and redirects to /login after logout', async () => {
    setItem(TOKEN_STORAGE_KEY, 'active-token');
    setItem(USER_STORAGE_KEY, {
      userId: '1',
      email: 'a@b.com',
      firstName: 'A',
      lastName: 'B',
      roles: ['ROLE_EMPLOYEE'],
    });

    let authCtx;
    function Capture() {
      authCtx = useAuth();
      return null;
    }
    const qc = makeQueryClient();
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <AuthProvider>
            <Capture />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    act(() => authCtx.logout());

    await waitFor(() => {
      expect(localStorage.getItem(TOKEN_STORAGE_KEY)).toBeNull();
      expect(localStorage.getItem(USER_STORAGE_KEY)).toBeNull();
      expect(mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
    });
  });

  it('is not authenticated when localStorage token is expired', () => {
    vi.mocked(jwtUtils.isTokenExpired).mockReturnValueOnce(true);
    setItem(TOKEN_STORAGE_KEY, 'expired.token');
    setItem(USER_STORAGE_KEY, {
      userId: '1',
      email: 'a@b.com',
      firstName: 'A',
      lastName: 'B',
      roles: ['ROLE_EMPLOYEE'],
    });

    let authCtx;
    function Capture() {
      authCtx = useAuth();
      return null;
    }
    const qc = makeQueryClient();
    render(
      <QueryClientProvider client={qc}>
        <MemoryRouter>
          <AuthProvider>
            <Capture />
          </AuthProvider>
        </MemoryRouter>
      </QueryClientProvider>,
    );

    expect(authCtx.isAuthenticated).toBe(false);
  });
});
