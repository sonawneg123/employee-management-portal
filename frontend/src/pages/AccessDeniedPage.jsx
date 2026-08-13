/**
 * @fileoverview AccessDeniedPage — premium 403 Forbidden error page.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import HomeRoundedIcon from '@mui/icons-material/HomeRounded';
import LockRoundedIcon from '@mui/icons-material/LockRounded';
import { ROUTES } from '@/constants/routes';

/**
 * @returns {JSX.Element}
 */
export default function AccessDeniedPage() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '100vh',
        gap: 2.5,
        p: 4,
        bgcolor: 'background.default',
        textAlign: 'center',
        animation: 'fadeUp 0.4s ease',
      }}
    >
      {/* Icon */}
      <Box
        sx={{
          width: 80,
          height: 80,
          borderRadius: '20px',
          bgcolor: 'rgba(239,68,68,0.1)',
          display: 'flex',
          alignItems: 'center',
          justifyContent: 'center',
        }}
      >
        <LockRoundedIcon sx={{ fontSize: 40, color: 'error.main' }} />
      </Box>

      <Typography
        variant="h1"
        sx={{
          fontSize: { xs: '4rem', md: '6rem' },
          fontWeight: 800,
          color: 'error.main',
          lineHeight: 1,
          letterSpacing: '-0.04em',
        }}
      >
        403
      </Typography>

      <Box sx={{ maxWidth: 440 }}>
        <Typography variant="h3" fontWeight={700} gutterBottom sx={{ letterSpacing: '-0.01em' }}>
          Access denied
        </Typography>
        <Typography variant="body1" color="text.secondary" sx={{ lineHeight: 1.7 }}>
          You don&apos;t have permission to view this page. Contact your administrator if you
          believe this is a mistake.
        </Typography>
      </Box>

      <Button
        component={RouterLink}
        to={ROUTES.DASHBOARD}
        variant="contained"
        startIcon={<HomeRoundedIcon />}
        size="large"
        sx={{ mt: 1, fontWeight: 700 }}
      >
        Back to Dashboard
      </Button>
    </Box>
  );
}
