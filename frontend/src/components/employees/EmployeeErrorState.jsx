/**
 * @fileoverview EmployeeErrorState — shown when the employee list fetch fails.
 */

import React from 'react';
import { Alert, Box, Button } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';

/**
 * @typedef {Object} EmployeeErrorStateProps
 * @property {any}        error     - The normalised error object from the hook.
 * @property {() => void} onRetry   - Callback to trigger a retry.
 */

/**
 * Error state component for the employee list.
 *
 * @param {EmployeeErrorStateProps} props
 * @returns {JSX.Element}
 */
export default function EmployeeErrorState({ error, onRetry }) {
  const message =
    error?.message ?? 'Failed to load employees. Please try again.';

  return (
    <Box sx={{ p: 3 }}>
      <Alert
        severity="error"
        action={
          <Button
            color="inherit"
            size="small"
            startIcon={<RefreshIcon />}
            onClick={onRetry}
            aria-label="Retry loading employees"
          >
            Retry
          </Button>
        }
      >
        {message}
      </Alert>
    </Box>
  );
}
