/**
 * @fileoverview WelcomeCard — personalised greeting banner.
 *
 * Greets the authenticated user by first name with a time-appropriate salutation,
 * displays today's date, and shows a role badge. Uses a subtle indigo gradient.
 */

import React, { useMemo } from 'react';
import { Box, Chip, Typography } from '@mui/material';
import { useAuth } from '@/hooks/useAuth';
import { ROLES } from '@/constants/roles';

/**
 * @returns {'Good morning' | 'Good afternoon' | 'Good evening'}
 */
function getGreeting() {
  const h = new Date().getHours();
  if (h < 12) return 'Good morning';
  if (h < 17) return 'Good afternoon';
  return 'Good evening';
}

/**
 * @param {string[]} roles
 * @returns {string}
 */
function getRoleLabel(roles) {
  if (!roles?.length) return 'User';
  if (roles.includes(ROLES.ADMIN)) return 'Administrator';
  if (roles.includes(ROLES.HR)) return 'HR Manager';
  if (roles.includes(ROLES.MANAGER)) return 'Manager';
  return 'Employee';
}

/**
 * Dashboard welcome banner.
 *
 * @returns {JSX.Element}
 */
export default function WelcomeCard() {
  const { user } = useAuth();
  const greeting = useMemo(getGreeting, []);
  const roleLabel = useMemo(() => getRoleLabel(user?.roles), [user]);

  const today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  });

  return (
    <Box
      sx={{
        mb: 3,
        borderRadius: '16px',
        background: (theme) =>
          theme.palette.mode === 'dark'
            ? 'linear-gradient(135deg, rgba(55,48,163,0.35) 0%, rgba(79,70,229,0.2) 100%)'
            : 'linear-gradient(135deg, rgba(79,70,229,0.06) 0%, rgba(124,58,237,0.04) 100%)',
        border: '1px solid',
        borderColor: (theme) =>
          theme.palette.mode === 'dark' ? 'rgba(79,70,229,0.25)' : 'rgba(79,70,229,0.12)',
        p: { xs: 2.5, sm: 3 },
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'space-between',
        flexWrap: 'wrap',
        gap: 2,
      }}
    >
      <Box>
        <Typography variant="h4" fontWeight={700} sx={{ mb: 0.25, letterSpacing: '-0.01em' }}>
          {greeting}, {user?.firstName ?? 'there'} 👋
        </Typography>
        <Typography variant="body2" color="text.secondary">
          {today}
        </Typography>
      </Box>

      <Chip
        label={roleLabel}
        sx={{
          height: 28,
          fontWeight: 700,
          fontSize: '0.775rem',
          bgcolor: 'rgba(79,70,229,0.1)',
          color: 'primary.main',
          border: '1px solid rgba(79,70,229,0.2)',
        }}
        aria-label={`Your role: ${roleLabel}`}
      />
    </Box>
  );
}
