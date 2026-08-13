/**
 * @fileoverview LoginForm — React Hook Form login form component.
 *
 * Responsibilities:
 * - Renders email, password, remember-me, and forgot-password controls.
 * - Validates with Zod via {@link loginSchema}.
 * - Calls {@link useLogin} on submit.
 * - Surfaces server-side errors via {@link FormError}.
 * - Handles field-level violation messages from the backend (422/400).
 * - Provides full keyboard navigation and ARIA labelling.
 * - Restores remembered email from localStorage on mount.
 */

import React, { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link as RouterLink } from 'react-router-dom';
import {
  Box,
  Checkbox,
  Divider,
  FormControlLabel,
  Link,
  TextField,
  Typography,
} from '@mui/material';
import LoginIcon from '@mui/icons-material/Login';

import { loginSchema } from '@/utils/validationSchemas';
import { useLogin } from '@/hooks/useLogin';
import { getItem, setItem, removeItem } from '@/utils/localStorage';
import { ROUTES } from '@/constants/routes';
import PasswordField from '@/components/auth/PasswordField';
import FormError from '@/components/auth/FormError';
import LoadingButton from '@/components/auth/LoadingButton';

/** localStorage key for the "remember me" email value. */
const REMEMBER_EMAIL_KEY = 'emp_portal_remember_email';

/**
 * @typedef {Object} LoginFormValues
 * @property {string}  email
 * @property {string}  password
 * @property {boolean} rememberMe
 */

/**
 * Fully controlled login form.
 *
 * @returns {JSX.Element}
 */
export default function LoginForm() {
  const { mutate: login, isPending, isError, error, reset: resetMutation } = useLogin();

  const {
    control,
    register,
    handleSubmit,
    setError,
    setValue,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(loginSchema),
    defaultValues: {
      email: '',
      password: '',
      rememberMe: false,
    },
  });

  const isLoading = isPending || isSubmitting;

  // ── Restore remembered email on mount ───────────────────────────────────
  useEffect(() => {
    const remembered = getItem(REMEMBER_EMAIL_KEY);
    if (remembered) {
      setValue('email', remembered);
      setValue('rememberMe', true);
    }
  }, [setValue]);

  // ── Apply server-side field violations ──────────────────────────────────
  useEffect(() => {
    if (isError && error?.violations) {
      Object.entries(error.violations).forEach(([field, message]) => {
        setError(field, { type: 'server', message: String(message) });
      });
    }
  }, [isError, error, setError]);

  /**
   * Handles form submission.
   *
   * @param {LoginFormValues} values
   */
  const onSubmit = async (values) => {
    // Persist or clear remembered email
    if (values.rememberMe) {
      setItem(REMEMBER_EMAIL_KEY, values.email);
    } else {
      removeItem(REMEMBER_EMAIL_KEY);
    }

    resetMutation();

    try {
      await login({ email: values.email, password: values.password });
    } catch {
      // Errors are surfaced through the mutation's error state — no rethrow needed
    }
  };

  /**
   * Returns the form-level error message, preferring violations over generic message.
   *
   * @returns {string | null}
   */
  const getFormErrorMessage = () => {
    if (!isError || !error) return null;
    // Violation errors are shown per-field; only show form-level for non-violation errors
    if (error.violations) return null;
    return error.message ?? 'Login failed. Please try again.';
  };

  return (
    <Box component="form" onSubmit={handleSubmit(onSubmit)} noValidate aria-label="Login form">
      {/* Form-level error alert */}
      <FormError message={getFormErrorMessage()} />

      {/* Email field */}
      <Controller
        name="email"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            id="login-email"
            label="Email address"
            type="email"
            autoComplete="email"
            autoFocus
            required
            fullWidth
            size="small"
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
            sx={{ mb: 2.5 }}
            inputProps={{
              'aria-required': true,
              'aria-describedby': errors.email ? 'login-email-error' : undefined,
            }}
            FormHelperTextProps={{ id: 'login-email-error', role: 'alert' }}
          />
        )}
      />

      {/* Password field with visibility toggle */}
      <Box sx={{ mb: 1 }}>
        <PasswordField
          control={control}
          name="password"
          label="Password"
          id="login-password"
          autoComplete="current-password"
          required
        />
      </Box>

      {/* Remember me + Forgot password row */}
      <Box
        sx={{
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'space-between',
          mb: 3,
          flexWrap: 'wrap',
          gap: 1,
        }}
      >
        <FormControlLabel
          control={
            <Checkbox
              {...register('rememberMe')}
              size="small"
              inputProps={{ 'aria-label': 'Remember me' }}
            />
          }
          label={<Typography variant="body2">Remember me</Typography>}
        />
        <Link
          component={RouterLink}
          to="/forgot-password"
          variant="body2"
          underline="hover"
          aria-label="Forgot your password? Click to reset."
        >
          Forgot password?
        </Link>
      </Box>

      {/* Submit button */}
      <LoadingButton
        loading={isLoading}
        startIcon={<LoginIcon />}
        aria-label="Sign in to your account"
      >
        Sign In
      </LoadingButton>

      {/* Divider + register link */}
      <Divider sx={{ my: 3 }}>
        <Typography variant="caption" color="text.secondary">
          New to EMP Portal?
        </Typography>
      </Divider>

      <Box sx={{ textAlign: 'center' }}>
        <Link
          component={RouterLink}
          to={ROUTES.REGISTER}
          variant="body2"
          fontWeight={500}
          underline="hover"
          aria-label="Create a new account"
        >
          Create an account
        </Link>
      </Box>
    </Box>
  );
}
