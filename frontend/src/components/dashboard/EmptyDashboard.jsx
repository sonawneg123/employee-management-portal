/**
 * @fileoverview EmptyDashboard — shown when the dashboard has no data at all.
 *
 * Distinct from per-widget empty states — this covers the case where the API
 * returns an entirely empty summary (e.g., fresh installation with no employees).
 */

import React from 'react';
import { Box, Button, Typography } from '@mui/material';
import { Link as RouterLink } from 'react-router-dom';
import DashboardIcon from '@mui/icons-material/Dashboard';
import { ROUTES } from '@/constants/routes';

/**
 * Full-page empty dashboard state for new installations.
 *
 * @returns {JSX.Element}
 */
export default function EmptyDashboard() {
  return (
    <Box
      sx={{
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        minHeight: '60vh',
        textAlign: 'center',
        gap: 2,
        p: 4,
      }}
      role="status"
    >
      <DashboardIcon sx={{ fontSize: 72, color: 'text.disabled', opacity: 0.4 }} />
      <Typography variant="h5" fontWeight={700}>
        Your dashboard is empty
      </Typography>
      <Typography variant="body1" color="text.secondary" maxWidth={420}>
        Start by adding departments and employees to see statistics, charts, and activity feeds
        here.
      </Typography>
      <Button component={RouterLink} to={ROUTES.EMPLOYEES} variant="contained" size="large">
        Add First Employee
      </Button>
    </Box>
  );
}
