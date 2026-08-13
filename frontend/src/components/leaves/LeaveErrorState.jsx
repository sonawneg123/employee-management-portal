/**
 * @fileoverview LeaveErrorState — shown when the leave list fetch fails.
 */

import React from 'react';
import { Alert, Box, Button } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';

/**
 * @typedef {Object} LeaveErrorStateProps
 * @property {any}        error
 * @property {() => void} onRetry
 */

/**
 * Error state for the leave list.
 *
 * @param {LeaveErrorStateProps} props
 * @returns {JSX.Element}
 */
export default function LeaveErrorState({ error, onRetry }) {
  const message = error?.message ?? 'Failed to load leave requests. Please try again.';
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
            aria-label="Retry"
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
