/**
 * @fileoverview NotFoundPage — 404 error page.
 */

import React from 'react';
import { Link as RouterLink } from 'react-router-dom';
import { Box, Button, Typography } from '@mui/material';
import HomeIcon from '@mui/icons-material/Home';
import { ROUTES } from '@/constants/routes';

/**
 * Renders a styled 404 Not Found error page.
 *
 * @returns {JSX.Element}
 */
export default function NotFoundPage() {
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
      <Typography
        variant="h1"
        sx={{ fontSize: '6rem', fontWeight: 800, color: 'primary.main', lineHeight: 1 }}
      >
        404
      </Typography>
      <Typography variant="h4" fontWeight={700}>
        Page not found
      </Typography>
      <Typography variant="body1" color="text.secondary" maxWidth={400}>
        The page you are looking for does not exist or has been moved.
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
