/**
 * @fileoverview LoadingScreen — full-viewport centred loading indicator.
 *
 * Shown during initial authentication state resolution so that the user
 * never sees a flash of unauthenticated content.
 */

import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';

/**
 * Full-screen loading overlay with a spinner and optional message.
 *
 * @param {{ message?: string }} [props]
 * @returns {JSX.Element}
 */
export default function LoadingScreen({ message = 'Loading…' }) {
  return (
    <Box
      sx={{
        position: 'fixed',
        inset: 0,
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        gap: 2,
        bgcolor: 'background.default',
        zIndex: (theme) => theme.zIndex.modal + 1,
      }}
    >
      <CircularProgress size={48} thickness={4} />
      <Typography variant="body2" color="text.secondary">
        {message}
      </Typography>
    </Box>
  );
}
