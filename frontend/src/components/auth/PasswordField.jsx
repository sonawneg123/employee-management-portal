/**
 * @fileoverview PasswordField — controlled password input with visibility toggle.
 *
 * Wraps MUI {@link TextField} and adds a show/hide password icon button.
 * Integrates with React Hook Form via the {@code control} prop and
 * {@link Controller}.
 *
 * Accessibility:
 * - The toggle button has a descriptive {@code aria-label}.
 * - The input uses {@code autoComplete} hints so that password managers work.
 */

import React, { useState } from 'react';
import { Controller } from 'react-hook-form';
import {
  FormControl,
  FormHelperText,
  IconButton,
  InputAdornment,
  InputLabel,
  OutlinedInput,
} from '@mui/material';
import VisibilityIcon     from '@mui/icons-material/Visibility';
import VisibilityOffIcon  from '@mui/icons-material/VisibilityOff';

/**
 * @typedef {Object} PasswordFieldProps
 * @property {import('react-hook-form').Control<any>} control     - RHF control object.
 * @property {string}           name                              - RHF field name.
 * @property {string}           label                             - Input label text.
 * @property {string}           [id]                              - HTML id (defaults to name).
 * @property {string}           [autoComplete]                    - autocomplete attribute value.
 * @property {boolean}          [disabled=false]                  - Whether the field is disabled.
 * @property {boolean}          [required=false]                  - Whether the field is required.
 */

/**
 * Password input field with show/hide toggle, integrated with React Hook Form.
 *
 * @param {PasswordFieldProps} props
 * @returns {JSX.Element}
 */
export default function PasswordField({
  control,
  name,
  label,
  id,
  autoComplete = 'current-password',
  disabled = false,
  required = false,
}) {
  const fieldId = id ?? name;
  const [showPassword, setShowPassword] = useState(false);

  const handleToggle = () => setShowPassword((prev) => !prev);
  const handleMouseDown = (/** @type {React.MouseEvent} */ e) => e.preventDefault();

  return (
    <Controller
      name={name}
      control={control}
      render={({ field, fieldState: { error } }) => (
        <FormControl
          fullWidth
          variant="outlined"
          size="small"
          error={Boolean(error)}
          disabled={disabled}
          required={required}
        >
          <InputLabel htmlFor={fieldId} shrink>
            {label}
          </InputLabel>
          <OutlinedInput
            {...field}
            id={fieldId}
            type={showPassword ? 'text' : 'password'}
            autoComplete={autoComplete}
            label={label}
            notched
            endAdornment={
              <InputAdornment position="end">
                <IconButton
                  aria-label={showPassword ? 'Hide password' : 'Show password'}
                  onClick={handleToggle}
                  onMouseDown={handleMouseDown}
                  edge="end"
                  size="small"
                  tabIndex={-1}
                >
                  {showPassword ? (
                    <VisibilityOffIcon fontSize="small" />
                  ) : (
                    <VisibilityIcon fontSize="small" />
                  )}
                </IconButton>
              </InputAdornment>
            }
            aria-describedby={error ? `${fieldId}-error` : undefined}
            inputProps={{ 'aria-required': required }}
          />
          {error && (
            <FormHelperText id={`${fieldId}-error`} role="alert">
              {error.message}
            </FormHelperText>
          )}
        </FormControl>
      )}
    />
  );
}
