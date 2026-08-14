/**
 * @fileoverview ForgotPasswordPage — three-step password-reset flow.
 *
 * Step 1 — Enter email → POST /auth/forgot-password
 * Step 2 — Enter OTP   → POST /auth/verify-otp
 * Step 3 — New password → POST /auth/reset-password
 * Final  — Success screen with link back to login
 *
 * UX features:
 * - Zod + React Hook Form validation for each step
 * - Loading state disables the submit button during API calls
 * - Server errors are displayed inline
 * - Resend OTP cooldown (60 s) with countdown timer
 * - Password visibility toggle on step 3
 * - Accessible form controls and error messages
 */

import React, { useState, useEffect, useCallback } from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Helmet } from 'react-helmet-async';
import { Box, Link, TextField, Typography } from '@mui/material';
import ArrowBackIcon from '@mui/icons-material/ArrowBack';
import LockResetIcon from '@mui/icons-material/LockReset';
import CheckCircleOutlineIcon from '@mui/icons-material/CheckCircleOutline';

import {
  forgotPasswordSchema,
  verifyOtpSchema,
  resetPasswordSchema,
} from '@/utils/validationSchemas';
import { forgotPassword, verifyOtp, resetPassword } from '@/services/authApi';
import { ROUTES } from '@/constants/routes';
import AuthLayout from '@/components/auth/AuthLayout';
import AuthCard from '@/components/auth/AuthCard';
import PasswordField from '@/components/auth/PasswordField';
import FormError from '@/components/auth/FormError';
import LoadingButton from '@/components/auth/LoadingButton';

/** Duration of the resend-OTP cooldown in seconds. */
const RESEND_COOLDOWN_SECONDS = 60;

/**
 * Three-step forgot-password page.
 *
 * @returns {JSX.Element}
 */
