/**
 * @fileoverview WelcomeCard — personalised greeting banner at the top of the dashboard.
 *
 * Greets the logged-in user by first name and displays their role badge
 * alongside a time-appropriate salutation.
 */

import React, { useMemo } from 'react';
import { Box, Card, CardContent, Chip, Typography } from '@mui/material';
import WavingHandIcon from '@mui/icons-material/WavingHand';
import { useAuth } from '@/hooks/useAuth';
import { ROLES } from '@/constants/roles';

/**
 * Returns a time-based greeting string.
 *
 * @returns {'Good morning' | 'Good afternoon' | 'Good evening'}
 */
function getGreeting() {
  const hour = new Date().getHours();
  if (hour < 12) return 'Good morning';
  if (hour < 17) return 'Good afternoon';
  return 'Good evening';
}

/**
 * Maps a ROLE_ string to a human-readable label for the chip.
 *
 * @param {string[]} roles
 * @returns {string}
 */
function getRoleLabel(roles) {
  if (!roles?.length) return 'User';
  if (roles.includes(ROLES.ADMIN))    return 'Administrator';
  if (roles.includes(ROLES.HR))       return 'HR Manager';
  if (roles.includes(ROLES.MANAGER))  return 'Team Manager';
  return 'Employee';
}

/**
 * Personalised welcome card shown at the top of the dashboard.
 *
 * @returns {JSX.Element}
 */
export default function WelcomeCard() {
  const { user } = useAuth();
  const greeting   = useMemo(() => getGreeting(), []);
  const roleLabel  = useMemo(() => getRoleLabel(user?.roles), [user]);

  const today = new Date().toLocaleDateString('en-US', {
    weekday: 'long',
    year:    'numeric',
    month:   'long',
    day:     'numeric',
  });

  return (
    <Card
      sx={{
        mb: 3,
        background: (theme) =>
          theme.palette.mode === 'dark'
            ? 'linear-gradient(135deg, #1a3a5c 0%, #1e2d4f 100%)'
            : 'linear-gradient(135deg, #e3f0ff 0%, #f0f4ff 100%)',
        border: '1px solid',
        borderColor: (theme) =>
          theme.palette.mode === 'dark' ? 'rgba(255,255,255,0.08)' : 'rgba(25,118,210,0.15)',
      }}
    >
      <CardContent sx={{ p: { xs: 2.5, sm: 3 }, '&:last-child': { pb: { xs: 2.5, sm: 3 } } }}>
        <Box
          sx={{
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            flexWrap: 'wrap',
            gap: 2,
          }}
        >
          <Box>
            <Box sx={{ display: 'flex', alignItems: 'center', gap: 1, mb: 0.5 }}>
              <WavingHandIcon sx={{ color: '#f59e0b', fontSize: 24 }} aria-hidden="true" />
              <Typography variant="h5" fontWeight={700}>
                {greeting}, {user?.firstName ?? 'there'}!
              </Typography>
            </Box>
            <Typography variant="body2" color="text.secondary">
              {today}
            </Typography>
          </Box>

          <Chip
            label={roleLabel}
            color="primary"
            variant="outlined"
            size="small"
            sx={{ fontWeight: 600, borderRadius: 2 }}
            aria-label={`Your role: ${roleLabel}`}
          />
        </Box>
      </CardContent>
    </Card>
  );
}
