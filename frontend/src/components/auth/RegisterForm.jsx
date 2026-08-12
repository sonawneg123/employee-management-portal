/**
 * @fileoverview RegisterForm — React Hook Form registration form component.
 *
 * Responsibilities:
 * - Renders first name, last name, email, password, and confirm-password fields.
 * - Validates with Zod via {@link registerSchema}.
 * - Shows real-time password strength indicator.
 * - Calls {@link useRegister} on submit.
 * - Surfaces server-side errors (409 duplicate email, 400 validation) via
 *   {@link FormError} and per-field errors.
 * - Fully keyboard-navigable with ARIA labelling.
 */

import React, { useEffect } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Link as RouterLink } from 'react-router-dom';
import {
  Alert,
  Box,
  Divider,
  Grid,
  LinearProgress,
  Link,
  TextField,
  Tooltip,
  Typography,
} from '@mui/material';
import PersonAddIcon from '@mui/icons-material/PersonAdd';
import InfoOutlinedIcon from '@mui/icons-material/InfoOutlined';

import { registerSchema } from '@/utils/validationSchemas';
import { useRegister } from '@/hooks/useRegister';
import { ROUTES } from '@/constants/routes';
import PasswordField from '@/components/auth/PasswordField';
import FormError     from '@/components/auth/FormError';
import LoadingButton from '@/components/auth/LoadingButton';

/**
 * @typedef {Object} RegisterFormValues
 * @property {string} firstName
 * @property {string} lastName
 * @property {string} email
 * @property {string} password
 * @property {string} confirmPassword
 */

/**
 * Calculates a password strength score between 0 and 4.
 *
 * Criteria (each adds 1 point):
 * 1. Length ≥ 8 characters.
 * 2. Contains uppercase letters.
 * 3. Contains a digit.
 * 4. Contains a special character.
 *
 * @param {string} password
 * @returns {{ score: number, label: string, color: 'error'|'warning'|'info'|'success' }}
 */
function getPasswordStrength(password) {
  if (!password) return { score: 0, label: '', color: 'error' };
  let score = 0;
  if (password.length >= 8)                    score += 1;
  if (/[A-Z]/.test(password))                 score += 1;
  if (/\d/.test(password))                     score += 1;
  if (/[^A-Za-z0-9]/.test(password))          score += 1;

  const map = {
    0: { label: '',           color: 'error' },
    1: { label: 'Weak',       color: 'error' },
    2: { label: 'Fair',       color: 'warning' },
    3: { label: 'Good',       color: 'info' },
    4: { label: 'Strong',     color: 'success' },
  };
  return { score, ...map[score] };
}

/**
 * Fully controlled registration form.
 *
 * @returns {JSX.Element}
 */
