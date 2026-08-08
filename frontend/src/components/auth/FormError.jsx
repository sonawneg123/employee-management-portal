/**
 * @fileoverview FormError — displays an error Alert for form-level and server errors.
 *
 * Renders an MUI {@link Alert} with the error message when {@code message} is
 * non-empty. Used to surface API errors (401, 409, 422, 500) at the form level,
 * distinct from per-field validation messages.
 */

import React from 'react';
import { Alert, AlertTitle, Collapse } from '@mui/material';

/**
 * Form-level error alert.
 *
 * @param {{
 *   message?: string | null,
 *   title?:   string,
 *   severity?: 'error' | 'warning' | 'info' | 'success',
 * }} props
 * @returns {JSX.Element | null}
 */
export default function FormError({ message, title, severity = 'error' }) {
  return (
    <Collapse in={Boolean(message)} unmountOnExit>
      <Alert
        severity={severity}
        sx={{ mb: 2.5, borderRadius: 2 }}
        role="alert"
        aria-live="polite"
      >
        {title && <AlertTitle>{title}</AlertTitle>}
        {message}
      </Alert>
    </Collapse>
  );
}
