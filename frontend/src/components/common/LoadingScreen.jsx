/**
 * @fileoverview LoadingScreen — premium full-viewport loading indicator.
 *
 * Shown during initial authentication state resolution so that the user
 * never sees a flash of unauthenticated content.
 * Premium SaaS design with navy + gold animated logo.
 */

import React from 'react';
import { Box, CircularProgress, Typography } from '@mui/material';
import PeopleAltIcon from '@mui/icons-material/PeopleAlt';

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
        gap: 3,
        background: 'linear-gradient(135deg, #0F1628 0%, #1A2342 50%, #2D3A6B 100%)',
        zIndex: (theme) => theme.zIndex.modal + 1,
      }}
    >
      {/* Logo mark */}
      <Box
        sx={{
          width: 64,
          height: 64,
          borderRadius: '18px',
          background: 'linear-gradient(135deg, #F5C518, #C49A00)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
          boxShadow: '0 8px 32px rgba(245,197,24,0.4)',
          animation: 'pulse 2s ease-in-out infinite',
          '@keyframes pulse': {
            '0%, 100%': { transform: 'scale(1)', opacity: 1 },
            '50%': { transform: 'scale(1.05)', opacity: 0.9 },
          },
        }}
      >
        <PeopleAltIcon sx={{ color: '#1A2342', fontSize: 32 }} />
      </Box>

      <Box sx={{ position: 'relative' }}>
        <CircularProgress
          size={40}
          thickness={3}
          sx={{
            color: 'rgba(245,197,24,0.3)',
            '& .MuiCircularProgress-circle': {
              strokeLinecap: 'round',
            },
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
            '& .MuiCircularProgress-circle': {
              strokeLinecap: 'round',
            },
          }}
        />
      </Box>

      <Typography variant="body2" sx={{ color: 'rgba(255,255,255,0.55)', fontWeight: 500 }}>
        {message}
      </Typography>
    </Box>
  );
}
