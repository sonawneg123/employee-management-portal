/**
 * @fileoverview AccessDeniedPage — 403 Forbidden error page.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import LockIcon  from '@mui/icons-material/Lock';
import HomeIcon  from '@mui/icons-material/Home';
import { ROUTES } from '@/constants/routes';

/**
 * Renders a styled 403 Access Denied error page.
 *
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
        gap: 3,
        p: 4,
        bgcolor: 'background.default',
        textAlign: 'center',
      }}
    >
      <LockIcon sx={{ fontSize: 64, color: 'error.main' }} />
      <Typography
        variant="h1"
        sx={{ fontSize: '6rem', fontWeight: 800, color: 'error.main', lineHeight: 1 }}
      >
        403
      </Typography>
      <Typography variant="h4" fontWeight={700}>
        Access Denied
      </Typography>
      <Typography variant="body1" color="text.secondary" maxWidth={400}>
        You do not have permission to view this page. Please contact your administrator if you
        believe this is a mistake.
      </Typography>
      <Button
        component={RouterLink}
        to={ROUTES.DASHBOARD}
        variant="contained"
        startIcon={<HomeIcon />}
        size="large"
      >
        Back to Dashboard
      </Button>
    </Box>
  );
}