export default function RegisterForm() {
  const { mutate: register, isPending, isError, error, reset: resetMutation } = useRegister();

  const {
    control,
    register: registerField,
    handleSubmit,
    setError,
    watch,
    formState: { errors, isSubmitting },
  } = useForm({
    resolver: zodResolver(registerSchema),
    defaultValues: {
      firstName:       '',
      lastName:        '',
      email:           '',
      password:        '',
      confirmPassword: '',
    },
    mode: 'onTouched',
  });

  const isLoading = isPending || isSubmitting;
  const passwordValue = watch('password', '');
  const strength = getPasswordStrength(passwordValue);

  // ── Apply server-side field violations ──────────────────────────────────
  useEffect(() => {
    if (isError && error?.violations) {
      Object.entries(error.violations).forEach(([field, message]) => {
        setError(field, { type: 'server', message: String(message) });
      });
    }
  }, [isError, error, setError]);

  /**
   * Returns the form-level error message.
   *
   * @returns {string | null}
   */
  const getFormErrorMessage = () => {
    if (!isError || !error) return null;
    if (error.violations) return null;
    if (error.status === 409) return 'This email address is already registered. Please sign in or use a different email.';
    return error.message ?? 'Registration failed. Please try again.';
  };

  /**
   * Handles form submission.
   *
   * @param {RegisterFormValues} values
   */
  const onSubmit = async (values) => {
    resetMutation();
    try {
      await register({
        firstName: values.firstName,
        lastName:  values.lastName,
        email:     values.email,
        password:  values.password,
      });
    } catch {
      // Errors are surfaced through mutation state
    }
  };

  return (
    <Box
      component="form"
      onSubmit={handleSubmit(onSubmit)}
      noValidate
      aria-label="Registration form"
    >
      {/* Role info note — registration always creates a ROLE_EMPLOYEE account.
          HR/Admin accounts must be promoted by an administrator via the Admin panel.
          role="status" is used (not "alert") so it does not interfere with form-level error alerts. */}
      <Alert
        severity="info"
        role="status"
        icon={<InfoOutlinedIcon fontSize="small" />}
        sx={{ mb: 2.5, borderRadius: 2, fontSize: '0.8rem' }}
      >
        Registration creates an <strong>Employee</strong> account. To create HR or Admin accounts,
        register here then ask your system administrator to promote the account via the Admin panel.
      </Alert>

      {/* Form-level error */}
      <FormError message={getFormErrorMessage()} />

      {/* Name row */}
      <Grid container spacing={2} sx={{ mb: 2.5 }}>
        <Grid size={6}>
          <Controller
            name="firstName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                id="register-firstname"
                label="First name"
                autoComplete="given-name"
                autoFocus
                required
                fullWidth
                size="small"
                error={Boolean(errors.firstName)}
                helperText={errors.firstName?.message}
                inputProps={{ 'aria-required': true }}
              />
            )}
          />
        </Grid>
        <Grid size={6}>
          <Controller
            name="lastName"
            control={control}
            render={({ field }) => (
              <TextField
                {...field}
                id="register-lastname"
                label="Last name"
                autoComplete="family-name"
                required
                fullWidth
                size="small"
                error={Boolean(errors.lastName)}
                helperText={errors.lastName?.message}
                inputProps={{ 'aria-required': true }}
              />
            )}
          />
        </Grid>
      </Grid>

      {/* Email field */}
      <Controller
        name="email"
        control={control}
        render={({ field }) => (
          <TextField
            {...field}
            id="register-email"
            label="Email address"
            type="email"
            autoComplete="email"
            required
            fullWidth
            size="small"
            error={Boolean(errors.email)}
            helperText={errors.email?.message}
            sx={{ mb: 2.5 }}
            inputProps={{ 'aria-required': true }}
          />
        )}
      />

      {/* Password field with strength meter */}
      <Box sx={{ mb: 1 }}>
        <PasswordField
          control={control}
          name="password"
          label="Password"
          id="register-password"
          autoComplete="new-password"
          required
        />
      </Box>

      {/* Password strength indicator */}
      {passwordValue.length > 0 && (
        <Box sx={{ mb: 2.5 }}>
          <Tooltip
            title="Strong passwords have uppercase letters, numbers, and special characters."
            placement="right"
            arrow
          >
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1.5 }}>
              <LinearProgress
                variant="determinate"
                value={(strength.score / 4) * 100}
                color={strength.color}
                sx={{ flexGrow: 1, height: 6, borderRadius: 3 }}
                aria-label={`Password strength: ${strength.label}`}
              />
              <Typography
                variant="caption"
                color={`${strength.color}.main`}
                sx={{ whiteSpace: 'nowrap', minWidth: 44 }}
              >
                {strength.label}
              </Typography>
            </Box>
          </Tooltip>
        </Box>
      )}

      {/* Confirm password field */}
      <Box sx={{ mb: 3 }}>
        <PasswordField
          control={control}
          name="confirmPassword"
          label="Confirm password"
          id="register-confirm-password"
          autoComplete="new-password"
          required
        />
      </Box>

      {/* Terms note */}
      <Typography variant="caption" color="text.secondary" sx={{ display: 'block', mb: 3 }}>
        By creating an account, you agree to our{' '}
        <Link href="#" underline="hover">Terms of Service</Link> and{' '}
        <Link href="#" underline="hover">Privacy Policy</Link>.
      </Typography>

      {/* Submit button */}
      <LoadingButton loading={isLoading} startIcon={<PersonAddIcon />} aria-label="Create account">
        Create Account
      </LoadingButton>

      {/* Divider + login link */}
      <Divider sx={{ my: 3 }}>
        <Typography variant="caption" color="text.secondary">
          Already have an account?
        </Typography>
      </Divider>

      <Box sx={{ textAlign: 'center' }}>
        <Link
          component={RouterLink}
          to={ROUTES.LOGIN}
          variant="body2"
          fontWeight={500}
          underline="hover"
          aria-label="Sign in to your existing account"
        >
          Sign in instead
        </Link>
      </Box>
    </Box>
  );
}
