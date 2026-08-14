/**
 * @fileoverview Tests for ForgotPasswordPage — the three-step password-reset flow.
 *
 * Scenarios covered:
 * 1.  Forgot Password navigation — link renders and page loads.
 * 2.  Email submission — valid email calls forgotPassword API.
 * 3.  OTP screen — appears after successful email submission.
 * 4.  Incorrect OTP — server error displayed.
 * 5.  Expired OTP error — displayed when server returns expiry message.
 * 6.  Password mismatch — inline validation error.
 * 7.  Successful password reset — success screen shown.
 * 8.  Loading states — button disabled while in flight.
 * 9.  Resend cooldown — countdown shown, button hidden during cooldown.
 * 10. Invalid email format — validation error.
 * 11. OTP not exactly 6 digits — validation error.
 * 12. Server error during email step — displayed inline.
 */

import React from 'react';
import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import { render, screen, waitFor, act } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { ThemeProvider, createTheme } from '@mui/material';
import { HelmetProvider } from 'react-helmet-async';

import ForgotPasswordPage from '@/pages/auth/ForgotPasswordPage';
import * as authApi from '@/services/authApi';

// ── Module mock ───────────────────────────────────────────────────────────────
vi.mock('@/services/authApi', () => ({
  forgotPassword: vi.fn(),
  verifyOtp: vi.fn(),
  resetPassword: vi.fn(),
  login: vi.fn(),
  register: vi.fn(),
}));

// ── Test utilities ────────────────────────────────────────────────────────────

const testTheme = createTheme();

function makeQueryClient() {
  return new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
}

function renderPage(initialPath = '/forgot-password') {
  const qc = makeQueryClient();
  const user = userEvent.setup();

  const result = render(
    <HelmetProvider>
      <QueryClientProvider client={qc}>
        <ThemeProvider theme={testTheme}>
          <MemoryRouter initialEntries={[initialPath]}>
            <ForgotPasswordPage />
          </MemoryRouter>
        </ThemeProvider>
      </QueryClientProvider>
    </HelmetProvider>,
  );

  return { ...result, user };
}

// ── Shared helpers ────────────────────────────────────────────────────────────

/** Advances the flow past Step 1 (email). */
async function fillAndSubmitEmail(user, email = 'alice@example.com') {
  authApi.forgotPassword.mockResolvedValueOnce({
    message: 'If an account exists for this email, an OTP has been sent.',
  });

  await user.type(screen.getByLabelText(/email address/i), email);
  await user.click(screen.getByRole('button', { name: /send otp/i }));

  await waitFor(() => {
    expect(screen.getByLabelText(/6-digit otp/i)).toBeInTheDocument();
  });
}

/** Advances the flow past Step 2 (OTP). */
async function fillAndVerifyOtp(user, otp = '482713') {
  authApi.verifyOtp.mockResolvedValueOnce({
    message: 'OTP verified successfully. You may now reset your password.',
  });

  await user.type(screen.getByLabelText(/6-digit otp/i), otp);
  await user.click(screen.getByRole('button', { name: /verify otp/i }));

  await waitFor(() => {
    expect(screen.getByLabelText('New password *')).toBeInTheDocument();
  });
}

// ── Tests ─────────────────────────────────────────────────────────────────────

