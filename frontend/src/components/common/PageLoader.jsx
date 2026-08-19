/**
 * @fileoverview PageLoader — inline page-level loading indicator.
 *
 * Used as the Suspense fallback while a lazy route chunk is being downloaded.
 * Unlike {@link LoadingScreen}, this is positioned within the content area
 * rather than as a fixed overlay.
 * Premium SaaS design with gold spinner.
 */

import React from 'react';
import { Box, CircularProgress } from '@mui/material';

/**
 * Centred inline loading spinner for Suspense boundaries.
 *
 * @returns {JSX.Element}
 */
export default function PageLoader() {
  return (
    <Box
      sx={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '50vh',
        width: '100%',
      }}
    >
      <Box sx={{ position: 'relative' }}>
        <CircularProgress
          size={40}
          thickness={3}
          sx={{
            color: 'rgba(26,35,66,0.1)',
            '& .MuiCircularProgress-circle': { strokeLinecap: 'round' },
          }}
          variant="determinate"
          value={100}
        />
        <CircularProgress
          size={40}
          thickness={3}
          sx={{
            color: '#F5C518',
            position: 'absolute',
            left: 0,
            '& .MuiCircularProgress-circle': { strokeLinecap: 'round' },
          }}
        />
      </Box>
    </Box>
  );
}
