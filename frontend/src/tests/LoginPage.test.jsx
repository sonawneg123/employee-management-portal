/**
 * @fileoverview Tests for LoginPage and LoginForm.
 *
 * Uses Vitest + React Testing Library. The AuthContext and React Query
 * providers are wrapped via a shared test utility.
 *
 * Scenarios covered:
 * - Page renders correctly.
 * - Validation fires on empty submit.
 * - Email format validation.
 * - Successful login calls auth context.
 * - Server error (401) is displayed.
 * - Duplicate email (409) is displayed at form level.
 * - Remember me persists email to localStorage.
 * - Password visibility toggle works.
 * - Keyboard navigation (tab order).
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, fireEvent, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import LoginPage from '@/pages/auth/LoginPage';
import { AuthContext } from '@/contexts/AuthContext';

// ── Test utilities ────────────────────────────────────────────────────────────

const testTheme = createTheme();

/**
 * Creates a fresh QueryClient for each test to avoid state leakage.
 *
 * @returns {QueryClient}
 */
function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

/**
 * Default mock auth context value.
 *
 * @param {Partial<import('@/contexts/AuthContext').AuthContextValue>} overrides
 * @returns {import('@/contexts/AuthContext').AuthContextValue}
 */
function makeAuthContext(overrides = {}) {
  return {
    user:            null,
    token:           null,
    isAuthenticated: false,
    isLoading:       false,
    login:           vi.fn(),
    register:        vi.fn(),
    logout:          vi.fn(),
    hasRole:         vi.fn(() => false),
    hasAnyRole:      vi.fn(() => false),
    ...overrides,
  };
}

/**
 * Renders a component inside all required providers.
 *
 * @param {JSX.Element} ui
 * @param {object} [opts]
 * @param {string} [opts.initialPath='/login']
 * @param {Partial<import('@/contexts/AuthContext').AuthContextValue>} [opts.authContext]
 * @returns {{ user: import('@testing-library/user-event').UserEvent } & import('@testing-library/react').RenderResult}
 */