describe('ForgotPasswordPage', () => {
  afterEach(() => {
    vi.clearAllMocks();
  });

  // ── 1. Navigation / rendering ──────────────────────────────────────────────
  describe('Rendering', () => {
    it('1. renders the Forgot Password heading', () => {
      renderPage();
      expect(screen.getByText('Forgot Password')).toBeInTheDocument();
    });

    it('renders the email input', () => {
      renderPage();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    });

    it('renders the Send OTP button', () => {
      renderPage();
      expect(screen.getByRole('button', { name: /send otp/i })).toBeInTheDocument();
    });

    it('renders the Back to Login link', () => {
      renderPage();
      expect(screen.getByRole('link', { name: /back to login/i })).toBeInTheDocument();
    });
  });

  // ── 2. Email submission ────────────────────────────────────────────────────
  describe('Email step', () => {
    it('2. calls forgotPassword with correct email on submit', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user, 'test@example.com');

      expect(authApi.forgotPassword).toHaveBeenCalledWith({ email: 'test@example.com' });
    });

    it('10. shows validation error for invalid email format', async () => {
      const { user } = renderPage();
      await user.type(screen.getByLabelText(/email address/i), 'not-an-email');
      await user.click(screen.getByRole('button', { name: /send otp/i }));

      await waitFor(() => {
        expect(screen.getByText(/valid email address/i)).toBeInTheDocument();
      });
    });

    it('shows required error when email is blank', async () => {
      const { user } = renderPage();
      await user.click(screen.getByRole('button', { name: /send otp/i }));

      await waitFor(() => {
        expect(screen.getByText('Email is required')).toBeInTheDocument();
      });
    });

    it('12. shows server error inline if forgotPassword call fails', async () => {
      authApi.forgotPassword.mockRejectedValueOnce({ message: 'Network error.' });

      const { user } = renderPage();
      await user.type(screen.getByLabelText(/email address/i), 'alice@example.com');
      await user.click(screen.getByRole('button', { name: /send otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/network error/i);
      });
    });

    it('shows "This email is not registered with us." for a 404 response', async () => {
      authApi.forgotPassword.mockRejectedValueOnce({
        status: 404,
        message: 'This email is not registered with us.',
      });

      const { user } = renderPage();
      await user.type(screen.getByLabelText(/email address/i), 'nobody@example.com');
      await user.click(screen.getByRole('button', { name: /send otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(
          /this email is not registered with us/i,
        );
      });
    });

    it('stays on step 1 when email is not registered (no OTP screen)', async () => {
      authApi.forgotPassword.mockRejectedValueOnce({
        status: 404,
        message: 'This email is not registered with us.',
      });

      const { user } = renderPage();
      await user.type(screen.getByLabelText(/email address/i), 'nobody@example.com');
      await user.click(screen.getByRole('button', { name: /send otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toBeInTheDocument();
      });

      // Must NOT have advanced to the OTP screen
      expect(screen.queryByLabelText(/6-digit otp/i)).not.toBeInTheDocument();
      expect(screen.getByLabelText(/email address/i)).toBeInTheDocument();
    });

    it('8. Send OTP button is disabled during loading', async () => {
      authApi.forgotPassword.mockImplementation(() => new Promise(() => {})); // never resolves

      const { user } = renderPage();
      await user.type(screen.getByLabelText(/email address/i), 'alice@example.com');
      await user.click(screen.getByRole('button', { name: /send otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /send otp/i })).toBeDisabled();
      });
    });
  });

  // ── 3. OTP screen ──────────────────────────────────────────────────────────
  describe('OTP step', () => {
    it('3. OTP input screen renders after successful email submission', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      expect(screen.getByLabelText(/6-digit otp/i)).toBeInTheDocument();
      expect(screen.getByRole('button', { name: /verify otp/i })).toBeInTheDocument();
    });

    it('11. shows validation error when OTP is not 6 digits', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      await user.type(screen.getByLabelText(/6-digit otp/i), '123');
      await user.click(screen.getByRole('button', { name: /verify otp/i }));

      await waitFor(() => {
        expect(screen.getByText(/exactly 6 digits/i)).toBeInTheDocument();
      });
    });

    it('4. displays server error for incorrect OTP', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      authApi.verifyOtp.mockRejectedValueOnce({
        message: 'Incorrect OTP. 4 attempt(s) remaining.',
      });

      await user.type(screen.getByLabelText(/6-digit otp/i), '000000');
      await user.click(screen.getByRole('button', { name: /verify otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/incorrect otp/i);
      });
    });

    it('5. displays error message for expired OTP', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      authApi.verifyOtp.mockRejectedValueOnce({
        message: 'OTP is invalid or has expired. Please request a new one.',
      });

      await user.type(screen.getByLabelText(/6-digit otp/i), '123456');
      await user.click(screen.getByRole('button', { name: /verify otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('alert')).toHaveTextContent(/expired/i);
      });
    });

    it('8. Verify OTP button is disabled during loading', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      authApi.verifyOtp.mockImplementation(() => new Promise(() => {}));

      await user.type(screen.getByLabelText(/6-digit otp/i), '482713');
      await user.click(screen.getByRole('button', { name: /verify otp/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /verify otp/i })).toBeDisabled();
      });
    });

    it('9. resend cooldown is shown after email is sent', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      // Countdown should be visible immediately after sending
      expect(screen.getByText(/resend otp in \d+s/i)).toBeInTheDocument();
    });

    it('9. Resend OTP button is hidden during cooldown', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);

      // "Resend OTP" as a clickable link should NOT appear during cooldown
      expect(screen.queryByRole('button', { name: /^resend otp$/i })).not.toBeInTheDocument();
    });
  });

  // ── 6. Password mismatch ───────────────────────────────────────────────────
  describe('New password step', () => {
    it('6. shows password mismatch error when confirmPassword differs', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);
      await fillAndVerifyOtp(user);

      await user.type(screen.getByLabelText('New password *'), 'NewPass1!');
      await user.type(screen.getByLabelText('Confirm new password *'), 'Different1!');
      await user.click(screen.getByRole('button', { name: /reset password/i }));

      await waitFor(() => {
        expect(screen.getByText(/passwords do not match/i)).toBeInTheDocument();
      });
    });

    it('8. Reset Password button disabled during loading', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);
      await fillAndVerifyOtp(user);

      authApi.resetPassword.mockImplementation(() => new Promise(() => {}));

      await user.type(screen.getByLabelText('New password *'), 'NewPass1!');
      await user.type(screen.getByLabelText('Confirm new password *'), 'NewPass1!');
      await user.click(screen.getByRole('button', { name: /reset password/i }));

      await waitFor(() => {
        expect(screen.getByRole('button', { name: /reset password/i })).toBeDisabled();
      });
    });
  });

  // ── 7. Successful password reset ───────────────────────────────────────────
  describe('Success screen', () => {
    it('7. shows success screen after password is reset', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);
      await fillAndVerifyOtp(user);

      authApi.resetPassword.mockResolvedValueOnce({
        message: 'Password has been reset successfully. Please log in with your new password.',
      });

      await user.type(screen.getByLabelText('New password *'), 'NewPass1!');
      await user.type(screen.getByLabelText('Confirm new password *'), 'NewPass1!');
      await user.click(screen.getByRole('button', { name: /reset password/i }));

      await waitFor(() => {
        expect(screen.getByText(/password reset successfully/i)).toBeInTheDocument();
      });
    });

    it('7. success screen shows Back to Login link', async () => {
      const { user } = renderPage();
      await fillAndSubmitEmail(user);
      await fillAndVerifyOtp(user);

      authApi.resetPassword.mockResolvedValueOnce({ message: 'Password has been reset.' });

      await user.type(screen.getByLabelText('New password *'), 'NewPass1!');
      await user.type(screen.getByLabelText('Confirm new password *'), 'NewPass1!');
      await user.click(screen.getByRole('button', { name: /reset password/i }));

      await waitFor(() => {
        expect(screen.getByRole('link', { name: /go to login/i })).toBeInTheDocument();
      });
    });
  });
});
