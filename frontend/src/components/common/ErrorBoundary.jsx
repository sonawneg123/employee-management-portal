/**
 * @fileoverview ErrorBoundary — catches unexpected React render errors.
 *
 * Prevents unhandled JavaScript errors from crashing the entire UI.
 * Renders a user-friendly fallback with a reload button when a render
 * error is caught. Class component is required — React error boundaries
 * cannot yet be implemented with hooks.
 */

import React from 'react';
import { Box, Button, Typography, Alert } from '@mui/material';
import RefreshIcon from '@mui/icons-material/Refresh';

/**
 * @typedef {Object} ErrorBoundaryState
 * @property {boolean}    hasError - Whether an error has been caught.
 * @property {Error|null} error    - The caught error object.
 */

/**
 * React class error boundary.
 *
 * Wrap any subtree that should degrade gracefully on unexpected errors.
 *
 * @extends {React.Component<{children: React.ReactNode, fallback?: React.ReactNode}>}
 */
class ErrorBoundary extends React.Component {
  /** @param {{ children: React.ReactNode, fallback?: React.ReactNode }} props */
  constructor(props) {
    super(props);
    /** @type {ErrorBoundaryState} */
    this.state = { hasError: false, error: null };
  }

  /**
   * Updates state when a descendant throws during rendering.
   *
   * @param {Error} error
   * @returns {ErrorBoundaryState}
   */
  static getDerivedStateFromError(error) {
    return { hasError: true, error };
  }

  /**
   * Logs the error and component stack for debugging.
   *
   * @param {Error} error
   * @param {React.ErrorInfo} info
   */
  componentDidCatch(error, info) {
    console.error('[ErrorBoundary] Caught error:', error, info.componentStack);
  }

  render() {
    if (this.state.hasError) {
      if (this.props.fallback) return this.props.fallback;

      return (
        <Box
          sx={{
            display: 'flex',
            flexDirection: 'column',
            alignItems: 'center',
            justifyContent: 'center',
            minHeight: '50vh',
            p: 4,
            gap: 3,
          }}
        >
          <Alert severity="error" sx={{ maxWidth: 480, width: '100%' }}>
            <Typography variant="subtitle1" fontWeight={600} gutterBottom>
              Something went wrong
            </Typography>
            <Typography variant="body2" color="text.secondary">
              {this.state.error?.message ?? 'An unexpected error occurred in this section.'}
            </Typography>
          </Alert>
          <Button
            variant="contained"
            startIcon={<RefreshIcon />}
            onClick={() => window.location.reload()}
          >
            Reload page
          </Button>
        </Box>
      );
    }

    return this.props.children;
  }
}

export default ErrorBoundary;