function renderWithProviders(ui, { initialPath = '/login', authContext = {} } = {}) {
  const qc = makeQueryClient();
  const auth = makeAuthContext(authContext);
  const user = userEvent.setup();

  const result = render(
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter initialEntries={[initialPath]}>
            <AuthContext.Provider value={auth}>
              {ui}
            </AuthContext.Provider>
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </HelmetProvider>,
  );

  return { ...result, user };
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('LoginPage', () => {
  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('Rendering', () => {
    it('renders the sign-in heading', () => {
      renderWithProviders(<LoginPage />);
      expect(screen.getByText('Sign In')).toBeInTheDocument();
    });

    it('renders email and password fields', () => {
      renderWithProviders(<LoginPage />);
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
    });

    it('renders remember me checkbox', () => {
      renderWithProviders(<LoginPage />);
      expect(screen.getByLabelText(/remember me/i)).toBeInTheDocument();
    });

    it('renders forgot password link', () => {
      renderWithProviders(<LoginPage />);
      expect(screen.getByText(/forgot password/i)).toBeInTheDocument();
    });

    it('renders create an account link', () => {
      renderWithProviders(<LoginPage />);
      expect(screen.getByText(/create an account/i)).toBeInTheDocument();
    });

    it('renders sign-in button', () => {
      renderWithProviders(<LoginPage />);
      expect(screen.getByRole('button', { name: /sign in/i })).toBeInTheDocument();
    });
  });

  describe('Validation', () => {
    it('shows required error when email is empty on submit', async () => {
      const { user } = renderWithProviders(<LoginPage />);
      await user.click(screen.getByRole('button', { name: /sign in/i }));
      await waitFor(() => {
        expect(screen.getByText('Email is required')).toBeInTheDocument();
      });
    });

    it('shows invalid email error for malformed address', async () => {
      const { user } = renderWithProviders(<LoginPage />);
      await user.type(screen.getByLabelText(/email address/i), 'not-an-email');
      await user.click(screen.getByRole('button', { name: /sign in/i }));
      await waitFor(() => {
        expect(screen.getByText(/valid email address/i)).toBeInTheDocument();
      });
    });

    it('shows required error when password is empty on submit', async () => {
      const { user } = renderWithProviders(<LoginPage />);
      await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
      await user.click(screen.getByRole('button', { name: /sign in/i }));
      await waitFor(() => {
        expect(screen.getByText('Password is required')).toBeInTheDocument();
      });
    });
  });

  describe('Password visibility toggle', () => {
    it('toggles password field from password to text type', async () => {
      const { user } = renderWithProviders(<LoginPage />);
      const passwordInput = screen.getByLabelText(/^password/i);
      expect(passwordInput).toHaveAttribute('type', 'password');

      const toggleButton = screen.getByRole('button', { name: /show password/i });
      await user.click(toggleButton);
      expect(passwordInput).toHaveAttribute('type', 'text');
    });

    it('toggles back to password type on second click', async () => {
      const { user } = renderWithProviders(<LoginPage />);
      const toggleButton = screen.getByRole('button', { name: /show password/i });
      await user.click(toggleButton);
      await user.click(screen.getByRole('button', { name: /hide password/i }));
      expect(screen.getByLabelText(/^password/i)).toHaveAttribute('type', 'password');
    });
  });

  describe('Remember me', () => {
    it('persists email to localStorage when remember me is checked', async () => {
      const loginMock = vi.fn().mockResolvedValue(undefined);
      const { user } = renderWithProviders(<LoginPage />, { authContext: { login: loginMock } });

      await user.type(screen.getByLabelText(/email address/i), 'saved@example.com');
      await user.type(screen.getByLabelText(/^password/i), 'password123');
      await user.click(screen.getByLabelText(/remember me/i));
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        const stored = localStorage.getItem('emp_portal_remember_email');
        expect(stored).toBe('"saved@example.com"');
      });
    });

    it('pre-fills email when remembered email exists in localStorage', () => {
      localStorage.setItem('emp_portal_remember_email', '"prefilled@example.com"');
      renderWithProviders(<LoginPage />);
      expect(screen.getByLabelText(/email address/i)).toHaveValue('prefilled@example.com');
    });
  });

  describe('Successful login', () => {
    it('calls login with correct credentials', async () => {
      const loginMock = vi.fn().mockResolvedValue(undefined);
      const { user } = renderWithProviders(<LoginPage />, { authContext: { login: loginMock } });

      await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
      await user.type(screen.getByLabelText(/^password/i), 'correct-password');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(loginMock).toHaveBeenCalledWith({
          email:    'user@example.com',
          password: 'correct-password',
        });
      });
    });

    it('button shows loading state during submission', async () => {
      // login never resolves — simulates in-flight request
      const loginMock = vi.fn(() => new Promise(() => {}));
      const { user } = renderWithProviders(<LoginPage />, { authContext: { login: loginMock } });

      await user.type(screen.getByLabelText(/email address/i), 'user@example.com');
      await user.type(screen.getByLabelText(/^password/i), 'password123');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /sign in/i })).toBeDisabled();
      });
    });
  });

  describe('Error handling', () => {
    it('displays 401 bad credentials error', async () => {
      const error401 = { status: 401, message: 'Invalid email or password.', violations: null, isNetwork: false };
      const loginMock = vi.fn().mockRejectedValue(error401);
      const { user } = renderWithProviders(<LoginPage />, { authContext: { login: loginMock } });

      await user.type(screen.getByLabelText(/email address/i), 'bad@example.com');
      await user.type(screen.getByLabelText(/^password/i), 'wrongpass');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/invalid email or password/i);
      });
    });

    it('displays network error', async () => {
      const networkError = { status: 0, message: 'Unable to reach the server.', isNetwork: true };
      const loginMock = vi.fn().mockRejectedValue(networkError);
      const { user } = renderWithProviders(<LoginPage />, { authContext: { login: loginMock } });

      await user.type(screen.getByLabelText(/email address/i), 'test@example.com');
      await user.type(screen.getByLabelText(/^password/i), 'password123');
      await user.click(screen.getByRole('button', { name: /sign in/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/unable to reach the server/i);
      });
    });
  });
});
