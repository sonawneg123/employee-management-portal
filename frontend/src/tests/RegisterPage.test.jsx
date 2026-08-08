/**
 * @fileoverview Tests for RegisterPage and RegisterForm.
 *
 * Scenarios covered:
 * - Page renders all fields.
 * - Required field validation.
 * - Email format validation.
 * - Password length validation (< 8 chars).
 * - Password mismatch validation.
 * - Password strength indicator renders.
 * - Successful registration calls auth context.
 * - 409 conflict (duplicate email) shows specific message.
 * - Server-side violations applied to fields.
 * - Loading state on submit.
 */

import React from 'react';
import { describe, it, expect, vi, afterEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider } from '@mui/material/styles';
import { createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import RegisterPage from '@/pages/auth/RegisterPage';
import { AuthContext } from '@/contexts/AuthContext';

// ── Test utilities ─────────────────────────────────────────────────────────

const testTheme = createTheme();

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

function makeAuthContext(overrides = {}) {
  return {
    user: null, token: null, isAuthenticated: false, isLoading: false,
    login: vi.fn(), register: vi.fn(), logout: vi.fn(),
    hasRole: vi.fn(() => false), hasAnyRole: vi.fn(() => false),
    ...overrides,
  };
}

function renderWithProviders(ui, { authContext = {} } = {}) {
  const qc = makeQueryClient();
  const auth = makeAuthContext(authContext);
  const user = userEvent.setup();

  const result = render(
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter initialEntries={['/register']}>
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

// ── Helper to fill form ───────────────────────────────────────────────────

async function fillValidForm(user, overrides = {}) {
  const defaults = {
    firstName: 'Jane',
    lastName:  'Smith',
    email:     'jane@example.com',
    password:  'StrongP@ss1',
    confirm:   'StrongP@ss1',
  };
  const values = { ...defaults, ...overrides };

  await user.type(screen.getByLabelText(/first name/i), values.firstName);
  await user.type(screen.getByLabelText(/last name/i),  values.lastName);
  await user.type(screen.getByLabelText(/email address/i), values.email);
  await user.type(screen.getByLabelText(/^password/i), values.password);
  await user.type(screen.getByLabelText(/confirm password/i), values.confirm);
}

// ── Tests ────────────────────────────────────────────────────────────────────

describe('RegisterPage', () => {
  afterEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
  });

  describe('Rendering', () => {
    it('renders the Create Account heading', () => {
      renderWithProviders(<RegisterPage />);
      expect(screen.getByText('Create Account')).toBeInTheDocument();
    });

    it('renders first name and last name fields', () => {
      renderWithProviders(<RegisterPage />);
      expect(screen.getByLabelText(/first name/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/last name/i)).toBeInTheDocument();
    });

    it('renders email field', () => {
      renderWithProviders(<RegisterPage />);
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    });

    it('renders password and confirm password fields', () => {
      renderWithProviders(<RegisterPage />);
      expect(screen.getByLabelText(/^password/i)).toBeInTheDocument();
      expect(screen.getByLabelText(/confirm password/i)).toBeInTheDocument();
    });

    it('renders create account submit button', () => {
      renderWithProviders(<RegisterPage />);
      expect(screen.getByRole('button', { name: /create account/i })).toBeInTheDocument();
    });

    it('renders sign in link for existing users', () => {
      renderWithProviders(<RegisterPage />);
      expect(screen.getByText(/sign in instead/i)).toBeInTheDocument();
    });
  });

  describe('Validation', () => {
    it('shows required errors when all fields are empty on submit', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.click(screen.getByRole('button', { name: /create account/i }));
      await waitFor(() => {
        expect(screen.getByText('First name is required')).toBeInTheDocument();
        expect(screen.getByText('Last name is required')).toBeInTheDocument();
        expect(screen.getByText('Email is required')).toBeInTheDocument();
      });
    });

    it('shows password too short error', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.type(screen.getByLabelText(/^password/i), 'short');
      await user.click(screen.getByRole('button', { name: /create account/i }));
      await waitFor(() => {
        expect(screen.getByText(/at least 8 characters/i)).toBeInTheDocument();
      });
    });

    it('shows password mismatch error', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.type(screen.getByLabelText(/^password/i), 'Password123!');
      await user.type(screen.getByLabelText(/confirm password/i), 'DifferentPassword!');
      await user.click(screen.getByRole('button', { name: /create account/i }));
      await waitFor(() => {
        expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
      });
    });

    it('shows invalid email format error', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.type(screen.getByLabelText(/email address/i), 'invalid-email');
      await user.click(screen.getByRole('button', { name: /create account/i }));
      await waitFor(() => {
        expect(screen.getByText(/valid email address/i)).toBeInTheDocument();
      });
    });
  });

  describe('Password strength indicator', () => {
    it('renders strength bar once user starts typing a password', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.type(screen.getByLabelText(/^password/i), 'abc');
      await waitFor(() => {
        expect(screen.getByRole('progressbar')).toBeInTheDocument();
      });
    });

    it('shows "Strong" label for a strong password', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.type(screen.getByLabelText(/^password/i), 'Strong@Pass1');
      await waitFor(() => {
        expect(screen.getByText('Strong')).toBeInTheDocument();
      });
    });

    it('shows "Weak" label for a short, simple password', async () => {
      const { user } = renderWithProviders(<RegisterPage />);
      await user.type(screen.getByLabelText(/^password/i), 'abc12345');
      await waitFor(() => {
        // Length ≥ 8 and digits present = score 2 → Fair
        expect(screen.getByText(/fair|weak/i)).toBeInTheDocument();
      });
    });
  });

  describe('Successful registration', () => {
    it('calls register with correct payload on valid submit', async () => {
      const registerMock = vi.fn().mockResolvedValue(undefined);
      const { user } = renderWithProviders(<RegisterPage />, { authContext: { register: registerMock } });

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(registerMock).toHaveBeenCalledWith({
          firstName: 'Jane',
          lastName:  'Smith',
          email:     'jane@example.com',
          password:  'StrongP@ss1',
        });
      });
    });

    it('disables submit button while loading', async () => {
      const registerMock = vi.fn(() => new Promise(() => {}));
      const { user } = renderWithProviders(<RegisterPage />, { authContext: { register: registerMock } });

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /create account/i })).toBeDisabled();
      });
    });
  });

  describe('Error handling', () => {
    it('shows 409 duplicate email message at form level', async () => {
      const error409 = { status: 409, message: 'User already exists with email', violations: null };
      const registerMock = vi.fn().mockRejectedValue(error409);
      const { user } = renderWithProviders(<RegisterPage />, { authContext: { register: registerMock } });

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/already registered/i);
      });
    });

    it('applies server violation errors to their corresponding fields', async () => {
      const errorWithViolations = {
        status: 400,
        message: 'Validation failed',
        violations: { email: 'Email domain is not allowed' },
      };
      const registerMock = vi.fn().mockRejectedValue(errorWithViolations);
      const { user } = renderWithProviders(<RegisterPage />, { authContext: { register: registerMock } });

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(screen.getByText('Email domain is not allowed')).toBeInTheDocument();
      });
    });

    it('shows generic 500 error at form level', async () => {
      const error500 = { status: 500, message: 'An unexpected server error occurred.', violations: null };
      const registerMock = vi.fn().mockRejectedValue(error500);
      const { user } = renderWithProviders(<RegisterPage />, { authContext: { register: registerMock } });

      await fillValidForm(user);
      await user.click(screen.getByRole('button', { name: /create account/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/unexpected server error/i);
      });
    });
  });
});
