/**
 * @fileoverview DepartmentErrorState — shown when the department list fetch fails.
 */

import React from 'react';
import { Alert, Box, Button } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';

/**
 * @typedef {Object} DepartmentErrorStateProps
 * @property {any}        error
 * @property {() => void} onRetry
 */

/**
 * Error state component for the department list.
 *
 * @param {DepartmentErrorStateProps} props
 * @returns {JSX.Element}
 */
export default function DepartmentErrorState({ error, onRetry }) {
  const message = error?.message ?? 'Failed to load departments. Please try again.';

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
            aria-label="Retry loading departments"
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