export default function ForgotPasswordPage() {
  const [step, setStep] = useState(1); // 1=email, 2=otp, 3=new-password, 4=success
  const [email, setEmail] = useState('');
  const [serverError, setServerError] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [resendCooldown, setResendCooldown] = useState(0);

  // ── Resend countdown timer ─────────────────────────────────────────────────
  useEffect(() => {
    if (resendCooldown <= 0) return;
    const timer = setTimeout(() => setResendCooldown((prev) => prev - 1), 1000);
    return () => clearTimeout(timer);
  }, [resendCooldown]);

  // ── Step 1 form — email ────────────────────────────────────────────────────
  const step1Form = useForm({
    resolver: zodResolver(forgotPasswordSchema),
    defaultValues: { email: '' },
  });

  // ── Step 2 form — OTP ─────────────────────────────────────────────────────
  const step2Form = useForm({
    resolver: zodResolver(verifyOtpSchema),
    defaultValues: { otp: '' },
  });

  // ── Step 3 form — new password ────────────────────────────────────────────
  const step3Form = useForm({
    resolver: zodResolver(resetPasswordSchema),
    defaultValues: { newPassword: '', confirmPassword: '' },
  });

  // ── Step 1 submit ──────────────────────────────────────────────────────────
  const onSubmitEmail = async (values) => {
    setServerError('');
    setIsLoading(true);
    try {
      await forgotPassword({ email: values.email });
      setEmail(values.email);
      setResendCooldown(RESEND_COOLDOWN_SECONDS);
      setStep(2);
    } catch (err) {
      // 404 → email not registered; show a clear, specific message.
      if (err?.status === 404) {
        setServerError('This email is not registered with us.');
      } else {
        setServerError(err?.message ?? 'Something went wrong. Please try again.');
      }
    } finally {
      setIsLoading(false);
    }
  };

  // ── Resend OTP ─────────────────────────────────────────────────────────────
  const handleResend = useCallback(async () => {
    if (resendCooldown > 0) return;
    setServerError('');
    setIsLoading(true);
    try {
      await forgotPassword({ email });
      setResendCooldown(RESEND_COOLDOWN_SECONDS);
      step2Form.reset();
    } catch (err) {
      setServerError(err?.message ?? 'Failed to resend OTP. Please try again.');
    } finally {
      setIsLoading(false);
    }
  }, [email, resendCooldown, step2Form]);

  // ── Step 2 submit ──────────────────────────────────────────────────────────
  const onSubmitOtp = async (values) => {
    setServerError('');
    setIsLoading(true);
    try {
      await verifyOtp({ email, otp: values.otp });
      setStep(3);
    } catch (err) {
      setServerError(err?.message ?? 'OTP verification failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // ── Step 3 submit ──────────────────────────────────────────────────────────
  const onSubmitReset = async (values) => {
    setServerError('');
    setIsLoading(true);
    try {
      await resetPassword({
        email,
        newPassword: values.newPassword,
        confirmPassword: values.confirmPassword,
      });
      setStep(4);
    } catch (err) {
      setServerError(err?.message ?? 'Password reset failed. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  // ── Renders ────────────────────────────────────────────────────────────────

  const renderStep1 = () => (
    <Box
      component="form"
      onSubmit={step1Form.handleSubmit(onSubmitEmail)}
      noValidate
      aria-label="Forgot password form"
    >
      <FormError message={serverError} />

      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Enter the email address associated with your account and we&apos;ll send you a one-time
        password (OTP) to reset it.
      </Typography>

      <Controller
        name="email"
        control={step1Form.control}
        render={({ field }) => (
          <TextField
            {...field}
            id="fp-email"
            label="Email address"
            type="email"
            autoComplete="email"
            autoFocus
            required
            fullWidth
            size="small"
            error={Boolean(step1Form.formState.errors.email)}
            helperText={step1Form.formState.errors.email?.message}
            sx={{ mb: 3 }}
            inputProps={{ 'aria-required': true }}
          />
        )}
      />

      <LoadingButton
        loading={isLoading}
        startIcon={<LockResetIcon />}
        aria-label="Send OTP to email"
      >
        Send OTP
      </LoadingButton>

      <Box sx={{ mt: 2.5, textAlign: 'center' }}>
        <Link
          component={RouterLink}
          to={ROUTES.LOGIN}
          variant="body2"
          underline="hover"
          aria-label="Back to login"
          sx={{ display: 'inline-flex', alignItems: 'center', gap: 0.5 }}
        >
          <ArrowBackIcon fontSize="inherit" />
          Back to Login
        </Link>
      </Box>
    </Box>
  );

  const renderStep2 = () => (
    <Box
      component="form"
      onSubmit={step2Form.handleSubmit(onSubmitOtp)}
      noValidate
      aria-label="OTP verification form"
    >
      <FormError message={serverError} />

      <Typography variant="body2" color="text.secondary" sx={{ mb: 1 }}>
        We sent a 6-digit OTP to <strong>{email}</strong>. Enter it below. The code expires in 10
        minutes.
      </Typography>

      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Check your spam folder if you don&apos;t see it.
      </Typography>

      <Controller
        name="otp"
        control={step2Form.control}
        render={({ field }) => (
          <TextField
            {...field}
            id="fp-otp"
            label="6-digit OTP"
            type="text"
            inputMode="numeric"
            autoComplete="one-time-code"
            autoFocus
            required
            fullWidth
            size="small"
            error={Boolean(step2Form.formState.errors.otp)}
            helperText={step2Form.formState.errors.otp?.message}
            sx={{ mb: 3 }}
            inputProps={{
              maxLength: 6,
              'aria-required': true,
              'aria-label': 'One-time password',
            }}
          />
        )}
      />

      <LoadingButton loading={isLoading} aria-label="Verify OTP">
        Verify OTP
      </LoadingButton>

      {/* Resend OTP row */}
      <Box sx={{ mt: 2, textAlign: 'center' }}>
        <Typography variant="body2" color="text.secondary" component="span">
          Didn&apos;t receive it?{' '}
        </Typography>
        {resendCooldown > 0 ? (
          <Typography
            variant="body2"
            color="text.disabled"
            component="span"
            aria-label={`Resend available in ${resendCooldown} seconds`}
          >
            Resend OTP in {resendCooldown}s
          </Typography>
        ) : (
          <Link
            component="button"
            type="button"
            variant="body2"
            underline="hover"
            onClick={handleResend}
            disabled={isLoading}
            aria-label="Resend OTP"
          >
            Resend OTP
          </Link>
        )}
      </Box>

      <Box sx={{ mt: 1.5, textAlign: 'center' }}>
        <Link
          component="button"
          type="button"
          variant="body2"
          underline="hover"
          onClick={() => {
            setStep(1);
            setServerError('');
          }}
          aria-label="Change email address"
        >
          Use a different email
        </Link>
      </Box>
    </Box>
  );

  const renderStep3 = () => (
    <Box
      component="form"
      onSubmit={step3Form.handleSubmit(onSubmitReset)}
      noValidate
      aria-label="Reset password form"
    >
      <FormError message={serverError} />

      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Choose a strong password for <strong>{email}</strong>. It must be at least 8 characters.
      </Typography>

      <Box sx={{ mb: 2.5 }}>
        <PasswordField
          control={step3Form.control}
          name="newPassword"
          label="New password"
          id="fp-new-password"
          autoComplete="new-password"
          required
        />
      </Box>

      <Box sx={{ mb: 3 }}>
        <PasswordField
          control={step3Form.control}
          name="confirmPassword"
          label="Confirm new password"
          id="fp-confirm-password"
          autoComplete="new-password"
          required
        />
      </Box>

      <LoadingButton loading={isLoading} startIcon={<LockResetIcon />} aria-label="Reset password">
        Reset Password
      </LoadingButton>
    </Box>
  );

  const renderSuccess = () => (
    <Box sx={{ textAlign: 'center' }}>
      <CheckCircleOutlineIcon color="success" sx={{ fontSize: 64, mb: 2 }} aria-hidden="true" />
      <Typography variant="h6" fontWeight={700} sx={{ mb: 1 }}>
        Password Reset Successfully
      </Typography>
      <Typography variant="body2" color="text.secondary" sx={{ mb: 3 }}>
        Your password has been updated. You can now sign in with your new password.
      </Typography>
      <Link
        component={RouterLink}
        to={ROUTES.LOGIN}
        variant="body1"
        fontWeight={600}
        underline="hover"
        aria-label="Go to login page"
      >
        Back to Login
      </Link>
    </Box>
  );

  const stepConfig = {
    1: { title: 'Forgot Password', description: 'Enter your email to receive a reset OTP.' },
    2: { title: 'Enter OTP', description: `A code was sent to ${email}.` },
    3: { title: 'Set New Password', description: 'Choose a new password for your account.' },
    4: { title: 'Password Reset', description: '' },
  };

  const { title, description } = stepConfig[step] ?? stepConfig[1];

  return (
    <>
      <Helmet>
        <title>Forgot Password — PeopleCore HR</title>
        <meta name="description" content="Reset your PeopleCore HR account password." />
      </Helmet>

      <AuthLayout
        title="Reset your password"
        subtitle="Follow the steps to securely recover your account access."
      >
        <AuthCard title={title} description={description}>
          {step === 1 && renderStep1()}
          {step === 2 && renderStep2()}
          {step === 3 && renderStep3()}
          {step === 4 && renderSuccess()}
        </AuthCard>
      </AuthLayout>
    </>
  );
}
